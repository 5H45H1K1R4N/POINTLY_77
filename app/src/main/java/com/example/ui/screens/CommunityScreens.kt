package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.RoomFeedItemEntity
import com.example.data.database.RoomMessageEntity
import com.example.data.database.RoomShowcasePostEntity
import com.example.data.database.RoomSquadEntity
import com.example.ui.viewmodel.PointlyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CommunitySubTabs(
    selectedSubTab: Int,
    onSubTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        Triple(0, "Leaderboard", Icons.Rounded.Leaderboard),
        Triple(1, "Chat Rooms", Icons.Rounded.Forum),
        Triple(2, "Showcase", Icons.Rounded.Collections),
        Triple(3, "Feed", Icons.Rounded.DynamicFeed),
        Triple(4, "Squads", Icons.Rounded.Group)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (index, label, icon) ->
            val isSelected = selectedSubTab == index
            FilterChip(
                selected = isSelected,
                onClick = { onSubTabSelected(index) },
                label = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE8DEF8),
                    selectedLabelColor = Color(0xFF21005D),
                    selectedLeadingIconColor = Color(0xFF21005D),
                    containerColor = Color.White,
                    labelColor = Color(0xFF49454F),
                    iconColor = Color(0xFF49454F)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// ==========================================
// PHASE 1: CHAT ROOMS VIEW
// ==========================================
@Composable
fun ChatRoomView(
    viewModel: PointlyViewModel,
    modifier: Modifier = Modifier
) {
    val currentChannel by viewModel.currentChannelId.collectAsStateWithLifecycle()
    val messages by viewModel.channelMessages.collectAsStateWithLifecycle()
    val presenceMap by viewModel.userPresences.collectAsStateWithLifecycle()
    val currentUserDoc by viewModel.currentUserDocument.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var replyingToMessage by remember { mutableStateOf<RoomMessageEntity?>(null) }
    var editingMessage by remember { mutableStateOf<RoomMessageEntity?>(null) }

    // Update typing status dynamically
    LaunchedEffect(textState) {
        if (textState.isNotEmpty()) {
            viewModel.updateTypingStatus(true)
            delay(3000)
            viewModel.updateTypingStatus(false)
        } else {
            viewModel.updateTypingStatus(false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFEF7FF)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Channels Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val userClass = currentUserDoc?.className ?: "Class"
            val userSection = currentUserDoc?.section ?: "A"
            val channels = listOf(
                "school" to "🏫 School Arena",
                "class_$userClass" to "🎓 Class $userClass",
                "section_${userClass}_$userSection" to "✍️ Section $userSection"
            )

            channels.forEach { (id, label) ->
                val isSelected = currentChannel == id
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setChannelId(id) },
                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = Color(0xFFD0BCFF),
                        selectedLabelColor = Color(0xFF21005D)
                    )
                )
            }
        }

        // Typing indicators
        val typingUsers = presenceMap.filter { it.value.typingChannelId == currentChannel && it.key != currentUserDoc?.uid }
        if (typingUsers.isNotEmpty()) {
            Text(
                text = "${typingUsers.values.joinToString { it.name }} is typing...",
                fontSize = 11.sp,
                color = Color(0xFF6750A4),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // Messages list
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isOwn = message.senderUid == (currentUserDoc?.uid ?: "")
                    ChatMessageCard(
                        message = message,
                        isOwnMessage = isOwn,
                        presence = presenceMap[message.senderUid],
                        onReply = { replyingToMessage = message },
                        onEdit = { 
                            editingMessage = message
                            textState = message.text
                        },
                        onDelete = { viewModel.deleteChatMessage(message.messageId) },
                        onReact = { emoji -> viewModel.addChatReaction(message.messageId, emoji) }
                    )
                }
            }
        }

        // Reply Indicator
        replyingToMessage?.let { reply ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8DEF8))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Replying to ${reply.senderName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                    Text(reply.text, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { replyingToMessage = null }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
                }
            }
        }

        // Edit Indicator
        editingMessage?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4EFF4))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Editing message...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                IconButton(onClick = { 
                    editingMessage = null
                    textState = ""
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancel edit", modifier = Modifier.size(16.dp))
                }
            }
        }

        // Message input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                placeholder = { Text("Type study query or chat...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textState.isNotBlank()) {
                        val editMsg = editingMessage
                        val replyMsg = replyingToMessage
                        if (editMsg != null) {
                            viewModel.editChatMessage(editMsg.messageId, textState)
                            editingMessage = null
                        } else {
                            viewModel.sendChatMessage(
                                text = textState,
                                replyToId = replyMsg?.messageId,
                                replyToText = replyMsg?.text
                            )
                            replyingToMessage = null
                        }
                        textState = ""
                    }
                })
            )

            IconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        val editMsg = editingMessage
                        val replyMsg = replyingToMessage
                        if (editMsg != null) {
                            viewModel.editChatMessage(editMsg.messageId, textState)
                            editingMessage = null
                        } else {
                            viewModel.sendChatMessage(
                                text = textState,
                                replyToId = replyMsg?.messageId,
                                replyToText = replyMsg?.text
                            )
                            replyingToMessage = null
                        }
                        textState = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF6750A4))
                    .testTag("chat_send_button")
            ) {
                Icon(Icons.Rounded.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageCard(
    message: RoomMessageEntity,
    isOwnMessage: Boolean,
    presence: com.example.data.repository.UserPresence?,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit
) {
    var showReactionMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isOwnMessage) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    StudentAvatar(name = message.senderName, modifier = Modifier.size(32.dp))
                    if (presence?.isOnline == true) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
            }

            Column(horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start) {
                if (!isOwnMessage) {
                    Text(message.senderName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }

                // Parent Reply text
                if (message.replyToId != null) {
                    Text(
                        text = "↳ ${message.replyToText}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .background(Color(0xFFF4EFF4), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    )
                }

                Card(
                    modifier = Modifier.combinedClickable(
                        onLongClick = { showReactionMenu = true },
                        onClick = { showReactionMenu = !showReactionMenu }
                    ),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                        bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOwnMessage) Color(0xFFD0BCFF) else Color(0xFFF3EDF7)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (message.isDeleted) "This message was deleted" else message.text,
                            fontSize = 14.sp,
                            color = if (message.isDeleted) Color.Gray else Color(0xFF1D1B20)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (message.isEdited) {
                                Text("Edited", fontSize = 9.sp, color = Color.Gray)
                            }
                            // Formatted read receipt or reactions
                            if (message.reactions.isNotEmpty()) {
                                Text(message.reactions, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Action menu
        AnimatedVisibility(visible = showReactionMenu) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("👍", "🔥", "❤️", "💡", "🙌").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable {
                                onReact(emoji)
                                showReactionMenu = false
                            }
                            .padding(4.dp)
                    )
                }
                Icon(
                    Icons.Rounded.Reply,
                    contentDescription = "Reply",
                    modifier = Modifier
                        .clickable {
                            onReply()
                            showReactionMenu = false
                        }
                        .padding(4.dp)
                        .size(18.dp),
                    tint = Color.Gray
                )
                if (isOwnMessage && !message.isDeleted) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier
                            .clickable {
                                onEdit()
                                showReactionMenu = false
                            }
                            .padding(4.dp)
                            .size(18.dp),
                        tint = Color.Gray
                    )
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier
                            .clickable {
                                onDelete()
                                showReactionMenu = false
                            }
                            .padding(4.dp)
                            .size(18.dp),
                        tint = Color.Red
                    )
                }
            }
        }
    }
}


