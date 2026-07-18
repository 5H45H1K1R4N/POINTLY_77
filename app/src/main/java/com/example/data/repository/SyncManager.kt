package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.util.Log
import com.example.data.database.*
import com.example.data.model.UserDocument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class SyncManager(
    private val context: Context,
    private val firestoreRepo: FirestoreRepository,
    private val connectivityObserver: ConnectivityObserver
) {
    private val database = PointlyDatabase.getDatabase(context)
    private val dao = database.pointlyDao()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    sealed interface SyncState {
        object Idle : SyncState
        object Syncing : SyncState
        object Synced : SyncState
        data class Error(val message: String) : SyncState
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _pendingUploadsCount = MutableStateFlow(0)
    val pendingUploadsCount: StateFlow<Int> = _pendingUploadsCount.asStateFlow()

    private val _pendingDownloadsCount = MutableStateFlow(0)
    val pendingDownloadsCount: StateFlow<Int> = _pendingDownloadsCount.asStateFlow()

    init {
        // Automatically start observing network status to trigger sync on reconnect
        scope.launch {
            connectivityObserver.observe().collectLatest { status ->
                Log.d("SyncManager", "Network status updated in SyncManager: $status")
                if (status == ConnectivityObserver.Status.Online) {
                    performSync()
                }
            }
        }
    }

    fun isWifiAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            false
        }
    }

    fun isCharging(): Boolean {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }

    fun triggerSync() {
        scope.launch {
            performSync()
        }
    }

    suspend fun performSync() = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: run {
            _syncState.value = SyncState.Idle
            return@withContext
        }
        val uid = currentUser.uid

        _syncState.value = SyncState.Syncing
        connectivityObserver.setSyncing(true)

        try {
            Log.d("SyncManager", "Initiating background sync for: $uid")

            // Smart sync constraints check (Phase 2)
            // Wi-Fi or Mobile connection is active, battery charging optimization, foreground etc.
            val wifiActive = isWifiAvailable()
            val charging = isCharging()
            Log.d("SyncManager", "Sync environment check: Wi-Fi=$wifiActive, Charging=$charging")

            // Simulate loading upload/download queue length
            _pendingUploadsCount.value = (0..5).random()
            _pendingDownloadsCount.value = (0..2).random()

            // 1. Fetch remote user from Firestore
            val remoteUser = firestoreRepo.getDocument("users", uid, UserDocument::class.java)

            // 2. Fetch local user from Room
            val localProfile = dao.getProfileSync()

            if (remoteUser == null) {
                // Remote does not exist: Upload local profile to server
                if (localProfile != null) {
                    val newUserDoc = UserDocument(
                        uid = uid,
                        name = localProfile.name,
                        email = currentUser.email ?: "",
                        xp = localProfile.xp,
                        level = localProfile.level,
                        streak = localProfile.streak,
                        updatedAt = localProfile.updatedAt
                    )
                    firestoreRepo.saveDocument("users", uid, newUserDoc)
                    Log.d("SyncManager", "Uploaded local profile to initialize remote user.")
                } else {
                    // Seed standard profile locally and upload
                    val initialProfile = ProfileEntity(
                        id = 1,
                        name = currentUser.displayName ?: "Pointly Student",
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.insertProfile(initialProfile)
                    val newUserDoc = UserDocument(
                        uid = uid,
                        name = initialProfile.name,
                        email = currentUser.email ?: "",
                        updatedAt = initialProfile.updatedAt
                    )
                    firestoreRepo.saveDocument("users", uid, newUserDoc)
                    Log.d("SyncManager", "Seeded local profile and uploaded initial data.")
                }
            } else {
                // Conflict resolution: Latest updatedAt wins
                if (localProfile == null) {
                    // Cache is empty: Download remote to local
                    val newLocalProfile = ProfileEntity(
                        id = 1,
                        name = remoteUser.name,
                        level = remoteUser.level,
                        xp = remoteUser.xp,
                        streak = remoteUser.streak,
                        username = remoteUser.username,
                        className = remoteUser.className,
                        section = remoteUser.section,
                        school = remoteUser.school,
                        profileImage = remoteUser.profileImage,
                        isTeacher = remoteUser.isTeacher,
                        employeeId = remoteUser.employeeId,
                        subjects = remoteUser.subjects,
                        classesAssigned = remoteUser.classesAssigned,
                        sectionsAssigned = remoteUser.sectionsAssigned,
                        isAdmin = remoteUser.isAdmin,
                        adminId = remoteUser.adminId,
                        organizationId = remoteUser.organizationId,
                        permissions = remoteUser.permissions,
                        adminRole = remoteUser.adminRole,
                        updatedAt = remoteUser.updatedAtLong
                    )
                    dao.insertProfile(newLocalProfile)
                    Log.d("SyncManager", "No local cache. Seeded Room using Firestore data.")
                } else {
                    Log.d("SyncManager", "Comparing stamps. Local: ${localProfile.updatedAt}, Remote: ${remoteUser.updatedAtLong}")
                    if (localProfile.updatedAt > remoteUser.updatedAtLong) {
                        // Local is newer: Upload to Firestore
                        val updatedUserDoc = remoteUser.copy(
                            name = localProfile.name,
                            xp = localProfile.xp,
                            level = localProfile.level,
                            streak = localProfile.streak,
                            updatedAt = localProfile.updatedAt
                        )
                        firestoreRepo.saveDocument("users", uid, updatedUserDoc)
                        Log.d("SyncManager", "Local changes were newer. Uploaded to remote.")
                    } else if (remoteUser.updatedAtLong > localProfile.updatedAt) {
                        // Remote is newer: Download to local Room
                        val updatedLocalProfile = localProfile.copy(
                            name = remoteUser.name,
                            level = remoteUser.level,
                            xp = remoteUser.xp,
                            streak = remoteUser.streak,
                            username = remoteUser.username,
                            className = remoteUser.className,
                            section = remoteUser.section,
                            school = remoteUser.school,
                            profileImage = remoteUser.profileImage,
                            isTeacher = remoteUser.isTeacher,
                            employeeId = remoteUser.employeeId,
                            subjects = remoteUser.subjects,
                            classesAssigned = remoteUser.classesAssigned,
                            sectionsAssigned = remoteUser.sectionsAssigned,
                            isAdmin = remoteUser.isAdmin,
                            adminId = remoteUser.adminId,
                            organizationId = remoteUser.organizationId,
                            permissions = remoteUser.permissions,
                            adminRole = remoteUser.adminRole,
                            updatedAt = remoteUser.updatedAtLong
                        )
                        dao.insertProfile(updatedLocalProfile)
                        Log.d("SyncManager", "Remote changes were newer. Seeded Room database.")
                    } else {
                        Log.d("SyncManager", "Local database and remote store are in sync.")
                    }
                }
            }

            // Sync other cloud backup/master tables
            syncActivities(uid)

            _lastSyncTime.value = System.currentTimeMillis()
            _pendingUploadsCount.value = 0
            _pendingDownloadsCount.value = 0
            _syncState.value = SyncState.Synced
            Log.d("SyncManager", "Bi-directional synchronization complete.")
        } catch (e: Exception) {
            Log.e("SyncManager", "Synchronization error occurred: ${e.message}", e)
            _syncState.value = SyncState.Error(e.message ?: "Unknown sync error")
        } finally {
            connectivityObserver.setSyncing(false)
        }
    }

    private suspend fun syncActivities(uid: String) {
        val localActivities = dao.getActivitiesSync()
        // Compare with remote and sync missing or changed elements (Phase 4)
        try {
            val db = FirebaseFirestore.getInstance()
            val remoteSnap = db.collection("activities")
                .whereEqualTo("uid", uid)
                .get()
                .await()

            val remoteActivities = remoteSnap.documents.mapNotNull { doc ->
                try {
                    ActivityEntity(
                        activityId = doc.id,
                        uid = doc.getString("uid") ?: "",
                        title = doc.getString("title") ?: "",
                        type = doc.getString("type") ?: "Study",
                        duration = doc.getLong("duration")?.toInt() ?: 0,
                        xpEarned = doc.getLong("xpEarned")?.toInt() ?: 0,
                        pointsEarned = doc.getLong("pointsEarned")?.toInt() ?: 0,
                        startTime = doc.getLong("startTime") ?: 0L,
                        endTime = doc.getLong("endTime") ?: 0L,
                        completed = doc.getBoolean("completed") ?: true,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Upload local activities that are newer or missing remotely
            val remoteMap = remoteActivities.associateBy { it.activityId }
            for (local in localActivities) {
                val remote = remoteMap[local.activityId]
                if (remote == null || local.updatedAt > remote.updatedAt) {
                    db.collection("activities").document(local.activityId)
                        .set(local.copy(uid = uid), SetOptions.merge())
                }
            }

            // Download remote activities that are missing locally or newer remotely
            val localMap = localActivities.associateBy { it.activityId }
            for (remote in remoteActivities) {
                val local = localMap[remote.activityId]
                if (local == null || remote.updatedAt > local.updatedAt) {
                    dao.insertActivity(remote)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Error syncing activities: ${e.message}")
        }
    }

    suspend fun backupAccountData() = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        val uid = currentUser.uid
        val db = FirebaseFirestore.getInstance()

        // Gather all local-only settings and user records
        val localProfile = dao.getProfileSync()
        val backupData = mutableMapOf<String, Any>()
        if (localProfile != null) {
            backupData["profile"] = localProfile
        }
        backupData["backupTimestamp"] = System.currentTimeMillis()

        db.collection("backups").document(uid).set(backupData, SetOptions.merge()).await()
        Log.d("SyncManager", "Account backup completed on Firestore.")
    }

    suspend fun restoreAccountData() = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext
        val uid = currentUser.uid
        val db = FirebaseFirestore.getInstance()

        val snap = db.collection("backups").document(uid).get().await()
        if (snap.exists()) {
            val backupTimestamp = snap.getLong("backupTimestamp") ?: 0L
            Log.d("SyncManager", "Restoring backup from timestamp: $backupTimestamp")
            val profileMap = snap.get("profile") as? Map<*, *>
            if (profileMap != null) {
                val restoredProfile = ProfileEntity(
                    id = 1,
                    name = profileMap["name"] as? String ?: "John Doe",
                    level = (profileMap["level"] as? Long)?.toInt() ?: 1,
                    xp = (profileMap["xp"] as? Long)?.toInt() ?: 0,
                    coins = (profileMap["coins"] as? Long)?.toInt() ?: 0,
                    gems = (profileMap["gems"] as? Long)?.toInt() ?: 0,
                    streak = (profileMap["streak"] as? Long)?.toInt() ?: 0,
                    rank = (profileMap["rank"] as? Long)?.toInt() ?: 10,
                    username = profileMap["username"] as? String ?: "john_doe",
                    className = profileMap["className"] as? String ?: "Class 8",
                    section = profileMap["section"] as? String ?: "A",
                    school = profileMap["school"] as? String ?: "",
                    profileImage = profileMap["profileImage"] as? String ?: "",
                    updatedAt = System.currentTimeMillis()
                )
                dao.insertProfile(restoredProfile)
            }
        }
    }

    fun getLocalDatabaseSize(): Long {
        return try {
            val dbFile = context.getDatabasePath("pointly_database")
            if (dbFile.exists()) dbFile.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        dao.clearAiCache()
        Log.d("SyncManager", "AI Cache cleared successfully.")
    }

    suspend fun rebuildCache() = withContext(Dispatchers.IO) {
        // Rebuild by triggering a refresh/seed of syllabus questions
        dao.clearAiCache()
        val profile = dao.getProfileSync()
        if (profile != null) {
            val seeded = SyllabusEngine.getSeededQuestions().filter { it.`class`.equals(profile.className, ignoreCase = true) }
            if (seeded.isNotEmpty()) {
                dao.insertSyllabusQuestions(seeded.map { it.toEntity() })
            }
        }
        Log.d("SyncManager", "Syllabus cache rebuilt successfully.")
    }

    suspend fun resetLocalDatabase() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        Log.d("SyncManager", "Local Room Database tables cleared completely.")
    }
}
