package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ProfileEntity
import com.example.data.database.StudyMissionEntity
import com.example.data.database.AchievementEntity
import com.example.data.database.ActivityEntity
import com.example.data.database.PomodoroStateEntity
import com.example.data.database.LeaderboardEntryEntity
import com.example.data.database.BadgeEntity
import com.example.data.database.ChallengeEntity
import com.example.data.database.DailyLoginRewardEntity
import com.example.data.database.ShopItemEntity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import com.example.data.model.UserDocument
import com.example.ui.viewmodel.PointlyViewModel
import com.example.data.model.QuizMode
import com.example.data.model.SyllabusQuestion
import com.example.data.model.SyllabusAnalytics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.viewmodel.AuthUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointlyBentoScreen(
    viewModel: PointlyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isEmailVerified by viewModel.isEmailVerified.collectAsStateWithLifecycle()
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()

    LaunchedEffect(authUiState) {
        when (val state = authUiState) {
            is com.example.ui.viewmodel.AuthUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.clearAuthUiState()
            }
            is com.example.ui.viewmodel.AuthUiState.Error -> {
                Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
                viewModel.clearAuthUiState()
            }
            else -> {}
        }
    }

    if (currentUser == null) {
        PointlyAuthScreen(viewModel = viewModel, modifier = modifier)
        return
    }

    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val missions by viewModel.missionsState.collectAsStateWithLifecycle()
    val achievements by viewModel.achievementsState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    val isStudyActive by viewModel.isStudySessionActive.collectAsStateWithLifecycle()
    val isFocusActive by viewModel.isFocusZoneActive.collectAsStateWithLifecycle()
    val isEditingProfile by viewModel.isEditingProfile.collectAsStateWithLifecycle()

    // Dialog state for Classmate invitations
    var showInviteDialog by remember { mutableStateOf(false) }
    var showSyllabusDashboard by remember { mutableStateOf(false) }

    // Floating celebration effect triggers on achievement message
    val celebrationMessage by viewModel.celebrationMessage.collectAsStateWithLifecycle()
    LaunchedEffect(celebrationMessage) {
        celebrationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearCelebrationMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background, // Bento theme soft canvas
        bottomBar = {
            PointlyBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views based on Tab state
            when (currentTab) {
                0 -> BentoHomeTab(
                    profile = profile,
                    missions = missions,
                    viewModel = viewModel,
                    onInviteClick = { showInviteDialog = true },
                    onSyllabusClick = { showSyllabusDashboard = true }
                )
                1 -> MissionsTab(
                    missions = missions,
                    viewModel = viewModel
                )
                2 -> SocialLeaderboardTab(
                    profile = profile,
                    viewModel = viewModel,
                    onInviteClick = { showInviteDialog = true }
                )
                3 -> ProfileAchievementsTab(
                    profile = profile,
                    achievements = achievements,
                    viewModel = viewModel
                )
                4 -> AiStudyCompanionScreen(
                    viewModel = viewModel
                )
            }

            // Animated Study Session & Gemini Quiz overlay
            AnimatedVisibility(
                visible = isStudyActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                StudySessionOverlay(
                    viewModel = viewModel,
                    onClose = { viewModel.stopStudySession() }
                )
            }

            // Animated Syllabus Quiz Overlay
            val activeSyllabusQuiz by viewModel.activeSyllabusQuiz.collectAsStateWithLifecycle()
            AnimatedVisibility(
                visible = activeSyllabusQuiz != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SyllabusQuizOverlay(
                    viewModel = viewModel,
                    onClose = { viewModel.quitSyllabusQuiz() }
                )
            }

            // Syllabus Dashboard Dialog Overlay
            if (showSyllabusDashboard) {
                SyllabusDashboardOverlay(
                    viewModel = viewModel,
                    onClose = { showSyllabusDashboard = false }
                )
            }

            // Animated Focus Zone & Pomodoro Overlay
            AnimatedVisibility(
                visible = isFocusActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FocusZoneOverlay(
                    viewModel = viewModel,
                    onClose = { viewModel.setFocusZoneActive(false) }
                )
            }

            // Edit Profile Dialog
            if (isEditingProfile && profile != null) {
                var editName by remember { mutableStateOf(profile!!.name) }
                var editTitle by remember { mutableStateOf(profile!!.title) }

                AlertDialog(
                    onDismissRequest = { viewModel.setEditingProfile(false) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.updateProfileName(editName, editTitle)
                                viewModel.setEditingProfile(false)
                            },
                            modifier = Modifier.testTag("save_profile_button")
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.setEditingProfile(false) }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Update Student ID", fontWeight = FontWeight.SemiBold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Student Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Academic Title / Tier") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = Color.White
                )
            }

            // Invite Classmates Dialog
            if (showInviteDialog) {
                AlertDialog(
                    onDismissRequest = { showInviteDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                showInviteDialog = false
                                Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("copy_invite_link_button")
                        ) {
                            Text("Copy Invite Link")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showInviteDialog = false }) {
                            Text("Dismiss")
                        }
                    },
                    title = { Text("Invite Study Squad", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Invite your classmates to earn dynamic team multipliers! Earn +500 XP when they complete their first quiz.", fontSize = 14.sp, color = Color(0xFF49454F))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Study Squad Code:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF6750A4))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                                border = BorderStroke(1.dp, Color(0xFFE8DEF8))
                            ) {
                                Text(
                                    text = "POINTLY-77-SQUAD-JOIN",
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF21005D)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = Color.White
                )
            }
        }
    }
}

@Composable
fun PointlyBottomNavigation(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFF3EDF7),
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(0, "Home", Icons.Rounded.Home),
            Triple(1, "Missions", Icons.Rounded.Assignment),
            Triple(4, "AI Study", Icons.Rounded.AutoAwesome),
            Triple(2, "Social", Icons.Rounded.People),
            Triple(3, "Profile", Icons.Rounded.Person)
        )

        items.forEach { (index, label, icon) ->
            NavigationBarItem(
                selected = currentTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1D192B),
                    selectedTextColor = Color(0xFF6750A4),
                    indicatorColor = Color(0xFFE8DEF8),
                    unselectedIconColor = Color(0xFF49454F),
                    unselectedTextColor = Color(0xFF49454F)
                ),
                modifier = Modifier.testTag("nav_item_$label")
            )
        }
    }
}

// ==========================================
// HOME TAB - BENTO GRID VIEW
// ==========================================
@Composable
fun BentoHomeTab(
    profile: ProfileEntity?,
    missions: List<StudyMissionEntity>,
    viewModel: PointlyViewModel,
    onInviteClick: () -> Unit,
    onSyllabusClick: () -> Unit
) {
    val activeMission = missions.find { !it.completed } ?: missions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- BENTO HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CURRENT STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6750A4),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Pointly 77",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
            }

            // Interactive Profile Badge
            profile?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clickable { viewModel.setEditingProfile(true) }
                        .testTag("profile_badge_clickable")
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LVL ${it.level}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D),
                            modifier = Modifier
                                .background(Color(0xFFEADDFF), RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Text(
                            text = it.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF49454F).copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8DEF8))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (it.name.length >= 2) it.name.substring(0, 2).uppercase() else "JD",
                            color = Color(0xFF6750A4),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // --- BENTO GRID BLOCK 1: ACTIVE MISSION (FULL WIDTH) ---
        activeMission?.let { mission ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_mission_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6750A4)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Circular subtle ambient glow behind the card
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = 220.dp.toPx(),
                                center = Offset(size.width * 0.95f, size.height * 0.1f)
                            )
                        }
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD0BCFF))
                                )
                                Text(
                                    text = "ACTIVE MISSION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = Color(0xFFD0BCFF)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mission.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Earn +${mission.xpReward} XP & Custom Badges",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Button(
                                onClick = { viewModel.startStudySession(mission.subject, mission.title) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF6750A4)
                                ),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("resume_study_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Partner", modifier = Modifier.size(16.dp))
                                    Text("Resume Study", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Due in",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = mission.timeRemaining,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- BENTO GRID ROW 2: STREAK (SPLIT 2 COL) & MINI STATS STACK ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT COLUMN: STREAK CARD (COL SPAN 2)
            profile?.let {
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(136.dp)
                        .clickable {
                            // Boost streak function for engagement fun
                            viewModel.startStudySession("Physics", "Bernoulli's Principle")
                        },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${it.streak}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF21005D),
                            lineHeight = 32.sp
                        )
                        Text(
                            text = "DAY STREAK",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D),
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // RIGHT COLUMN: RANK CARD & PROGRESS CARD (STACKED)
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .height(136.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stack A: Rank card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { viewModel.setTab(2) }, // Goes to leaderboards
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏆", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "RANK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F)
                            )
                            Text(
                                text = "#${profile?.rank ?: 4}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                        }
                    }
                }

                // Stack B: Level progress metric card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { viewModel.setTab(3) }, // Goes to profile/stats
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                    border = BorderStroke(1.dp, Color(0xFFE8DEF8))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8DEF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "XP PROG",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F)
                            )
                            profile?.let {
                                val progressPercent = ((it.xp.toFloat() / 2500f) * 100).toInt()
                                Text(
                                    text = "$progressPercent%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B20)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- BENTO GRID BLOCK 3: SOCIAL ACTIVITY & INVITE (COL SPAN 4) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Overlapping Avatar Stack
                    Box(modifier = Modifier.width(72.dp)) {
                        val colors = listOf(Color(0xFF6750A4), Color(0xFF7D5260), Color(0xFF21005D))
                        val initials = listOf("S", "M", "A")
                        for (i in 0 until 3) {
                            Box(
                                modifier = Modifier
                                    .padding(start = (i * 18).dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors[i])
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    initials[i],
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Sarah & 2 others studying now",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1D1B20),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                IconButton(
                    onClick = onInviteClick,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFF3EDF7), CircleShape)
                        .testTag("invite_squad_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Invite Squad",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- BENTO GRID BLOCK 4: WEEKLY GOAL METRICS (COL SPAN 4) ---
        profile?.let { prof ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.contributeCommunityStudy() }, // Contribution interactive click
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF49454F)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "WEEKLY STUDY GOAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val ratio = (prof.weeklyStudyHours / prof.weeklyGoalHours).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFFD0BCFF),
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Text(
                                text = "%.1fh/%dh".format(prof.weeklyStudyHours, prof.weeklyGoalHours.toInt()),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📊", fontSize = 18.sp)
                    }
                }
            }
        }

        // --- BENTO GRID BLOCK 5: FOCUS ZONE & POMODORO TIMER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.setFocusZoneActive(true) }
                .testTag("focus_zone_bento_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEF8)),
            border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFF6750A4).copy(alpha = 0.04f),
                            radius = 180.dp.toPx(),
                            center = Offset(size.width * 0.1f, size.height * 0.8f)
                        )
                    }
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("⏱️", fontSize = 16.sp)
                            Text(
                                text = "POMODORO FOCUS ZONE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = Color(0xFF6750A4)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Boost Your Productivity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                        Text(
                            text = "Customize study/break intervals, log offline study sessions, view deep local analytics, and receive ambient notifications.",
                            fontSize = 12.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF6750A4), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Open Focus Zone",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // --- BENTO GRID BLOCK 6: SYLLABUS ENGINE CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSyllabusClick() }
                .testTag("syllabus_engine_bento_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
            border = BorderStroke(1.dp, Color(0xFFE8DEF8)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFF6750A4).copy(alpha = 0.05f),
                            radius = 160.dp.toPx(),
                            center = Offset(size.width * 0.9f, size.height * 0.8f)
                        )
                    }
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🎓", fontSize = 16.sp)
                            Text(
                                text = "SYLLABUS QUIZ ENGINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = Color(0xFF6750A4)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Class Syllabus Practice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                        Text(
                            text = "Daily smart quiz, subject-wise chapters, custom mock exams, bookmarks, weak topic revision & real-time CBSE sync.",
                            fontSize = 12.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF6750A4), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = "Open Syllabus Engine",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// ACTIVE STUDY OVERLAY & GEMINI COMPANION PANEL