// ==========================================
// PHASE 2: STUDENT SHOWCASE VIEW
// ==========================================
@Composable
fun StudentShowcaseView(
    viewModel: PointlyViewModel,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.showcasePosts.collectAsStateWithLifecycle()
    val currentUserDoc by viewModel.currentUserDocument.collectAsStateWithLifecycle()
    var showUploadDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (posts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Collections, contentDescription = "No uploads", modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No showcase projects uploaded yet.", fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("Be the first to present your work!", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts) { post ->
                    ShowcasePostCard(
                        post = post,
                        currentUserUid = currentUserDoc?.uid ?: "",
                        onLike = { viewModel.toggleLikePost(post.postId, post.authorUid) },
                        onSave = { viewModel.toggleSavePost(post.postId) },
                        onReport = { viewModel.reportPost(post.postId) },
                        viewModel = viewModel
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showUploadDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("upload_showcase_fab"),
            containerColor = Color(0xFF6750A4),
            contentColor = Color.White
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Upload")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Present Work")
        }

        if (showUploadDialog) {
            UploadShowcaseDialog(
                onDismiss = { showUploadDialog = false },
                onUpload = { title, desc, cat, uri ->
                    viewModel.createShowcasePost(title, desc, cat, uri, "project_attachment")
                    showUploadDialog = false
                }
            )
        }
    }
}

