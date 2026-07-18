package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.*
import com.example.data.model.UserDocument
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import android.net.Uri

class CommunityRepository(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository
) {
    private val database = PointlyDatabase.getDatabase(context)
    private val dao = database.pointlyDao()
    private val db = FirebaseFirestore.getInstance()
    private val storage = try { FirebaseStorage.getInstance() } catch (e: Exception) { null }

    // ROOM CHATS FLOWS
    fun getMessagesFlow(channelId: String): Flow<List<RoomMessageEntity>> = dao.getMessagesFlow(channelId)

    // FIRESTORE CHAT SYNC
    fun listenFirestoreMessages(channelId: String): Flow<List<RoomMessageEntity>> = callbackFlow {
        val listener = db.collection("chats")
            .document(channelId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        toRoomMessage(doc, channelId)
                    }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveCachedMessages(messages: List<RoomMessageEntity>) = withContext(Dispatchers.IO) {
        dao.insertMessages(messages)
    }

    private fun toRoomMessage(doc: DocumentSnapshot, channelId: String): RoomMessageEntity? {
        try {
            return RoomMessageEntity(
                messageId = doc.id,
                channelId = channelId,
                senderUid = doc.getString("senderUid") ?: "",
                senderName = doc.getString("senderName") ?: "",
                text = doc.getString("text") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                replyToId = doc.getString("replyToId"),
                replyToText = doc.getString("replyToText"),
                isEdited = doc.getBoolean("edited") ?: false,
                isDeleted = doc.getBoolean("deleted") ?: false,
                reactions = doc.getString("reactions") ?: "",
                readBy = doc.getString("readBy") ?: ""
            )
        } catch (e: Exception) {
            return null
        }
    }

    // CHAT ACTIONS
    suspend fun sendMessage(
        channelId: String,
        senderUid: String,
        senderName: String,
        text: String,
        replyToId: String? = null,
        replyToText: String? = null
    ) {
        val msgId = db.collection("chats").document(channelId).collection("messages").document().id
        val data = mapOf(
            "senderUid" to senderUid,
            "senderName" to senderName,
            "text" to text,
            "timestamp" to System.currentTimeMillis(),
            "replyToId" to replyToId,
            "replyToText" to replyToText,
            "edited" to false,
            "deleted" to false,
            "reactions" to "",
            "readBy" to senderUid
        )
        db.collection("chats").document(channelId).collection("messages").document(msgId).set(data).await()
    }

    suspend fun editMessage(channelId: String, messageId: String, newText: String) {
        db.collection("chats").document(channelId).collection("messages").document(messageId)
            .update(mapOf("text" to newText, "edited" to true)).await()
    }

    suspend fun deleteMessage(channelId: String, messageId: String) {
        db.collection("chats").document(channelId).collection("messages").document(messageId)
            .update(mapOf("text" to "This message was deleted", "deleted" to true)).await()
    }

    suspend fun addReaction(channelId: String, messageId: String, userUid: String, emoji: String) {
        val docRef = db.collection("chats").document(channelId).collection("messages").document(messageId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentReactions = snapshot.getString("reactions") ?: ""
            // Format: "emoji:uid1,uid2;emoji2:uid3"
            val newReactions = appendEmojiReaction(currentReactions, userUid, emoji)
            transaction.update(docRef, "reactions", newReactions)
        }.await()
    }

    suspend fun markMessageAsRead(channelId: String, messageId: String, userUid: String) {
        val docRef = db.collection("chats").document(channelId).collection("messages").document(messageId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentReadBy = snapshot.getString("readBy") ?: ""
            val uids = currentReadBy.split(",").filter { it.isNotEmpty() }.toMutableSet()
            if (!uids.contains(userUid)) {
                uids.add(userUid)
                transaction.update(docRef, "readBy", uids.joinToString(","))
            }
        }.await()
    }

    private fun appendEmojiReaction(current: String, uid: String, emoji: String): String {
        val map = mutableMapOf<String, MutableSet<String>>()
        if (current.isNotEmpty()) {
            current.split(";").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val e = parts[0]
                    val uids = parts[1].split(",").filter { it.isNotEmpty() }.toMutableSet()
                    map[e] = uids
                }
            }
        }
        val targetSet = map.getOrPut(emoji) { mutableSetOf() }
        if (targetSet.contains(uid)) {
            targetSet.remove(uid) // Toggle reaction off
        } else {
            targetSet.add(uid)
        }
        if (targetSet.isEmpty()) {
            map.remove(emoji)
        }
        return map.entries.joinToString(";") { "${it.key}:${it.value.joinToString(",")}" }
    }

    // TYPING & ONLINE PRESENCE
    fun listenPresence(channelId: String): Flow<Map<String, UserPresence>> = callbackFlow {
        val listener = db.collection("presence")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val result = snapshot.documents.associate { doc ->
                        doc.id to UserPresence(
                            isOnline = doc.getBoolean("isOnline") ?: false,
                            typingChannelId = doc.getString("typingChannelId") ?: "",
                            lastActive = doc.getLong("lastActive") ?: 0L,
                            name = doc.getString("name") ?: ""
                        )
                    }
                    trySend(result)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updatePresence(userUid: String, name: String, isOnline: Boolean, typingChannelId: String = "") {
        val data = mapOf(
            "name" to name,
            "isOnline" to isOnline,
            "typingChannelId" to typingChannelId,
            "lastActive" to System.currentTimeMillis()
        )
        db.collection("presence").document(userUid).set(data).await()
    }

    // SHOWCASE POSTS
    fun getShowcasePostsFlow(): Flow<List<RoomShowcasePostEntity>> = dao.getShowcasePostsFlow()

    fun listenFirestoreShowcase(): Flow<List<RoomShowcasePostEntity>> = callbackFlow {
        val listener = db.collection("showcase")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        toRoomShowcasePost(doc)
                    }
                    trySend(posts)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveCachedShowcase(posts: List<RoomShowcasePostEntity>) = withContext(Dispatchers.IO) {
        dao.insertShowcasePosts(posts)
    }

    private fun toRoomShowcasePost(doc: DocumentSnapshot): RoomShowcasePostEntity? {
        try {
            return RoomShowcasePostEntity(
                postId = doc.id,
                authorUid = doc.getString("authorUid") ?: "",
                authorName = doc.getString("authorName") ?: "",
                authorUsername = doc.getString("authorUsername") ?: "",
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                category = doc.getString("category") ?: "",
                fileUrl = doc.getString("fileUrl") ?: "",
                likesCount = doc.getLong("likesCount")?.toInt() ?: 0,
                likedBy = doc.getString("likedBy") ?: "",
                commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0,
                savedBy = doc.getString("savedBy") ?: "",
                reportedBy = doc.getString("reportedBy") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun uploadShowcaseFile(uri: Uri, filename: String): String {
        return if (storage != null) {
            val ref = storage.reference.child("showcase/$filename")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } else {
            "https://firebasestorage.googleapis.com/v0/b/pointly-77/o/showcase%2Fmock.png?alt=media"
        }
    }

    suspend fun createShowcasePost(
        authorUid: String,
        authorName: String,
        authorUsername: String,
        title: String,
        description: String,
        category: String,
        fileUrl: String,
        postId: String? = null
    ) {
        val finalPostId = postId ?: db.collection("showcase").document().id
        val data = mapOf(
            "authorUid" to authorUid,
            "authorName" to authorName,
            "authorUsername" to authorUsername,
            "title" to title,
            "description" to description,
            "category" to category,
            "fileUrl" to fileUrl,
            "likesCount" to 0,
            "likedBy" to "",
            "commentsCount" to 0,
            "savedBy" to "",
            "reportedBy" to "",
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("showcase").document(finalPostId).set(data).await()

        createFeedItem(
            uid = authorUid,
            name = authorName,
            type = "NEW_UPLOAD",
            title = "New Showcase Upload",
            message = "$authorName uploaded a new $category project: \"$title\""
        )
    }

    suspend fun toggleLikePost(postId: String, userUid: String) {
        val docRef = db.collection("showcase").document(postId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val likedByStr = snapshot.getString("likedBy") ?: ""
            val likedUids = likedByStr.split(",").filter { it.isNotEmpty() }.toMutableSet()
            val newLikedBy: String
            val countDiff: Int
            if (likedUids.contains(userUid)) {
                likedUids.remove(userUid)
                newLikedBy = likedUids.joinToString(",")
                countDiff = -1
            } else {
                likedUids.add(userUid)
                newLikedBy = likedUids.joinToString(",")
                countDiff = 1
            }
            val currentLikes = (snapshot.getLong("likesCount") ?: 0L).toInt()
            transaction.update(docRef, mapOf(
                "likedBy" to newLikedBy,
                "likesCount" to (currentLikes + countDiff).coerceAtLeast(0)
            ))
        }.await()
    }

    suspend fun toggleSavePost(postId: String, userUid: String) {
        val docRef = db.collection("showcase").document(postId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val savedByStr = snapshot.getString("savedBy") ?: ""
            val savedUids = savedByStr.split(",").filter { it.isNotEmpty() }.toMutableSet()
            if (savedUids.contains(userUid)) {
                savedUids.remove(userUid)
            } else {
                savedUids.add(userUid)
            }
            transaction.update(docRef, "savedBy", savedUids.joinToString(","))
        }.await()
    }

    suspend fun reportPost(postId: String, userUid: String) {
        val docRef = db.collection("showcase").document(postId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val reportedByStr = snapshot.getString("reportedBy") ?: ""
            val reportedUids = reportedByStr.split(",").filter { it.isNotEmpty() }.toMutableSet()
            reportedUids.add(userUid)
            transaction.update(docRef, "reportedBy", reportedUids.joinToString(","))
        }.await()
    }

    // COMMENTS ON SHOWCASE POSTS
    fun listenPostComments(postId: String): Flow<List<PostComment>> = callbackFlow {
        val listener = db.collection("showcase")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        PostComment(
                            commentId = doc.id,
                            authorUid = doc.getString("authorUid") ?: "",
                            authorName = doc.getString("authorName") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    }
                    trySend(comments)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(postId: String, authorUid: String, authorName: String, text: String) {
        val docRef = db.collection("showcase").document(postId)
        val commentId = docRef.collection("comments").document().id
        val data = mapOf(
            "authorUid" to authorUid,
            "authorName" to authorName,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("showcase").document(postId).collection("comments").document(commentId).set(data).await()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentCommentsCount = (snapshot.getLong("commentsCount") ?: 0L).toInt()
            transaction.update(docRef, "commentsCount", currentCommentsCount + 1)
        }.await()
    }

    // COMMUNITY FEED
    fun getFeedFlow(): Flow<List<RoomFeedItemEntity>> = dao.getFeedFlow()

    fun listenFirestoreFeed(): Flow<List<RoomFeedItemEntity>> = callbackFlow {
        val listener = db.collection("community_feed")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val feedItems = snapshot.documents.mapNotNull { doc ->
                        try {
                            RoomFeedItemEntity(
                                id = doc.id,
                                uid = doc.getString("uid") ?: "",
                                name = doc.getString("name") ?: "",
                                type = doc.getString("type") ?: "",
                                title = doc.getString("title") ?: "",
                                message = doc.getString("message") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(feedItems)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveCachedFeed(items: List<RoomFeedItemEntity>) = withContext(Dispatchers.IO) {
        dao.insertFeedItems(items)
    }

    suspend fun createFeedItem(uid: String, name: String, type: String, title: String, message: String) {
        val feedId = db.collection("community_feed").document().id
        val data = mapOf(
            "uid" to uid,
            "name" to name,
            "type" to type,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("community_feed").document(feedId).set(data).await()
    }

    // STUDY SQUADS
    fun getSquadsFlow(): Flow<List<RoomSquadEntity>> = dao.getSquadsFlow()

    fun listenFirestoreSquads(): Flow<List<RoomSquadEntity>> = callbackFlow {
        val listener = db.collection("squads")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val squads = snapshot.documents.mapNotNull { doc ->
                        toRoomSquad(doc)
                    }
                    trySend(squads)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveCachedSquads(squads: List<RoomSquadEntity>) = withContext(Dispatchers.IO) {
        dao.insertSquads(squads)
    }

    private fun toRoomSquad(doc: DocumentSnapshot): RoomSquadEntity? {
        try {
            val memberList = doc.get("memberUids") as? List<*> ?: emptyList<Any>()
            return RoomSquadEntity(
                squadId = doc.id,
                name = doc.getString("name") ?: "",
                description = doc.getString("description") ?: "",
                inviteCode = doc.getString("inviteCode") ?: "",
                creatorUid = doc.getString("creatorUid") ?: "",
                memberUids = memberList.filterIsInstance<String>().joinToString(","),
                points = doc.getLong("points")?.toInt() ?: 0,
                xp = doc.getLong("xp")?.toInt() ?: 0,
                level = doc.getLong("level")?.toInt() ?: 1,
                weeklyMissionsCompleted = doc.getLong("weeklyMissionsCompleted")?.toInt() ?: 0,
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun createSquad(name: String, description: String, creatorUid: String): String {
        val squadId = db.collection("squads").document().id
        val inviteCode = (100000..999999).random().toString()
        val data = mapOf(
            "name" to name,
            "description" to description,
            "inviteCode" to inviteCode,
            "creatorUid" to creatorUid,
            "memberUids" to listOf(creatorUid),
            "points" to 0,
            "xp" to 0,
            "level" to 1,
            "weeklyMissionsCompleted" to 0,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        db.collection("squads").document(squadId).set(data).await()
        return inviteCode
    }

    suspend fun joinSquad(inviteCode: String, userUid: String): Boolean {
        val querySnapshot = db.collection("squads").whereEqualTo("inviteCode", inviteCode).get().await()
        if (querySnapshot.isEmpty) return false
        val doc = querySnapshot.documents.first()
        val squadId = doc.id
        val memberList = doc.get("memberUids") as? List<*> ?: emptyList<Any>()
        val members = memberList.filterIsInstance<String>().toMutableSet()
        if (!members.contains(userUid)) {
            members.add(userUid)
            db.collection("squads").document(squadId).update("memberUids", members.toList()).await()
        }
        return true
    }

    // NOTIFICATIONS
    fun getNotificationsFlow(): Flow<List<RoomNotificationEntity>> = dao.getNotificationsFlow()

    suspend fun createNotification(recipientUid: String, title: String, message: String, type: String) {
        val notifId = db.collection("notifications").document().id
        val data = mapOf(
            "recipientUid" to recipientUid,
            "title" to title,
            "message" to message,
            "type" to type,
            "read" to false,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("notifications").document(notifId).set(data).await()

        val local = RoomNotificationEntity(
            id = notifId,
            recipientUid = recipientUid,
            title = title,
            message = message,
            type = type,
            isRead = false,
            timestamp = System.currentTimeMillis()
        )
        dao.insertNotification(local)
    }
}

data class UserPresence(
    val isOnline: Boolean = false,
    val typingChannelId: String = "",
    val lastActive: Long = 0L,
    val name: String = ""
)

data class PostComment(
    val commentId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