// ==========================================
@Composable
fun StudySessionOverlay(
    viewModel: PointlyViewModel,
    onClose: () -> Unit
) {
    val subject by viewModel.activeStudySubject.collectAsStateWithLifecycle()
    val topic by viewModel.activeStudyTopic.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.studyTimerSeconds.collectAsStateWithLifecycle()

    val isQuizLoading by viewModel.isQuizLoading.collectAsStateWithLifecycle()
    val currentQuiz by viewModel.currentQuiz.collectAsStateWithLifecycle()
    val activeQuestionIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val selectedAnswer by viewModel.selectedAnswerIndex.collectAsStateWithLifecycle()
    val isAnswerChecked by viewModel.isAnswerChecked.collectAsStateWithLifecycle()
    val isAnswerCorrect by viewModel.isAnswerCorrect.collectAsStateWithLifecycle()
    val sessionXp by viewModel.totalSessionXpEarned.collectAsStateWithLifecycle()

    val formattedTime = remember(timerSeconds) {
        val mins = timerSeconds / 60
        val secs = timerSeconds % 60
        "%02d:%02d".format(mins, secs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overlay Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_study_overlay_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Study Overlay")
                }
                Column {
                    Text("ACTIVE STUDY SESSION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4), letterSpacing = 1.sp)
                    Text("$subject - $topic", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                }
            }

            // Live Timer pill
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = formattedTime,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF21005D),
                    fontSize = 15.sp
                )
            }
        }

        // Live stats in study panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Session XP", fontSize = 11.sp, color = Color(0xFF49454F))
                    Text("+$sessionXp XP", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("AI Partner Status", fontSize = 11.sp, color = Color(0xFF49454F))
                    Text("Synced", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                }
            }
        }

        // STUDY BODY: Toggle between start prompt and Gemini interactive quiz
        if (currentQuiz == null && !isQuizLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE8DEF8))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🧠", fontSize = 48.sp)
                    Text(
                        text = "Gemini AI Study Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D)
                    )
                    Text(
                        text = "Ready to test your comprehension? Let Gemini synthesize a dynamic, gamified quiz for '$topic' based on real academic parameters.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = Color(0xFF49454F),
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { viewModel.fetchGeminiQuizForCurrentTopic() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_quiz_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "Spark", modifier = Modifier.size(18.dp))
                            Text("Generate Gemini Quiz", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (isQuizLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF6750A4))
                    Text(
                        text = "Gemini is building your challenge...",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF49454F)
                    )
                }
            }
        } else {
            // RENDER ACTIVE GEMINI QUIZ
            val quiz = currentQuiz!!
            val currentQuestion = quiz.questions.getOrNull(activeQuestionIndex)

            if (currentQuestion != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quiz Progress Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${activeQuestionIndex + 1} of ${quiz.questions.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF6750A4)
                        )

                        // Linear progress indicator for questions
                        val progress = (activeQuestionIndex + 1).toFloat() / quiz.questions.size.toFloat()
                        LinearProgressIndicator(
                            progress = { progress },
                            color = Color(0xFF6750A4),
                            trackColor = Color(0xFFE8DEF8),
                            modifier = Modifier
                                .width(100.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    // Question Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = currentQuestion.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                        }
                    }

                    // Options list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        currentQuestion.options.forEachIndexed { optIndex, optionText ->
                            val isSelected = selectedAnswer == optIndex
                            val optionBorderColor = when {
                                isAnswerChecked && optIndex == currentQuestion.correctOption -> Color(0xFF4CAF50) // Green
                                isAnswerChecked && isSelected && !isAnswerCorrect -> Color(0xFFF44336) // Red
                                isSelected -> Color(0xFF6750A4)
                                else -> Color(0xFF79747E).copy(alpha = 0.2f)
                            }
                            val optionBgColor = when {
                                isAnswerChecked && optIndex == currentQuestion.correctOption -> Color(0xFFE8F5E9)
                                isAnswerChecked && isSelected && !isAnswerCorrect -> Color(0xFFFFEBEE)
                                isSelected -> Color(0xFFF3EDF7)
                                else -> Color.White
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAnswerChecked) { viewModel.selectAnswer(optIndex) }
                                    .testTag("quiz_option_$optIndex"),
                                colors = CardDefaults.cardColors(containerColor = optionBgColor),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, optionBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val bubbleBg = if (isSelected) Color(0xFF6750A4) else Color(0xFFF3EDF7)
                                    val bubbleText = if (isSelected) Color.White else Color(0xFF49454F)
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(bubbleBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ('A' + optIndex).toString(),
                                            color = bubbleText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = optionText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1D1B20)
                                    )
                                }
                            }
                        }
                    }

                    // Checked state explanation box
                    if (isAnswerChecked) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAnswerCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(if (isAnswerCorrect) "✅" else "❌", fontSize = 18.sp)
                                Column {
                                    Text(
                                        text = if (isAnswerCorrect) "Correct answer!" else "Incorrect",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isAnswerCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Text(
                                        text = currentQuestion.explanation,
                                        fontSize = 13.sp,
                                        color = Color(0xFF49454F),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Navigation Action CTA button
                    Button(
                        onClick = {
                            if (!isAnswerChecked) {
                                viewModel.checkAnswer()
                            } else {
                                viewModel.nextQuestion()
                            }
                        },
                        enabled = selectedAnswer != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("quiz_action_button")
                    ) {
                        val ctaText = when {
                            !isAnswerChecked -> "Check Answer"
                            activeQuestionIndex < quiz.questions.size - 1 -> "Next Question"
                            else -> "Complete & Claim Rewards"
                        }
                        Text(ctaText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// MISSIONS TAB
// ==========================================
@Composable
fun MissionsTab(
    missions: List<StudyMissionEntity>,
    viewModel: PointlyViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Weekly Challenges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
        Text("Complete active missions in the sandbox to boost your GPA streak multipliers and unlock cosmetic profile tiers.", fontSize = 13.sp, color = Color(0xFF49454F))

        missions.forEach { mission ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (mission.completed) Color(0xFFE8F5E9) else Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    if (mission.completed) Color(0xFF81C784).copy(alpha = 0.4f) else Color(0xFF79747E).copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mission.subject.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4),
                            letterSpacing = 1.sp
                        )
                        if (mission.completed) {
                            Text("COMPLETED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        } else {
                            Text(mission.timeRemaining, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF49454F))
                        }
                    }

                    Column {
                        Text(mission.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1D1B20))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(mission.description, fontSize = 13.sp, color = Color(0xFF49454F), lineHeight = 18.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+${mission.xpReward} XP Reward", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF21005D))
                        if (!mission.completed) {
                            Button(
                                onClick = { viewModel.startStudySession(mission.subject, mission.title) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Study", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SOCIAL TAB (LEADERBOARDS & COMMUNITY GOAL)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialLeaderboardTab(
    profile: ProfileEntity?,
    viewModel: PointlyViewModel,
    onInviteClick: () -> Unit
) {
    val leaderboardEntries by viewModel.leaderboardEntries.collectAsStateWithLifecycle()
    val scopeState by viewModel.leaderboardScope.collectAsStateWithLifecycle()
    val filterState by viewModel.leaderboardFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isLeaderboardRefreshing.collectAsStateWithLifecycle()
    val currentUserDoc by viewModel.currentUserDocument.collectAsStateWithLifecycle()
    val currentUserDocVal = currentUserDoc

    var selectedUserForSheet by remember { mutableStateOf<LeaderboardEntryEntity?>(null) }
    var activeSubTab by remember { mutableStateOf(0) }

    val scrollModifier = if (activeSubTab == 0) Modifier.verticalScroll(rememberScrollState()) else Modifier

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(bottom = if (activeSubTab == 0) 100.dp else 0.dp) // Leave space for current user sticky card
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Social Arena Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Social Arena", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                
                IconButton(
                    onClick = { viewModel.refreshLeaderboard() },
                    modifier = Modifier.testTag("refresh_leaderboard_btn")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF6750A4))
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Leaderboard", tint = Color(0xFF6750A4))
                    }
                }
            }

            // Phase 1-5 Community Sub Tabs
            CommunitySubTabs(
                selectedSubTab = activeSubTab,
                onSubTabSelected = { activeSubTab = it }
            )

            if (activeSubTab == 0) {
                // Collaborative Class Challenge
                val communityHours by viewModel.communityStudyHours.collectAsStateWithLifecycle()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF21005D))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("COMMUNITY SQUAD CHALLENGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD0BCFF), letterSpacing = 1.sp)
                                Text("Active Goal: 100h Study Time", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                            Text("🚀", fontSize = 24.sp)
                        }

                        val ratio = (communityHours / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = Color(0xFFD0BCFF),
                            trackColor = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current: %.1fh / 100h".format(communityHours),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Button(
                                onClick = onInviteClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF21005D)),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("Invite Friends", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Weekly Student Rankings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))

                // Search by username/name
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("leaderboard_search_bar"),
                    placeholder = { Text("Search student by name or username...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search Icon") },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear Search")
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFF79747E).copy(alpha = 0.3f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // Scope Selector (Class / Section / School)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PointlyViewModel.LeaderboardScope.values().forEach { scope ->
                        val isSelected = scopeState == scope
                        val label = when (scope) {
                            PointlyViewModel.LeaderboardScope.SCHOOL -> "Whole School"
                            PointlyViewModel.LeaderboardScope.CLASS -> "My Class"
                            PointlyViewModel.LeaderboardScope.SECTION -> "My Section"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setLeaderboardScope(scope) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8DEF8),
                                selectedLabelColor = Color(0xFF21005D)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Duration Filter Selector (Today / Weekly / Monthly / All-Time)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PointlyViewModel.LeaderboardFilter.values().forEach { filter ->
                        val isSelected = filterState == filter
                        val label = when (filter) {
                            PointlyViewModel.LeaderboardFilter.TODAY -> "Today"
                            PointlyViewModel.LeaderboardFilter.WEEKLY -> "Weekly"
                            PointlyViewModel.LeaderboardFilter.MONTHLY -> "Monthly"
                            PointlyViewModel.LeaderboardFilter.ALL_TIME -> "All-Time"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setLeaderboardFilter(filter) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD0BCFF).copy(alpha = 0.35f),
                                selectedLabelColor = Color(0xFF21005D)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Leaderboard entries list
                if (leaderboardEntries.isEmpty()) {
                    // Empty, loading, or error state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⏳", fontSize = 48.sp)
                            Text(
                                text = "No players found",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Be the first to complete a study session and rank up!",
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    // Top 3 Podium
                    LeaderboardPodium(
                        top3 = leaderboardEntries.take(3),
                        onUserClick = { selectedUserForSheet = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Scrollable List of index >= 3 entries
                    val remainingEntries = leaderboardEntries.drop(3)
                    remainingEntries.forEachIndexed { listIndex, user ->
                        val globalRank = listIndex + 4
                        val isCurrentUser = user.uid == (currentUserDocVal?.uid ?: "")
                        val cardBg = if (isCurrentUser) Color(0xFFF3EDF7) else Color.White
                        val borderCol = if (isCurrentUser) Color(0xFFD0BCFF) else Color(0xFF79747E).copy(alpha = 0.1f)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUserForSheet = user },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.5.dp, borderCol)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Rank & Avatar & Name Info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Rank Index & Movement
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(36.dp)
                                    ) {
                                        Text(
                                            text = "$globalRank",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1D1B20)
                                        )
                                        // Rank Movement Indicator
                                        val movementVal = (user.uid.hashCode() % 3)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            when {
                                                movementVal > 0 -> {
                                                    Icon(
                                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                                        contentDescription = "Moved Up",
                                                        tint = Color(0xFF4CAF50),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text("+1", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                                }
                                                movementVal < 0 -> {
                                                    Icon(
                                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                                        contentDescription = "Moved Down",
                                                        tint = Color(0xFFF44336),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text("-1", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                                                }
                                                else -> {
                                                    Text("—", fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }

                                    StudentAvatar(name = user.name, modifier = Modifier.size(40.dp))

                                    Column(modifier = Modifier.padding(start = 4.dp)) {
                                        Text(
                                            text = user.name,
                                            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${user.username}",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Right Points Display
                                val displayPoints = when (filterState) {
                                    PointlyViewModel.LeaderboardFilter.TODAY -> user.todayPoints
                                    PointlyViewModel.LeaderboardFilter.WEEKLY -> user.weeklyPoints
                                    PointlyViewModel.LeaderboardFilter.MONTHLY -> user.monthlyPoints
                                    PointlyViewModel.LeaderboardFilter.ALL_TIME -> user.points
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$displayPoints pts", fontWeight = FontWeight.Bold, color = Color(0xFF6750A4), fontSize = 13.sp)
                                    Text("Lvl ${user.level}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Load More button (Firestore pagination)
                    if (leaderboardEntries.size >= 20) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(
                                onClick = { viewModel.loadMoreLeaderboard() },
                                modifier = Modifier.testTag("load_more_leaderboard_btn")
                            ) {
                                Text("Load More Students", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Render Community Views based on active sub tab selection
                when (activeSubTab) {
                    1 -> ChatRoomView(viewModel = viewModel, modifier = Modifier.weight(1f))
                    2 -> StudentShowcaseView(viewModel = viewModel, modifier = Modifier.weight(1f))
                    3 -> CommunityFeedView(viewModel = viewModel, modifier = Modifier.weight(1f))
                    4 -> StudySquadsView(viewModel = viewModel, modifier = Modifier.weight(1f))
                }
            }
        }

        // Current User Sticky Card (at bottom)
        if (currentUserDocVal != null && activeSubTab == 0) {
            val userRankIndex = leaderboardEntries.indexOfFirst { it.uid == currentUserDocVal.uid }
            val globalRank = if (userRankIndex != -1) userRankIndex + 1 else null
            
            // Sticky card shows up if user is logged in
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.White)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val selfEntry = leaderboardEntries.find { it.uid == currentUserDocVal.uid } ?: LeaderboardEntryEntity(
                                uid = currentUserDocVal.uid,
                                name = currentUserDocVal.name,
                                username = currentUserDocVal.username,
                                className = currentUserDocVal.className,
                                section = currentUserDocVal.section,
                                profileImage = currentUserDocVal.profileImage,
                                points = currentUserDocVal.points,
                                xp = currentUserDocVal.xp,
                                level = currentUserDocVal.level,
                                streak = currentUserDocVal.streak,
                                weeklyPoints = currentUserDocVal.weeklyPoints,
                                monthlyPoints = currentUserDocVal.monthlyPoints,
                                todayPoints = currentUserDocVal.todayPoints,
                                activitiesCompleted = currentUserDocVal.activitiesCompleted,
                                totalStudyTime = currentUserDocVal.totalStudyTime
                            )
                            selectedUserForSheet = selfEntry
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF21005D)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = globalRank?.toString() ?: "—",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            StudentAvatar(name = currentUserDocVal.name, modifier = Modifier.size(40.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentUserDocVal.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFD0BCFF))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("YOU", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                                    }
                                }
                                Text(
                                    text = "@${currentUserDocVal.username}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        val currentUserPoints = when (filterState) {
                            PointlyViewModel.LeaderboardFilter.TODAY -> currentUserDocVal.todayPoints
                            PointlyViewModel.LeaderboardFilter.WEEKLY -> currentUserDocVal.weeklyPoints
                            PointlyViewModel.LeaderboardFilter.MONTHLY -> currentUserDocVal.monthlyPoints
                            PointlyViewModel.LeaderboardFilter.ALL_TIME -> currentUserDocVal.points
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("$currentUserPoints pts", fontWeight = FontWeight.Black, color = Color(0xFFD0BCFF), fontSize = 14.sp)
                            Text("Level ${currentUserDocVal.level}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }

    // Modal Profile Sheet
    if (selectedUserForSheet != null) {
        val user = selectedUserForSheet!!
        ModalBottomSheet(
            onDismissRequest = { selectedUserForSheet = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StudentAvatar(name = user.name, modifier = Modifier.size(80.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                    Text("@${user.username}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    if (user.className.isNotEmpty()) {
                        Text("${user.className} • Section ${user.section}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
                    }
                }
                
                HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.4f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚡ Points & Level", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${user.points} pts", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF21005D))
                            Text("Level ${user.level} (${user.xp} XP)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔥 Daily Streak", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${user.streak} days", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF9F1239))
                            Text("Consistent Scholar", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎓 Focus Sessions", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${user.activitiesCompleted}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF065F46))
                            Text("Sessions Completed", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⏱️ Total Study", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${user.totalStudyTime}m", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E40AF))
                            Text("Total Focus Minutes", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { selectedUserForSheet = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("Close Profile")
                }
            }
        }
    }
}

@Composable
fun StudentAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    val initials = if (name.isNotEmpty()) {
        name.trim().split("\\s+".toRegex()).take(2).map { it.first().uppercase() }.joinToString("")
    } else {
        "?"
    }
    
    val colors = listOf(
        Pair(Color(0xFF6750A4), Color(0xFFE8DEF8)),
        Pair(Color(0xFF21005D), Color(0xFFD0BCFF)),
        Pair(Color(0xFF006874), Color(0xFF80E8FF)),
        Pair(Color(0xFF386A20), Color(0xFFB7F397)),
        Pair(Color(0xFF984061), Color(0xFFFFD9E2)),
        Pair(Color(0xFF7D5260), Color(0xFFFFD8E4))
    )
    val index = java.lang.Math.abs(name.hashCode()) % colors.size
    val (textColor, bgColor) = colors[index]
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun LeaderboardPodium(
    top3: List<LeaderboardEntryEntity>,
    onUserClick: (LeaderboardEntryEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Index 1)
        if (top3.size > 1) {
            PodiumColumn(
                user = top3[1],
                rank = 2,
                height = 95.dp,
                color = Color(0xFF9E9E9E), // Silver
                onUserClick = onUserClick
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        
        // 1st Place (Index 0)
        if (top3.isNotEmpty()) {
            PodiumColumn(
                user = top3[0],
                rank = 1,
                height = 125.dp,
                color = Color(0xFFFFC107), // Gold
                onUserClick = onUserClick
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        
        // 3rd Place (Index 2)
        if (top3.size > 2) {
            PodiumColumn(
                user = top3[2],
                rank = 3,
                height = 75.dp,
                color = Color(0xFFCD7F32), // Bronze
                onUserClick = onUserClick
            )
        }
    }
}

@Composable
fun PodiumColumn(
    user: LeaderboardEntryEntity,
    rank: Int,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    onUserClick: (LeaderboardEntryEntity) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .clickable { onUserClick(user) }
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            StudentAvatar(name = user.name, modifier = Modifier.size(54.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = user.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "@${user.username}",
            fontSize = 9.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${user.points}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF6750A4)
                )
                Text(
                    text = "pts",
                    fontSize = 8.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// ==========================================
// PROFILE & ACHIEVEMENTS TAB
// ==========================================
@Composable
fun ProfileAchievementsTab(
    profile: ProfileEntity?,
    achievements: List<AchievementEntity>,
    viewModel: PointlyViewModel
) {
    val badges by viewModel.badgesState.collectAsStateWithLifecycle()
    val challenges by viewModel.challengesState.collectAsStateWithLifecycle()
    val dailyRewards by viewModel.dailyRewardsState.collectAsStateWithLifecycle()
    val shopItems by viewModel.shopItemsState.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf("profile") } // profile, achievements, badges, challenges, shop
    var achievementCategoryFilter by remember { mutableStateOf("All") }
    var shopCategoryFilter by remember { mutableStateOf("avatar_frame") } // avatar_frame, profile_theme, chat_bubble, profile_background, animated_decoration
    var badgeDetailDialog by remember { mutableStateOf<BadgeEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Tab Selection Bar (Pill style M3 tabs) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color(0xFFF3EDF7), RoundedCornerShape(100.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                "profile" to "👤 Overview",
                "achievements" to "🏆 Achievements",
                "badges" to "🏅 Badges",
                "challenges" to "⚡ Challenges",
                "shop" to "🛒 Shop"
            )
            tabs.forEach { (key, label) ->
                val selected = activeSubTab == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (selected) Color(0xFF6750A4) else Color.Transparent)
                        .clickable { activeSubTab = key }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else Color(0xFF49454F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- SUB-TAB CONTENTS ---
        when (activeSubTab) {
            "profile" -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Core Profile display Card with Equipped Shop Cosmetics
                    profile?.let { prof ->
                        // Dynamic Theme Colors based on Shop Customization
                        val cardBg = when (prof.profileTheme) {
                            "theme_dark" -> Color(0xFF12121A) // space nebula dark
                            "theme_cherry" -> Color(0xFFFFF0F5) // lavender blush cherry pink
                            else -> Color(0xFFF3EDF7) // standard lilac
                        }
                        val textColor = if (prof.profileTheme == "theme_dark") Color.White else Color(0xFF1D1B20)
                        val subTextColor = if (prof.profileTheme == "theme_dark") Color.White.copy(alpha = 0.7f) else Color(0xFF49454F)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(
                                2.dp,
                                when (prof.profileTheme) {
                                    "theme_dark" -> Color(0xFFBB86FC)
                                    "theme_cherry" -> Color(0xFFFFB7C5)
                                    else -> Color(0xFFE8DEF8)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar Block with Custom Frames
                                Box(
                                    modifier = Modifier.size(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Animated Decoration Background
                                    if (prof.animatedDecoration == "decor_sparks") {
                                        Text("✨", fontSize = 28.sp, modifier = Modifier.align(Alignment.TopStart))
                                        Text("✨", fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomEnd))
                                    } else if (prof.animatedDecoration == "decor_confetti") {
                                        Text("🎉", fontSize = 28.sp, modifier = Modifier.align(Alignment.TopEnd))
                                        Text("🎉", fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomStart))
                                    }

                                    // Avatar Frame Border
                                    val frameBorderColor = when (prof.avatarFrame) {
                                        "frame_neon" -> Color(0xFF00FFCC) // Neon teal
                                        "frame_cyber" -> Color(0xFF00E5FF) // Robotic cyan
                                        "badge_frame" -> Color(0xFFFFD700) // Golden frame
                                        else -> Color.Transparent
                                    }
                                    val frameStroke = if (frameBorderColor != Color.Transparent) 4.dp else 0.dp

                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (prof.profileBackground == "bg_futuristic") Color(0xFF311B92)
                                                else if (prof.profileBackground == "bg_anime") Color(0xFFFCE4EC)
                                                else Color(0xFF6750A4)
                                            )
                                            .border(frameStroke, frameBorderColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (prof.name.length >= 2) prof.name.substring(0, 2).uppercase() else "JD",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = prof.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = prof.title,
                                        fontSize = 14.sp,
                                        color = if (prof.profileTheme == "theme_dark") Color(0xFFD0BCFF) else Color(0xFF6750A4),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // XP Progress bar
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Level Progress", fontSize = 11.sp, color = subTextColor)
                                        Text("${prof.xp}/2500 XP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    }
                                    LinearProgressIndicator(
                                        progress = (prof.xp / 2500f).coerceIn(0f, 1f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(100.dp)),
                                        color = Color(0xFF6750A4),
                                        trackColor = Color(0xFFEADDFF)
                                    )
                                }

                                // Quick stats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔥 Streak", fontSize = 11.sp, color = subTextColor)
                                        Text("${prof.streak} Days", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🏅 Rank", fontSize = 11.sp, color = subTextColor)
                                        Text("#${prof.rank}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🪙 Coins", fontSize = 11.sp, color = subTextColor)
                                        Text("${prof.coins}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("💎 Gems", fontSize = 11.sp, color = subTextColor)
                                        Text("${prof.gems}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.setEditingProfile(true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF6750A4)),
                                        shape = RoundedCornerShape(100.dp),
                                        border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                                    ) {
                                        Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.logout() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFECEF), contentColor = Color(0xFFBA1A1A)),
                                        shape = RoundedCornerShape(100.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFDAD6))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ExitToApp,
                                            contentDescription = "Sign Out",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sign Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    
                    // --- Theme Settings Card ---
                    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🎨 Application Theme Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Choose your preferred system theme configuration.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themes = listOf("Light" to "☀️ Light", "Dark" to "🌙 Dark", "System" to "⚙️ System")
                                themes.forEach { (key, label) ->
                                    val isSelected = currentTheme == key
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewModel.setAppTheme(key) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Phase 4: Daily rewards Calendar ---
                    Text(
                        text = "📅 Daily Login Rewards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Login every day consecutively to increase coin, XP multipliers and claim legendary gemstone caches!",
                                fontSize = 12.sp,
                                color = Color(0xFF49454F)
                            )

                            // 7 Days Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                (1..7).forEach { day ->
                                    val reward = dailyRewards.find { it.day == day }
                                    val claimed = reward?.claimed == true
                                    val isCurrent = reward != null && !claimed && (profile?.consecutiveLoginDays ?: 0) + 1 == day

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isCurrent) Color(0xFFEADDFF)
                                                else if (claimed) Color(0xFFE8F5E9)
                                                else Color(0xFFF4F4F6)
                                            )
                                            .border(
                                                width = if (isCurrent) 1.5.dp else 0.dp,
                                                color = if (isCurrent) Color(0xFF6750A4) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable(enabled = isCurrent) {
                                                viewModel.claimDailyReward(day)
                                            }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Day $day", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                                        Text(
                                            text = if (day % 7 == 0) "💎" else "🪙",
                                            fontSize = 16.sp
                                        )
                                        if (claimed) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "Claimed",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else if (isCurrent) {
                                            Text("CLAIM", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF6750A4))
                                        } else {
                                            Text("+${reward?.coinReward ?: 20}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF79747E))
                                        }
                                    }
                                }
                            }

                            // Interactive Playful elements: Free Lucky Spin Wheel & Mystery Loot Chest placeholders
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.earnCoins(100)
                                            viewModel.postSystemNotification(
                                                "Lucky Spin Wheel!",
                                                "spun the lucky spinner wheel and struck the jackpot of +100 Golden Coins!",
                                                "lucky_spin"
                                            )
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                                    border = BorderStroke(1.dp, Color(0xFFFFF59D))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("🎡", fontSize = 24.sp)
                                        Column {
                                            Text("Lucky Spin", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                                            Text("Spin Wheel Free", fontSize = 10.sp, color = Color(0xFF855C00))
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.earnGems(5)
                                            viewModel.postSystemNotification(
                                                "Mystery Loot Chest!",
                                                "unlocked the milestone silver chest and obtained +5 Gemstones!",
                                                "loot_chest"
                                            )
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
                                    border = BorderStroke(1.dp, Color(0xFFCFD8DC))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("📦", fontSize = 24.sp)
                                        Column {
                                            Text("Loot Chest", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                                            Text("Claim Chest Free", fontSize = 10.sp, color = Color(0xFF455A64))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "achievements" -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Filters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf("All", "Study", "Quiz", "Community", "Showcase", "Streak", "Special Events")
                        categories.forEach { cat ->
                            val active = achievementCategoryFilter == cat
                            FilterChip(
                                selected = active,
                                onClick = { achievementCategoryFilter = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Achievements List
                    val filteredAchievements = achievements.filter {
                        achievementCategoryFilter == "All" || it.category.equals(achievementCategoryFilter, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredAchievements.size) { index ->
                            val achievement = filteredAchievements[index]

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (achievement.earned) Color.White else Color(0xFF79747E).copy(alpha = 0.04f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (achievement.earned) {
                                        when (achievement.rarity.lowercase()) {
                                            "legendary" -> Color(0xFFFFD700)
                                            "epic" -> Color(0xFF9C27B0)
                                            "rare" -> Color(0xFF2196F3)
                                            else -> Color(0xFFE8DEF8)
                                        }
                                    } else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icon with Rarity Frame Glow
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (achievement.earned) {
                                                    when (achievement.rarity.lowercase()) {
                                                        "legendary" -> Color(0xFFFFF9C4)
                                                        "epic" -> Color(0xFFF3E5F5)
                                                        "rare" -> Color(0xFFE1F5FE)
                                                        else -> Color(0xFFEADDFF)
                                                    }
                                                } else Color(0xFF79747E).copy(alpha = 0.12f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(achievement.icon, fontSize = 22.sp)
                                    }

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = achievement.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF1D1B20)
                                            )
                                            // Rarity Tag Label
                                            Text(
                                                text = achievement.rarity.uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = when (achievement.rarity.lowercase()) {
                                                    "legendary" -> Color(0xFFF57F17)
                                                    "epic" -> Color(0xFF7B1FA2)
                                                    "rare" -> Color(0xFF0288D1)
                                                    else -> Color(0xFF616161)
                                                },
                                                modifier = Modifier
                                                    .background(
                                                        when (achievement.rarity.lowercase()) {
                                                            "legendary" -> Color(0xFFFFF9C4)
                                                            "epic" -> Color(0xFFF3E5F5)
                                                            "rare" -> Color(0xFFE1F5FE)
                                                            else -> Color(0xFFEEEEEE)
                                                        },
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            achievement.description,
                                            fontSize = 11.sp,
                                            color = Color(0xFF49454F),
                                            lineHeight = 15.sp
                                        )

                                        // Progress Bar
                                        if (!achievement.completed) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                LinearProgressIndicator(
                                                    progress = (achievement.progress.toFloat() / achievement.target).coerceIn(0f, 1f),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(100.dp)),
                                                    color = Color(0xFF6750A4),
                                                    trackColor = Color(0xFFEEEEEE)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "${achievement.progress}/${achievement.target}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF49454F)
                                                )
                                            }
                                        }

                                        // Rewards Display
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text("🪙 +${achievement.coinReward}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                                            Text("✨ +${achievement.xpReward} XP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                                        }
                                    }

                                    // Completion action or status
                                    if (achievement.claimed) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Claimed",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else if (achievement.completed) {
                                        Button(
                                            onClick = { viewModel.claimAchievementReward(achievement.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("CLAIM", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Locked",
                                            tint = Color(0xFF79747E).copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "badges" -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🏆 Earned Badges Showcase",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )

                    val unlockedCount = badges.count { it.unlocked }
                    Text(
                        text = "Unlocked $unlockedCount out of ${badges.size} Badges",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )

                    // Badges Grid List
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(badges.size) { index ->
                            val badge = badges[index]

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { badgeDetailDialog = badge },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (badge.unlocked) Color.White else Color(0xFFF1F1F3)
                                ),
                                border = BorderStroke(
                                    width = if (badge.unlocked) 1.5.dp else 0.dp,
                                    color = if (badge.unlocked) {
                                        when (badge.rarity.lowercase()) {
                                            "legendary" -> Color(0xFFFFD700)
                                            "animated" -> Color(0xFFFF4081)
                                            "epic" -> Color(0xFF9C27B0)
                                            "rare" -> Color(0xFF2196F3)
                                            else -> Color(0xFF81C784)
                                        }
                                    } else Color.Transparent
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Badge Icon
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (badge.unlocked) {
                                                    when (badge.rarity.lowercase()) {
                                                        "legendary" -> Color(0xFFFFF9C4)
                                                        "animated" -> Color(0xFFF8BBD0)
                                                        "epic" -> Color(0xFFF3E5F5)
                                                        "rare" -> Color(0xFFE1F5FE)
                                                        else -> Color(0xFFE8F5E9)
                                                    }
                                                } else Color(0xFFE0E0E0)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = badge.icon,
                                            fontSize = 26.sp,
                                            modifier = Modifier.alpha(if (badge.unlocked) 1f else 0.35f)
                                        )
                                        if (!badge.unlocked) {
                                            Icon(
                                                imageVector = Icons.Rounded.Lock,
                                                contentDescription = "Locked",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(Color(0xFF757575), CircleShape)
                                                    .padding(2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = badge.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (badge.unlocked) Color(0xFF1D1B20) else Color(0xFF757575),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = badge.rarity.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (badge.unlocked) {
                                            when (badge.rarity.lowercase()) {
                                                "legendary" -> Color(0xFFF57F17)
                                                "animated" -> Color(0xFFC2185B)
                                                "epic" -> Color(0xFF7B1FA2)
                                                "rare" -> Color(0xFF0288D1)
                                                else -> Color(0xFF2E7D32)
                                            }
                                        } else Color(0xFF9E9E9E)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "challenges" -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⚡ school & Sandbox Weekly Challenges",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )

                    // Quick simulation buttons for testing Phase 5 & 10
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.progressChallenge("daily_quiz", 1)
                                viewModel.progressChallenge("weekly_study", 10)
                                viewModel.progressAchievement("first_quiz", 1)
                                viewModel.progressAchievement("perfect_quiz", 1)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simulate Quiz Event 📝", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.progressChallenge("daily_help", 1)
                                viewModel.progressChallenge("weekly_pomo", 1)
                                viewModel.progressAchievement("community_hero", 1)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simulate Help Event 🤝", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(challenges.size) { index ->
                            val challenge = challenges[index]

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (challenge.type.lowercase()) {
                                                    "daily" -> Color(0xFFFFF3E0)
                                                    "weekly" -> Color(0xFFE1F5FE)
                                                    "monthly" -> Color(0xFFF3E5F5)
                                                    else -> Color(0xFFE8F5E9)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (challenge.type.lowercase()) {
                                                "daily" -> "🌅"
                                                "weekly" -> "🗓️"
                                                "monthly" -> "🏆"
                                                else -> "☄️"
                                            },
                                            fontSize = 18.sp
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = challenge.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF1D1B20)
                                            )
                                            Text(
                                                text = challenge.type.uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = when (challenge.type.lowercase()) {
                                                    "daily" -> Color(0xFFE65100)
                                                    "weekly" -> Color(0xFF01579B)
                                                    "monthly" -> Color(0xFF4A148C)
                                                    else -> Color(0xFF1B5E20)
                                                },
                                                modifier = Modifier
                                                    .background(
                                                        when (challenge.type.lowercase()) {
                                                            "daily" -> Color(0xFFFFE0B2)
                                                            "weekly" -> Color(0xFFB3E5FC)
                                                            "monthly" -> Color(0xFFE1BEE7)
                                                            else -> Color(0xFFC8E6C9)
                                                        },
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(challenge.description, fontSize = 11.sp, color = Color(0xFF49454F))

                                        // Progress Slider
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            LinearProgressIndicator(
                                                progress = (challenge.progress.toFloat() / challenge.targetValue).coerceIn(0f, 1f),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(100.dp)),
                                                color = Color(0xFF6750A4),
                                                trackColor = Color(0xFFEEEEEE)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "${challenge.progress}/${challenge.targetValue}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF49454F)
                                            )
                                        }

                                        // Rewards
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🪙 +${challenge.coinReward}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                                            Text("✨ +${challenge.xpReward} XP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                                        }
                                    }

                                    // Claim Action
                                    if (challenge.claimed) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Claimed",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else if (challenge.completed) {
                                        Button(
                                            onClick = { viewModel.claimChallengeReward(challenge.challengeId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("CLAIM", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Active",
                                            tint = Color(0xFF79747E).copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "shop" -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🛒 Cosmetics Customization Shop",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )

                    // Shop Category Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val shopCategories = listOf(
                            "avatar_frame" to "Frames",
                            "profile_theme" to "Themes",
                            "chat_bubble" to "Bubbles",
                            "profile_background" to "BGs",
                            "animated_decoration" to "Animations"
                        )
                        shopCategories.forEach { (key, label) ->
                            val active = shopCategoryFilter == key
                            FilterChip(
                                selected = active,
                                onClick = { shopCategoryFilter = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Shop Items List
                    val filteredShopItems = shopItems.filter { it.category == shopCategoryFilter }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredShopItems.size) { index ->
                            val item = filteredShopItems[index]
                            val isEquipped = when (item.category) {
                                "avatar_frame" -> profile?.avatarFrame == item.id
                                "profile_theme" -> profile?.profileTheme == item.id
                                "chat_bubble" -> profile?.chatBubbleColor == item.id
                                "badge_frame" -> profile?.badgeFrame == item.id
                                "profile_background" -> profile?.profileBackground == item.id
                                "animated_decoration" -> profile?.animatedDecoration == item.id
                                else -> false
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(
                                    width = if (isEquipped) 2.dp else 1.dp,
                                    color = if (isEquipped) Color(0xFF6750A4) else Color(0xFF79747E).copy(alpha = 0.15f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Visual Preview
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF3EDF7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(item.icon, fontSize = 28.sp)
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.category.replace("_", " ").uppercase(), fontSize = 8.sp, color = Color(0xFF79747E))
                                    }

                                    // Action Buy or Equip Button
                                    if (item.purchased) {
                                        Button(
                                            onClick = { viewModel.purchaseShopItem(item.id) }, // Equip
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isEquipped) Color(0xFF4CAF50) else Color(0xFF6750A4)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isEquipped) "ACTIVE" else "EQUIP",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        val canAfford = (profile?.coins ?: 0) >= item.cost
                                        Button(
                                            onClick = { viewModel.purchaseShopItem(item.id) },
                                            enabled = canAfford,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFF57F17),
                                                disabledContainerColor = Color(0xFFE0E0E0)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("🪙", fontSize = 10.sp)
                                                Text("${item.cost}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Badge detail custom bottom sheet or alert dialog ---
    badgeDetailDialog?.let { badge ->
        AlertDialog(
            onDismissRequest = { badgeDetailDialog = null },
            confirmButton = {
                TextButton(onClick = { badgeDetailDialog = null }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                }
            },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(badge.icon, fontSize = 32.sp)
                    Text(badge.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(badge.description, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RARITY:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF79747E))
                        Text(
                            text = badge.rarity.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = when (badge.rarity.lowercase()) {
                                "legendary" -> Color(0xFFF57F17)
                                "animated" -> Color(0xFFC2185B)
                                "epic" -> Color(0xFF7B1FA2)
                                "rare" -> Color(0xFF0288D1)
                                else -> Color(0xFF2E7D32)
                            }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STATUS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF79747E))
                        Text(
                            text = if (badge.unlocked) "UNLOCKED 🎉" else "LOCKED 🔒",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (badge.unlocked) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }
        )
    }
}

// =========================================================================
// POINTLY 77 - SEAMLESS GAMIFIED AUTHENTICATION INTERFACES
// =========================================================================

enum class AuthScreenMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointlyAuthScreen(
    viewModel: PointlyViewModel,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(AuthScreenMode.LOGIN) }
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()

    // Remember Me
    var rememberMe by remember { mutableStateOf(true) }

    // Login Fields
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // On setup, load remembered User ID if present
    LaunchedEffect(Unit) {
        val remembered = viewModel.getRememberedUser()
        if (remembered.isNotEmpty()) {
            userId = remembered
            rememberMe = true
        }
    }

    // Register Fields
    var regName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regClass by remember { mutableStateOf("") }
    var regSection by remember { mutableStateOf("") }

    // Username Availability check
    val isUsernameAvailable by viewModel.isUsernameAvailable.collectAsStateWithLifecycle()
    val isCheckingUsername by viewModel.isCheckingUsername.collectAsStateWithLifecycle()

    // Forgot Password Fields
    var forgotEmail by remember { mutableStateOf("") }

    // Local Validation Errors
    var validationError by remember { mutableStateOf<String?>(null) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error,
        errorCursorColor = MaterialTheme.colorScheme.error
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High-Polished Gamified Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "P",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column {
                    Text(
                        "POINTLY 77",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "GAMIFIED LEARNING ECOSYSTEM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Central Bento-Inspired Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // MODE HEADER
                    val titleText = when (mode) {
                        AuthScreenMode.LOGIN -> "Initiate Session"
                        AuthScreenMode.REGISTER -> "Create Student Profile"
                        AuthScreenMode.FORGOT_PASSWORD -> "Recover Magical Key"
                    }
                    val subtitleText = when (mode) {
                        AuthScreenMode.LOGIN -> "Access your learning progression, streak, and missions."
                        AuthScreenMode.REGISTER -> "Begin your journey to claim epic achievements and study wisdom."
                        AuthScreenMode.FORGOT_PASSWORD -> "Enter your email address to receive password recovery spell."
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Local Error feedback or Firestore errors
                    validationError?.let {
                        Text(
                            text = "⚠ $it",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // INPUT FIELDS BASED ON SELECTED MODE
                    when (mode) {
                        AuthScreenMode.LOGIN -> {
                            OutlinedTextField(
                                value = userId,
                                onValueChange = {
                                    userId = it
                                    validationError = null
                                },
                                label = { Text("User ID (Username)") },
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_email_input"),
                                colors = textFieldColors
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    validationError = null
                                },
                                label = { Text("Secret Password") },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                colors = textFieldColors
                            )

                            // Remember Me Checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Remember Me",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        validationError = null
                                        mode = AuthScreenMode.FORGOT_PASSWORD
                                    },
                                    modifier = Modifier.testTag("switch_to_forgot_password_button")
                                ) {
                                    Text("Forgot Password?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    if (userId.trim().length < 3) {
                                        validationError = "Please enter a valid User ID."
                                    } else if (password.length < 6) {
                                        validationError = "Password must be at least 6 characters."
                                    } else {
                                        validationError = null
                                        viewModel.loginWithUserId(userId.trim(), password, rememberMe)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (authUiState is AuthUiState.Loading) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Login to Sanctuary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }

                        AuthScreenMode.REGISTER -> {
                            OutlinedTextField(
                                value = regName,
                                onValueChange = { regName = it; validationError = null },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input"),
                                colors = textFieldColors
                            )

                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = {
                                    regUsername = it
                                    validationError = null
                                    viewModel.checkUsernameAvailability(it)
                                },
                                label = { Text("Unique Username") },
                                leadingIcon = { Icon(Icons.Rounded.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_username_input"),
                                colors = textFieldColors
                            )

                            // Username availability helper text
                            if (regUsername.trim().length >= 3) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isCheckingUsername) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Checking availability...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        when (isUsernameAvailable) {
                                            true -> Text("Username is available! ✅", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            false -> Text("Username is already taken! ❌", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            null -> {}
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it; validationError = null },
                                label = { Text("School/Personal Email") },
                                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_email_input"),
                                colors = textFieldColors
                            )

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it; validationError = null },
                                label = { Text("Secure Password (Min 6 Chars)") },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = {
                                    IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                        Icon(
                                            imageVector = if (regPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_password_input"),
                                colors = textFieldColors
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = regClass,
                                    onValueChange = { regClass = it; validationError = null },
                                    label = { Text("Class") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("register_class_input"),
                                    colors = textFieldColors
                                )

                                OutlinedTextField(
                                    value = regSection,
                                    onValueChange = { regSection = it; validationError = null },
                                    label = { Text("Section") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("register_section_input"),
                                    colors = textFieldColors
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    if (regName.isBlank()) {
                                        validationError = "Name cannot be empty."
                                    } else if (regUsername.length < 3) {
                                        validationError = "Username must be at least 3 characters."
                                    } else if (isUsernameAvailable == false) {
                                        validationError = "The chosen username is already taken."
                                    } else if (!regEmail.contains("@")) {
                                        validationError = "Please enter a valid email address."
                                    } else if (regPassword.length < 6) {
                                        validationError = "Password must be at least 6 characters."
                                    } else if (regClass.isBlank() || regSection.isBlank()) {
                                        validationError = "Class and Section fields are required."
                                    } else {
                                        validationError = null
                                        viewModel.signUpWithEmail(
                                            email = regEmail.trim(),
                                            password = regPassword,
                                            name = regName.trim(),
                                            username = regUsername.trim(),
                                            className = regClass.trim(),
                                            section = regSection.trim()
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("register_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (authUiState is AuthUiState.Loading) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Begin Student Quest", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }

                        AuthScreenMode.FORGOT_PASSWORD -> {
                            OutlinedTextField(
                                value = forgotEmail,
                                onValueChange = { forgotEmail = it; validationError = null },
                                label = { Text("Your Email Address") },
                                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_email_input"),
                                colors = textFieldColors
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (!forgotEmail.contains("@")) {
                                        validationError = "Please enter a valid email address."
                                    } else {
                                        validationError = null
                                        viewModel.resetPassword(forgotEmail.trim())
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("forgot_password_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (authUiState is AuthUiState.Loading) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Summon Password Spell", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }

                            TextButton(
                                onClick = {
                                    validationError = null
                                    mode = AuthScreenMode.LOGIN
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Back to Login", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // MODE SWITCHING FOOTER
            if (mode != AuthScreenMode.FORGOT_PASSWORD) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val promptText = if (mode == AuthScreenMode.LOGIN) "New Student?" else "Already registered?"
                    val actionText = if (mode == AuthScreenMode.LOGIN) "Create profile" else "Login instead"

                    Text(promptText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            validationError = null
                            mode = if (mode == AuthScreenMode.LOGIN) AuthScreenMode.REGISTER else AuthScreenMode.LOGIN
                        },
                        modifier = Modifier.testTag(if (mode == AuthScreenMode.LOGIN) "switch_to_register_button" else "switch_to_login_button")
                    ) {
                        Text(actionText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PointlyVerificationScreen(
    viewModel: PointlyViewModel,
    modifier: Modifier = Modifier
) {
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3EDF7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MarkEmailUnread,
                        contentDescription = "Unverified Email",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    "Email Verification Shield",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF21005D)
                )

                Text(
                    text = "A verification spell was cast to ${user?.email ?: "your email"}. Please activate the verification link in your inbox to unlock the sanctuary dashboard.",
                    fontSize = 14.sp,
                    color = Color(0xFF49454F),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.checkEmailVerificationStatus() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_verification_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (authUiState is AuthUiState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("I Have Verified", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resendVerificationEmail() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("resend_verification_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                    ) {
                        Text("Resend Spell", fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                    }

                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("verification_logout_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBA1A1A)),
                        border = BorderStroke(1.dp, Color(0xFFFFDAD6))
                    ) {
                        Icon(Icons.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sign Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// STUNNING POMODORO FOCUS ZONE & HISTORY OVERLAY
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusZoneOverlay(
    viewModel: PointlyViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    
    // --- Dynamic notification permission request ---
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled for Pomodoro ticks!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications disabled. Ticks will still run in-app.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- State Collections from ViewModel ---
    val activities by viewModel.activitiesState.collectAsStateWithLifecycle()
    val pomodoroState by viewModel.pomodoroState.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()

    // --- Local Form UI States ---
    var manualTitle by remember { mutableStateOf("") }
    var manualType by remember { mutableStateOf("Study") }
    var manualDurationMins by remember { mutableFloatStateOf(25f) }
    var searchFilterText by remember { mutableStateOf("") }
    
    // --- Edit Activity Dialog State ---
    var editingActivity by remember { mutableStateOf<ActivityEntity?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }
    var editDurationMins by remember { mutableFloatStateOf(25f) }

    // --- Quick Preset and Category helpers ---
    val categories = listOf("Study", "Reading", "Workout", "Meditation", "Running", "Custom")
    val categoryEmojis = mapOf(
        "Study" to "✏️",
        "Reading" to "📖",
        "Workout" to "🏋️",
        "Meditation" to "🧘",
        "Running" to "🏃",
        "Custom" to "🛠️"
    )
    val categoryColors = mapOf(
        "Study" to Color(0xFF6750A4),
        "Reading" to Color(0xFF0288D1),
        "Workout" to Color(0xFF2E7D32),
        "Meditation" to Color(0xFFE65100),
        "Running" to Color(0xFFC2185B),
        "Custom" to Color(0xFF455A64)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- OVERLAY HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_focus_overlay_button")
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back to home")
                }
                Column {
                    Text("POMODORO", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Focus Sanctuary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                }
            }

            if (pomodoroState?.isRunning == true) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (pomodoroState?.isBreak == true) Color(0xFFE8F5E9) else Color(0xFFF3EDF7)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (pomodoroState?.isBreak == true) Color(0xFF4CAF50) else Color(0xFF6750A4), CircleShape)
                        )
                        Text(
                            text = if (pomodoroState?.isBreak == true) "On Break" else "Focusing",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pomodoroState?.isBreak == true) Color(0xFF2E7D32) else Color(0xFF21005D)
                        )
                    }
                }
            }
        }

        // --- SECTION 1: CUSTOMIZABLE POMODORO TIMER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pomodoro_timer_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE8DEF8)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Timer circular meter
                pomodoroState?.let { state ->
                    val remaining = state.remainingSeconds
                    val total = state.durationSeconds
                    val progress = if (total > 0) remaining.toFloat() / total.toFloat() else 1f
                    
                    val mins = remaining / 60
                    val secs = remaining % 60
                    val timerText = "%02d:%02d".format(mins, secs)
                    
                    val activeThemeColor = if (state.isBreak) Color(0xFF4CAF50) else Color(0xFF6750A4)
                    
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = activeThemeColor,
                            strokeWidth = 12.dp,
                            trackColor = activeThemeColor.copy(alpha = 0.12f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timerText,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1D1B20)
                            )
                            Text(
                                text = if (state.isBreak) "Take a Break!" else state.activityType.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = activeThemeColor
                            )
                        }
                    }

                    // --- Timer Controls Row ---
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isRunning) {
                            FilledIconButton(
                                onClick = { viewModel.pausePomodoro() },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEADDFF)),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("pomodoro_pause_button")
                            ) {
                                Icon(Icons.Rounded.Pause, contentDescription = "Pause timer", tint = Color(0xFF21005D), modifier = Modifier.size(28.dp))
                            }
                        } else {
                            FilledIconButton(
                                onClick = { viewModel.resumePomodoro() },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF6750A4)),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("pomodoro_play_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Start timer", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }

                        FilledIconButton(
                            onClick = { viewModel.stopPomodoro() },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFFDAD6)),
                            modifier = Modifier
                                    .size(56.dp)
                                    .testTag("pomodoro_stop_button")
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = "Stop timer", tint = Color(0xFFBA1A1A), modifier = Modifier.size(28.dp))
                        }

                        if (state.isBreak && state.isRunning) {
                            FilledIconButton(
                                onClick = { viewModel.skipBreak() },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE8F5E9)),
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("pomodoro_skip_button")
                            ) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Skip break", tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                    // --- Quick Interval Presets (Only allowed when not running) ---
                    if (!state.isRunning) {
                        Divider(color = Color(0xFFCAC4D0).copy(alpha = 0.4f), thickness = 1.dp)
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Set Custom Interval Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(
                                    Triple("Classic", 25, "✏️"),
                                    Triple("Deep Focus", 50, "🧠"),
                                    Triple("Sprint", 15, "⚡")
                                )
                                presets.forEach { (name, mins, emoji) ->
                                    val isSelected = (state.originalDurationSeconds == mins * 60)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.startPomodoro(mins, false, state.activityType) },
                                        label = { Text("$emoji $name (${mins}m)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // --- Dynamic Category chip filter ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Focus Category Activity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { type ->
                                    val isSelected = state.activityType == type
                                    val emoji = categoryEmojis[type] ?: "📝"
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updatePomodoroCategory(type) },
                                        label = { Text("$emoji $type") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = categoryColors[type]?.copy(alpha = 0.16f) ?: Color(0xFFE8DEF8),
                                            selectedLabelColor = categoryColors[type] ?: Color(0xFF6750A4)
                                        )
                                    )
                                }
                            }
                        }
                    }
                } ?: run {
                    // Fallback to recover pomodoro states
                    CircularProgressIndicator(color = Color(0xFF6750A4))
                    LaunchedEffect(Unit) {
                        viewModel.checkAndRecoverPomodoro()
                    }
                }
            }
        }

        // --- SECTION 2: BENTO GRID STATS & ANALYTICS ---
        Text("METRICS & PERFORMANCE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF49454F), fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stats Card 1: Streak
            Card(
                modifier = Modifier.weight(1f).height(100.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🔥 STREAK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Text("${stats.currentStreak} Days", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Text("Keep the fire burning!", fontSize = 9.sp, color = Color(0xFFE65100).copy(alpha = 0.8f))
                }
            }

            // Stats Card 2: Today Minutes
            Card(
                modifier = Modifier.weight(1f).height(100.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)),
                border = BorderStroke(1.dp, Color(0xFF4FC3F7).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⏱️ TODAY FOCUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                    Text("${stats.todayStudyMinutes}m", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                    Text("Monthly: ${stats.monthlyStudyMinutes}m", fontSize = 9.sp, color = Color(0xFF0288D1).copy(alpha = 0.8f))
                }
            }

            // Stats Card 3: Productivity/Focus Score
            Card(
                modifier = Modifier.weight(1f).height(100.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚡ FOCUS SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                    Text("${stats.focusScore}/100", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                    Text("Avg: ${stats.averageSessionMinutes} min/session", fontSize = 9.sp, color = Color(0xFF6750A4).copy(alpha = 0.8f))
                }
            }
        }

        // --- SUB SECTION: ATTRACTIVE PROGRESS BREAKDOWN (VISUAL CHART) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE8DEF8))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Duration Stats by Category", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                    Text("${stats.activitiesCompleted} focus sessions", fontSize = 11.sp, color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
                }

                if (activities.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No records completed yet. Start focus to populate!", fontSize = 12.sp, color = Color(0xFF49454F).copy(alpha = 0.6f))
                    }
                } else {
                    val typeDurations = activities.filter { it.completed }.groupBy { it.type }
                        .mapValues { (_, list) -> list.sumOf { it.duration } / 60 }
                    val maxDuration = typeDurations.values.maxOrNull() ?: 1
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { type ->
                            val duration = typeDurations[type] ?: 0
                            val emoji = categoryEmojis[type] ?: "📝"
                            val color = categoryColors[type] ?: Color(0xFF6750A4)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$emoji $type", fontSize = 11.sp, modifier = Modifier.width(90.dp), color = Color(0xFF49454F))
                                
                                val percentage = if (maxDuration > 0) duration.toFloat() / maxDuration.toFloat() else 0f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .background(Color(0xFFE8DEF8).copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(percentage.coerceIn(0.01f, 1f))
                                            .background(color, RoundedCornerShape(100.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("${duration}m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20), modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 3: LOG DIRECT STANDARD/CUSTOM ACTIVITY FORM ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("direct_activity_log_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE8DEF8))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📝", fontSize = 20.sp)
                    Text("Log Direct / Offline Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                }
                
                Text(
                    text = "Did you complete deep work offline? Log it manually below to secure your dynamic Level XP & Points metrics immediately.",
                    fontSize = 12.sp,
                    color = Color(0xFF49454F)
                )

                OutlinedTextField(
                    value = manualTitle,
                    onValueChange = { manualTitle = it },
                    label = { Text("Activity Title / Topic") },
                    placeholder = { Text("e.g. Reading Chapter 5 or Chest Workout") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_activity_title_field")
                )

                // Category selector chip row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select Activity Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { type ->
                            val isSelected = manualType == type
                            val emoji = categoryEmojis[type] ?: "📝"
                            FilterChip(
                                selected = isSelected,
                                onClick = { manualType = type },
                                label = { Text("$emoji $type") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = categoryColors[type]?.copy(alpha = 0.16f) ?: Color(0xFFE8DEF8),
                                    selectedLabelColor = categoryColors[type] ?: Color(0xFF6750A4)
                                )
                            )
                        }
                    }
                }

                // Slider for Duration
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Duration (Minutes)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                        Text("${manualDurationMins.toInt()} mins", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                    }
                    Slider(
                        value = manualDurationMins,
                        onValueChange = { manualDurationMins = it },
                        valueRange = 5f..120f,
                        steps = 23, // 5 min intervals
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6750A4),
                            activeTrackColor = Color(0xFF6750A4),
                            inactiveTrackColor = Color(0xFFE8DEF8)
                        )
                    )
                }

                Button(
                    onClick = {
                        if (manualTitle.isNotBlank()) {
                            viewModel.logActivityDirectly(manualTitle, manualType, manualDurationMins.toInt())
                            manualTitle = ""
                            Toast.makeText(context, "Activity logged successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter an activity title!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("log_manual_activity_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Secure +${manualDurationMins.toInt() * 2} XP & Points", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- SECTION 4: HISTORICAL LOGS & LIST VIEWS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ACTIVITY RECORD HISTORY", style = MaterialTheme.typography.labelSmall, color = Color(0xFF49454F), fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("${activities.size} Logged", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
        }

        // Search and filter box
        OutlinedTextField(
            value = searchFilterText,
            onValueChange = { searchFilterText = it },
            placeholder = { Text("Search logs by title, subject, or type...") },
            shape = RoundedCornerShape(100.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search history") },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_logs_field")
        )

        val filteredActivities = activities.filter {
            it.title.contains(searchFilterText, ignoreCase = true) ||
            it.type.contains(searchFilterText, ignoreCase = true)
        }

        if (filteredActivities.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE8DEF8))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔍", fontSize = 32.sp)
                    Text(
                        text = if (activities.isEmpty()) "Your focus timeline is empty." else "No search results matched your spell.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF49454F)
                    )
                    Text(
                        text = "Complete your first Pomodoro Focus session or log one offline to begin documenting your legendary rise.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F).copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredActivities.forEach { activity ->
                    val color = categoryColors[activity.type] ?: Color(0xFF6750A4)
                    val emoji = categoryEmojis[activity.type] ?: "📝"
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activity_item_card_${activity.activityId}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE8DEF8).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(color.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 20.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activity.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1B20)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${activity.duration / 60} mins",
                                            fontSize = 11.sp,
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("•", fontSize = 11.sp, color = Color(0xFF49454F).copy(alpha = 0.5f))
                                        
                                        // Format timestamp beautifully
                                        val cal = java.util.Calendar.getInstance()
                                        cal.timeInMillis = activity.endTime
                                        val dateStr = "%d/%d %02d:%02d".format(
                                            cal.get(java.util.Calendar.MONTH) + 1,
                                            cal.get(java.util.Calendar.DAY_OF_MONTH),
                                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                                            cal.get(java.util.Calendar.MINUTE)
                                        )
                                        Text(
                                            text = dateStr,
                                            fontSize = 11.sp,
                                            color = Color(0xFF49454F).copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Reward pills
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "+${activity.xpEarned} XP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = "+${activity.pointsEarned} Pts",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6750A4)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                // Edit trigger
                                IconButton(
                                    onClick = {
                                        editingActivity = activity
                                        editTitle = activity.title
                                        editType = activity.type
                                        editDurationMins = (activity.duration / 60).toFloat()
                                    },
                                    modifier = Modifier.size(32.dp).testTag("edit_activity_button_${activity.activityId}")
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit activity", tint = Color(0xFF6750A4), modifier = Modifier.size(16.dp))
                                }
                                // Delete trigger
                                IconButton(
                                    onClick = {
                                        viewModel.deleteActivity(activity.activityId)
                                        Toast.makeText(context, "Activity entry removed.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp).testTag("delete_activity_button_${activity.activityId}")
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete activity", tint = Color(0xFFBA1A1A), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- EDIT ACTIVITY ALERT DIALOG ---
        editingActivity?.let { activity ->
            AlertDialog(
                onDismissRequest = { editingActivity = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (editTitle.isNotBlank()) {
                                val updatedSec = editDurationMins.toInt() * 60
                                val updated = activity.copy(
                                    title = editTitle,
                                    type = editType,
                                    duration = updatedSec,
                                    xpEarned = editDurationMins.toInt() * 2,
                                    pointsEarned = editDurationMins.toInt() * 2,
                                    updatedAt = System.currentTimeMillis()
                                )
                                viewModel.updateActivityCustom(updated)
                                editingActivity = null
                                Toast.makeText(context, "Activity updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Title cannot be blank", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("save_edited_activity_button")
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingActivity = null }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Edit Activity Entry", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Activity Title") },
                            modifier = Modifier.fillMaxWidth().testTag("edit_activity_title_input")
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Category Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEach { type ->
                                    val isSelected = editType == type
                                    val emoji = categoryEmojis[type] ?: "📝"
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { editType = type },
                                        label = { Text("$emoji $type") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = categoryColors[type]?.copy(alpha = 0.16f) ?: Color(0xFFE8DEF8),
                                            selectedLabelColor = categoryColors[type] ?: Color(0xFF6750A4)
                                        )
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Duration (Minutes)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                                Text("${editDurationMins.toInt()} mins", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                            }
                            Slider(
                                value = editDurationMins,
                                onValueChange = { editDurationMins = it },
                                valueRange = 5f..120f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF6750A4),
                                    activeTrackColor = Color(0xFF6750A4),
                                    inactiveTrackColor = Color(0xFFE8DEF8)
                                )
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = Color.White
            )
        }
    }
}

// ==========================================
// SYLLABUS ENGINE DASHBOARD OVERLAY
// ==========================================
@Composable
fun SyllabusDashboardOverlay(
    viewModel: PointlyViewModel,
    onClose: () -> Unit
) {
    val selectedClass by viewModel.selectedSyllabusClass.collectAsStateWithLifecycle()
    val syllabusQuestions by viewModel.syllabusQuestionsState.collectAsStateWithLifecycle()
    val analytics by viewModel.syllabusAnalyticsState.collectAsStateWithLifecycle()

    var selectedMode by remember { mutableStateOf(QuizMode.DAILY_QUIZ) }
    var selectedSubject by remember { mutableStateOf<String?>("Physics") }
    var selectedChapter by remember { mutableStateOf<String?>("Force and Pressure") }

    var expandedSubjectDropdown by remember { mutableStateOf(false) }
    var expandedChapterDropdown by remember { mutableStateOf(false) }

    val subjectsList = listOf("Physics", "Chemistry", "Biology", "Mathematics", "History")
    val chaptersMap = mapOf(
        "Physics" to listOf("Force and Pressure", "Motion and Measurement", "Electricity", "Laws of Motion", "Friction"),
        "Chemistry" to listOf("Acids, Bases and Salts", "Matter in Our Surroundings", "Chemical Reactions", "Atoms and Molecules"),
        "Biology" to listOf("Crop Production", "Life Processes", "Food and its Sources", "Photosynthesis", "Cell Basics"),
        "Mathematics" to listOf("Integers", "Polynomials", "Algebraic Identities", "Linear Equations"),
        "History" to listOf("French Revolution", "Ancient Civilizations", "The Delhi Sultans")
    )

    val currentChapters = chaptersMap[selectedSubject] ?: emptyList()

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎓", fontSize = 24.sp)
                    Text("Syllabus Engine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Dashboard")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Class Selector Group (Class 6 - 10)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Academic Class", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF6750A4))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Class 6", "Class 7", "Class 8", "Class 9", "Class 10").forEach { cls ->
                            val isSelected = selectedClass.equals(cls, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedSyllabusClass(cls) },
                                label = { Text(cls, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("class_chip_$cls"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEADDFF),
                                    selectedLabelColor = Color(0xFF21005D)
                                )
                            )
                        }
                    }
                }

                // Analytics Bento Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📊", fontSize = 16.sp)
                            Text("Your Syllabus Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Accuracy", fontSize = 10.sp, color = Color(0xFF49454F))
                                    Text(
                                        text = if (analytics.overallAccuracy > 0) "${(analytics.overallAccuracy * 100).toInt()}%" else "0%",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF21005D)
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Questions", fontSize = 10.sp, color = Color(0xFF49454F))
                                    Text(
                                        text = "${analytics.totalQuestionsSolved} solved",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF21005D)
                                    )
                                }
                            }
                        }
                        if (analytics.weakTopics.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Weak Chapters/Topics to Practice:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB3261E))
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    analytics.weakTopics.forEach { topic ->
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFF9DEDC), RoundedCornerShape(100.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(topic, fontSize = 9.sp, color = Color(0xFF370B0B), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quiz Mode Selector Card Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose Practice Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF6750A4))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val modes = listOf(
                            Triple(QuizMode.DAILY_QUIZ, "Daily Quiz", "5 smart syllabus questions mixed"),
                            Triple(QuizMode.SUBJECT_WISE, "Subject Wise Practice", "10 target questions of chosen subject"),
                            Triple(QuizMode.CHAPTER_WISE, "Chapter Wise Challenge", "5 focused questions from specific chapters"),
                            Triple(QuizMode.PRACTICE_MODE, "Syllabus Practice", "10 mixed questions of standard difficulty"),
                            Triple(QuizMode.WEEKLY_TEST, "Weekly Syllabus Test", "15 comprehensive timed syllabus questions"),
                            Triple(QuizMode.MOCK_EXAM, "Timed Mock Exam", "20 questions mimicking authentic Board exams"),
                            Triple(QuizMode.RANDOM_REVISION, "Revision Assistant", "Smartly prioritized weak chapter review"),
                            Triple(QuizMode.BOOKMARKED, "Bookmarked Questions", "Re-solve and master your saved bookmarks"),
                            Triple(QuizMode.WRONG_ANSWERS_PRACTICE, "Weak Topics Practice", "Re-try incorrect and weak questions")
                        )

                        modes.forEach { (mode, title, desc) ->
                            val isSelected = selectedMode == mode
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMode = mode
                                        if (mode != QuizMode.SUBJECT_WISE && mode != QuizMode.CHAPTER_WISE) {
                                            selectedSubject = null
                                            selectedChapter = null
                                        } else {
                                            selectedSubject = "Physics"
                                            selectedChapter = "Force and Pressure"
                                        }
                                    }
                                    .testTag("mode_card_${mode.name}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFEADDFF) else Color.White
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFF79747E).copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (mode) {
                                            QuizMode.DAILY_QUIZ -> "🧠"
                                            QuizMode.SUBJECT_WISE -> "📚"
                                            QuizMode.CHAPTER_WISE -> "📖"
                                            QuizMode.PRACTICE_MODE -> "✏️"
                                            QuizMode.WEEKLY_TEST -> "📅"
                                            QuizMode.MOCK_EXAM -> "🏆"
                                            QuizMode.RANDOM_REVISION -> "🔄"
                                            QuizMode.BOOKMARKED -> "🔖"
                                            QuizMode.WRONG_ANSWERS_PRACTICE -> "⚠️"
                                        },
                                        fontSize = 20.sp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) Color(0xFF21005D) else Color(0xFF1D1B20))
                                        Text(desc, fontSize = 10.sp, color = Color(0xFF49454F))
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = Color(0xFF6750A4), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Contextual Dropdowns for Subject and Chapter Filters
                if (selectedMode == QuizMode.SUBJECT_WISE || selectedMode == QuizMode.CHAPTER_WISE) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select Subject & Chapter", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF6750A4))

                        // Subject Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedSubjectDropdown = true },
                                modifier = Modifier.fillMaxWidth().testTag("subject_dropdown_trigger")
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Subject: ${selectedSubject ?: "Choose Subject"}", fontWeight = FontWeight.Medium)
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = expandedSubjectDropdown,
                                onDismissRequest = { expandedSubjectDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                subjectsList.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub) },
                                        onClick = {
                                            selectedSubject = sub
                                            selectedChapter = chaptersMap[sub]?.firstOrNull()
                                            expandedSubjectDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Chapter Dropdown (Chapter Mode only)
                        if (selectedMode == QuizMode.CHAPTER_WISE) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedChapterDropdown = true },
                                    modifier = Modifier.fillMaxWidth().testTag("chapter_dropdown_trigger")
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Chapter: ${selectedChapter ?: "Choose Chapter"}", fontWeight = FontWeight.Medium)
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedChapterDropdown,
                                    onDismissRequest = { expandedChapterDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    currentChapters.forEach { ch ->
                                        DropdownMenuItem(
                                            text = { Text(ch) },
                                            onClick = {
                                                selectedChapter = ch
                                                expandedChapterDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.startSyllabusQuiz(selectedMode, selectedSubject, selectedChapter)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth().testTag("launch_syllabus_quiz_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Practice Challenge", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

// ==========================================
// ACTIVE SYLLABUS QUIZ SESSION OVERLAY
// ==========================================
@Composable
fun SyllabusQuizOverlay(
    viewModel: PointlyViewModel,
    onClose: () -> Unit
) {
    val quiz by viewModel.activeSyllabusQuiz.collectAsStateWithLifecycle()
    val index by viewModel.activeSyllabusQuizIndex.collectAsStateWithLifecycle()
    val selectedOption by viewModel.activeSyllabusSelectedOption.collectAsStateWithLifecycle()
    val isChecked by viewModel.activeSyllabusIsChecked.collectAsStateWithLifecycle()
    val isCorrect by viewModel.activeSyllabusIsCorrect.collectAsStateWithLifecycle()
    val score by viewModel.activeSyllabusScore.collectAsStateWithLifecycle()
    val xpEarned by viewModel.activeSyllabusXpEarned.collectAsStateWithLifecycle()
    val mode by viewModel.activeSyllabusMode.collectAsStateWithLifecycle()
    val timeSpent by viewModel.activeSyllabusTimeSpent.collectAsStateWithLifecycle()

    val currentQuestion = remember(quiz, index) { quiz?.getOrNull(index) }

    val formattedTime = remember(timeSpent) {
        val mins = timeSpent / 60
        val secs = timeSpent % 60
        "%02d:%02d".format(mins, secs)
    }

    if (quiz == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quiz Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Quit Quiz")
                }
                Column {
                    Text(
                        text = mode?.name?.replace("_", " ") ?: "SYLLABUS QUIZ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.sp
                    )
                    Text("Question ${index + 1} of ${quiz!!.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = formattedTime,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF21005D),
                    fontSize = 14.sp
                )
            }
        }

        // Live stats in quiz taking
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Score Progress", fontSize = 10.sp, color = Color(0xFF49454F))
                    Text("$score Correct", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("XP Earned", fontSize = 10.sp, color = Color(0xFF49454F))
                    Text("+$xpEarned XP", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006C4C))
                }
            }
        }

        currentQuestion?.let { q ->
            // Question Statement Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF79747E).copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEADDFF), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(q.subject, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF3EDF7), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(q.difficulty, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                            }
                        }

                        IconButton(onClick = { viewModel.toggleSyllabusBookmark(q.id, q.isBookmarked) }) {
                            Icon(
                                imageVector = if (q.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Bookmark question",
                                tint = if (q.isBookmarked) Color(0xFF6750A4) else Color(0xFF49454F)
                            )
                        }
                    }

                    Text(
                        text = q.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        lineHeight = 22.sp
                    )
                }
            }

            // Options List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                q.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedOption == optIndex
                    val isOptionCorrect = q.correctAnswerIndex == optIndex

                    val cardColor = when {
                        isChecked && isOptionCorrect -> Color(0xFFD3E3FD)
                        isChecked && isSelected && !isCorrect -> Color(0xFFF9DEDC)
                        isSelected -> Color(0xFFEADDFF)
                        else -> Color.White
                    }

                    val borderColor = when {
                        isChecked && isOptionCorrect -> Color(0xFF006C4C)
                        isChecked && isSelected && !isCorrect -> Color(0xFFB3261E)
                        isSelected -> Color(0xFF6750A4)
                        else -> Color(0xFF79747E).copy(alpha = 0.15f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { viewModel.selectSyllabusOption(optIndex) }
                            .testTag("option_$optIndex"),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(1.5.dp, borderColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = optionText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20),
                                modifier = Modifier.weight(1f)
                            )
                            if (isChecked) {
                                if (isOptionCorrect) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Correct", tint = Color(0xFF006C4C))
                                } else if (isSelected) {
                                    Icon(Icons.Rounded.Cancel, contentDescription = "Incorrect", tint = Color(0xFFB3261E))
                                }
                            } else if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(0xFF6750A4), CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            if (isChecked) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFEADDFF))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("💡", fontSize = 18.sp)
                            Text("Academic Explanation", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF21005D))
                        }
                        Text(
                            text = q.explanation,
                            fontSize = 12.sp,
                            color = Color(0xFF21005D),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isChecked) {
                    OutlinedButton(
                        onClick = { viewModel.skipSyllabusQuestion() },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("skip_syllabus_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Skip", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.checkSyllabusAnswer() },
                        enabled = selectedOption != null,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("check_syllabus_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Check Answer", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.nextSyllabusQuestion() },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("next_syllabus_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (index < quiz!!.size - 1) "Next Question" else "Conclude Practice Challenge",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