@Composable
fun ShowcasePostCard(
    post: RoomShowcasePostEntity,
    currentUserUid: String,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onReport: () -> Unit,
    viewModel: PointlyViewModel
) {
    var showCommentSheet by remember { mutableStateOf(false) }
    val commentsFlow: kotlinx.coroutines.flow.Flow<List<com.example.data.repository.PostComment>> = remember(post.postId) { viewModel.getPostCommentsFlow(post.postId) }
    val comments by commentsFlow.collectAsStateWithLifecycle(initialValue = emptyList<com.example.data.repository.PostComment>())
    var commentText by remember { mutableStateOf("") }

    val hasLiked = post.likedBy.split(",").contains(currentUserUid)
    val hasSaved = post.savedBy.split(",").contains(currentUserUid)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudentAvatar(name = post.authorName, modifier = Modifier.size(36.dp))
                    Column {
                        Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("@${post.authorUsername}", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                AssistChip(
                    onClick = {},
                    label = { Text(post.category) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF6750A4))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Description
            Text(post.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1D1B20))
            Spacer(modifier = Modifier.height(4.dp))
            Text(post.description, fontSize = 13.sp, color = Color(0xFF49454F))

            Spacer(modifier = Modifier.height(12.dp))

            // Attachment Banner/Placeholder (Bento aesthetic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3EDF7)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val icon = when (post.category.lowercase()) {
                        "drawing" -> Icons.Rounded.Brush
                        "science project" -> Icons.Rounded.Science
                        "coding project" -> Icons.Rounded.Code
                        "certificate" -> Icons.Rounded.WorkspacePremium
                        "pdf" -> Icons.Rounded.PictureAsPdf
                        else -> Icons.Rounded.Attachment
                    }
                    Icon(icon, contentDescription = "Category icon", modifier = Modifier.size(40.dp), tint = Color(0xFF6750A4))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Attachment: ${post.category}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF49454F))
                    Text(post.fileUrl.take(40) + "...", fontSize = 10.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onLike) {
                        Icon(
                            imageVector = if (hasLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (hasLiked) Color.Red else Color.Gray
                        )
                    }
                    Text("${post.likesCount}", modifier = Modifier.align(Alignment.CenterVertically), fontSize = 13.sp)

                    IconButton(onClick = { showCommentSheet = true }) {
                        Icon(Icons.Rounded.Comment, contentDescription = "Comment", tint = Color.Gray)
                    }
                    Text("${post.commentsCount}", modifier = Modifier.align(Alignment.CenterVertically), fontSize = 13.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = if (hasSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (hasSaved) Color(0xFF6750A4) else Color.Gray
                        )
                    }
                    IconButton(onClick = onReport) {
                        Icon(Icons.Rounded.Report, contentDescription = "Report", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }

            // Expanded comments section inside the Card itself or a sheet
            if (showCommentSheet) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(Color(0xFFFEF7FF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("Comments", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF6750A4))
                    
                    comments.forEach { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudentAvatar(name = comment.authorName, modifier = Modifier.size(24.dp))
                            Column {
                                Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(comment.text, fontSize = 12.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Add comment...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    viewModel.addPostComment(post.postId, post.authorUid, commentText)
                                    commentText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = "Post Comment", tint = Color(0xFF6750A4))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UploadShowcaseDialog(
    onDismiss: () -> Unit,
    onUpload: (String, String, String, Uri?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Drawing") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val categories = listOf("Drawing", "Science Project", "Coding Project", "Notes", "Certificate", "Image", "PDF")
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedFileUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Present Your Work", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                
                Text("Select Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Button(
                    onClick = { launcher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8DEF8), contentColor = Color(0xFF21005D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = "Attach file")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedFileUri != null) "File Attached! ✅" else "Attach Project File")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && description.isNotBlank()) onUpload(title, description, selectedCategory, selectedFileUri) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
            ) {
                Text("Present 🚀")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// ==========================================
// PHASE 3: COMMUNITY FEED VIEW
// ==========================================
@Composable
fun CommunityFeedView(
    viewModel: PointlyViewModel,
    modifier: Modifier = Modifier
) {
    val feedItems by viewModel.communityFeedItems.collectAsStateWithLifecycle()

    if (feedItems.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.DynamicFeed, contentDescription = "Empty feed", modifier = Modifier.size(64.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Nothing on the community pulse yet.", fontWeight = FontWeight.Bold, color = Color.Gray)
            Text("Complete quizzes and study to generate feed items!", fontSize = 12.sp, color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(feedItems) { item ->
                FeedItemCard(item = item)
            }
        }
    }
}

@Composable
fun FeedItemCard(item: RoomFeedItemEntity) {
    val (icon, color) = when (item.type.lowercase()) {
        "achievement" -> Icons.Rounded.EmojiEvents to Color(0xFFFFB300)
        "quiz" -> Icons.Rounded.Quiz to Color(0xFF1E88E5)
        "new_upload" -> Icons.Rounded.Backup to Color(0xFF43A047)
        "squad" -> Icons.Rounded.Groups to Color(0xFF6750A4)
        else -> Icons.Rounded.Star to Color(0xFFE91E63)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = "Feed Item Type", tint = color, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1D1B20))
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.message, fontSize = 12.sp, color = Color(0xFF49454F))
            }
        }
    }
}


// ==========================================
// PHASE 4: STUDY SQUADS VIEW
// ==========================================
@Composable
fun StudySquadsView(
    viewModel: PointlyViewModel,
    modifier: Modifier = Modifier
) {
    val squads by viewModel.studySquads.collectAsStateWithLifecycle()
    val currentUserDoc by viewModel.currentUserDocument.collectAsStateWithLifecycle()
    var showCreateSquadDialog by remember { mutableStateOf(false) }
    var showJoinSquadDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showCreateSquadDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create Squad")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Squad", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showJoinSquadDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8DEF8), contentColor = Color(0xFF21005D))
                ) {
                    Icon(Icons.Rounded.GroupAdd, contentDescription = "Join Squad")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Join Squad", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (squads.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Group, contentDescription = "No squads", modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You aren't in any Study Squads yet.", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Join or create one to share XP and study together!", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(squads) { squad ->
                        val isMember = squad.memberUids.split(",").contains(currentUserDoc?.uid ?: "")
                        if (isMember) {
                            SquadCard(squad = squad, viewModel = viewModel)
                        }
                    }
                }
            }
        }

        if (showCreateSquadDialog) {
            var squadName by remember { mutableStateOf("") }
            var squadDesc by remember { mutableStateOf("") }
            var inviteCodeResult by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { 
                    showCreateSquadDialog = false 
                    inviteCodeResult = null
                },
                title = { Text("Form Study Squad", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (inviteCodeResult == null) {
                            OutlinedTextField(value = squadName, onValueChange = { squadName = it }, label = { Text("Squad Name") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = squadDesc, onValueChange = { squadDesc = it }, label = { Text("Squad Description") }, modifier = Modifier.fillMaxWidth())
                        } else {
                            Text("Squad formed successfully! 🎉", fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                            Text("Share this invite code with your squad members:", fontSize = 12.sp)
                            Text(
                                inviteCodeResult!!,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                color = Color(0xFF21005D),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    if (inviteCodeResult == null) {
                        Button(
                            onClick = {
                                if (squadName.isNotBlank() && squadDesc.isNotBlank()) {
                                    viewModel.createStudySquad(squadName, squadDesc) { code ->
                                        inviteCodeResult = code
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) {
                            Text("Form Squad")
                        }
                    } else {
                        Button(onClick = { showCreateSquadDialog = false }) {
                            Text("Done")
                        }
                    }
                }
            )
        }

        if (showJoinSquadDialog) {
            var inputCode by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showJoinSquadDialog = false },
                title = { Text("Join Study Squad", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Enter the 6-digit squad invite code:")
                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = { inputCode = it },
                            label = { Text("Invite Code") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputCode.isNotBlank()) {
                                viewModel.joinStudySquad(inputCode) { success ->
                                    showJoinSquadDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Text("Join Squad")
                    }
                }
            )
        }
    }
}

@Composable
fun SquadCard(
    squad: RoomSquadEntity,
    viewModel: PointlyViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(squad.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1D1B20))
                    Text(squad.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF21005D), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Level ${squad.level}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Missions & Stats Block (Bento style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // XP Progress
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("SQUAD XP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                        Text("${squad.xp} XP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Weekly Missions
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("WEEKLY MISSIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                        Text("${squad.weeklyMissionsCompleted} Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Squad members count & action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Members: ${squad.memberUids.split(",").size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )

                Button(
                    onClick = { viewModel.setChannelId("squad_${squad.squadId}") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Icon(Icons.Rounded.Forum, contentDescription = "Squad chat", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Squad Chat", fontSize = 11.sp)
                }
            }
        }
    }
}
