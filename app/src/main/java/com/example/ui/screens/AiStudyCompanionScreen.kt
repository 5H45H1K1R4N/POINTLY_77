package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.QuizMode
import com.example.ui.viewmodel.PointlyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudyCompanionScreen(
    viewModel: PointlyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiRecommendations by viewModel.aiRecommendations.collectAsStateWithLifecycle()
    val aiStudyPlan by viewModel.aiStudyPlan.collectAsStateWithLifecycle()
    val aiRevisionMaterial by viewModel.aiRevisionMaterial.collectAsStateWithLifecycle()
    val aiChatHistory by viewModel.aiChatHistory.collectAsStateWithLifecycle()
    val aiPlannerMode by viewModel.aiPlannerMode.collectAsStateWithLifecycle()
    val aiRevisionType by viewModel.aiRevisionType.collectAsStateWithLifecycle()
    val aiSelectedSubject by viewModel.aiSelectedSubject.collectAsStateWithLifecycle()

    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val userDoc by viewModel.currentUserDocument.collectAsStateWithLifecycle()
    val analytics by viewModel.syllabusAnalyticsState.collectAsStateWithLifecycle()
    val challenges by viewModel.challengesState.collectAsStateWithLifecycle()

    // Screen-level navigation tabs
    var aiActiveSection by remember { mutableStateOf("Dashboard") } // Dashboard, Ask AI, Revision, Plans, Practice
    val sections = listOf(
        Triple("Dashboard", "Dashboard", Icons.Rounded.Dashboard),
        Triple("Ask AI", "Ask AI", Icons.Rounded.Forum),
        Triple("Revision", "Revision Lab", Icons.Rounded.AutoStories),
        Triple("Plans", "Study Plans", Icons.Rounded.CalendarMonth),
        Triple("Practice", "Practice Hub", Icons.Rounded.Quiz)
    )

    // Trigger initial recommendations fetch if empty
    LaunchedEffect(Unit) {
        if (aiRecommendations.isEmpty()) {
            viewModel.fetchAiRecommendations()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFBF8FD))
            .statusBarsPadding()
    ) {
        // --- PHASE 2: Context-Aware Banner ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF3EDF7),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Context",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Study Companion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Student: ${profile?.name ?: userDoc?.name ?: "Student"} • ${userDoc?.className ?: "Class 8"} (CBSE)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.testTag("ai_streak_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile?.streak ?: 77} Days",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // --- SECTION NAVIGATION TABS ---
        ScrollableTabRow(
            selectedTabIndex = sections.indexOfFirst { it.first == aiActiveSection },
            containerColor = Color(0xFFFBF8FD),
            edgePadding = 12.dp,
            divider = {}
        ) {
            sections.forEach { (sectionKey, label, icon) ->
                Tab(
                    selected = aiActiveSection == sectionKey,
                    onClick = { aiActiveSection = sectionKey },
                    text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // --- MAIN CONTENT AREA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isAiLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            AnimatedContent(
                targetState = aiActiveSection,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ai_section_animation"
            ) { section ->
                when (section) {
                    "Dashboard" -> DashboardSection(
                        viewModel = viewModel,
                        aiRecommendations = aiRecommendations,
                        worstSubject = analytics.worstSubject.ifEmpty { "Physics" },
                        streak = profile?.streak ?: 77,
                        onNavigateToSection = { aiActiveSection = it }
                    )
                    "Ask AI" -> AskAiSection(
                        viewModel = viewModel,
                        chatHistory = aiChatHistory
                    )
                    "Revision" -> RevisionSection(
                        viewModel = viewModel,
                        revisionMaterial = aiRevisionMaterial,
                        selectedType = aiRevisionType,
                        selectedSubject = aiSelectedSubject
                    )
                    "Plans" -> PlansSection(
                        viewModel = viewModel,
                        studyPlan = aiStudyPlan,
                        selectedMode = aiPlannerMode
                    )
                    "Practice" -> PracticeSection(
                        viewModel = viewModel,
                        analytics = analytics,
                        challengesCount = challenges.count { !it.completed }
                    )
                }
            }
        }
    }
}

// ==========================================
// SECTION 1: DASHBOARD
// ==========================================
@Composable
fun DashboardSection(
    viewModel: PointlyViewModel,
    aiRecommendations: String,
    worstSubject: String,
    streak: Int,
    onNavigateToSection: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Actions Bento Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Welcome to your AI Lab",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "A fully custom, context-aware companion to ace CBSE syllabus.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onNavigateToSection("Ask AI") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Solve Doubts", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onNavigateToSection("Revision") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Revision Cards", fontSize = 11.sp)
                    }
                }
            }
        }

        // Today's Recommendations Card (Bento Style)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = "Recommendations",
                            tint = Color(0xFFFFB300)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Personalized Focus",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.fetchAiRecommendations(forceRefresh = true) },
                        modifier = Modifier.size(32.dp).testTag("refresh_ai_rec_btn")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (aiRecommendations.isNotEmpty()) {
                    Text(
                        text = aiRecommendations,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Two Column Bento Row (Streak status & Weak Areas)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
                modifier = Modifier.weight(1f).height(120.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text("Streak Level", fontSize = 11.sp, color = Color.Gray)
                        Text("$streak Days", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF333333))
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
                modifier = Modifier.weight(1.2f).height(120.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingDown,
                        contentDescription = "Weak Areas",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text("Priority Subject", fontSize = 11.sp, color = Color.Gray)
                        Text(worstSubject, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Daily Reminder Bento Card (Phase 9)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = "Reminders",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Daily AI Study Notification", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Reminds you adaptive lessons daily", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                
                var checked by remember { mutableStateOf(true) }
                Switch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier = Modifier.testTag("notification_toggle")
                )
            }
        }
    }
}

// ==========================================
// SECTION 2: ASK AI
// ==========================================
@Composable
fun AskAiSection(
    viewModel: PointlyViewModel,
    chatHistory: List<Pair<String, Boolean>>
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    val listState = rememberScrollState()

    LaunchedEffect(chatHistory.size) {
        listState.animateScrollTo(listState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Chat Window
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE6E0EC), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (chatHistory.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Need Homework Help or Concept Explanations?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ask any syllabus doubt, equation, or topic. I'll explain step-by-step.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Or select a quick suggestion:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "Explain Friction vs. Static Friction simply",
                            "Give properties of Integers with examples",
                            "Explain Acids, Bases, and Indicators color changes",
                            "Explain how to solve Motion measurement questions"
                        ).forEach { suggestion ->
                            OutlinedButton(
                                onClick = { viewModel.askAiQuery(suggestion) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(suggestion, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(listState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    chatHistory.forEach { (text, isUser) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF3EDF7),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask doubt / explain concept...", fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                trailingIcon = {
                    if (chatHistory.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAiChat() }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Chat", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true
            )
            
            FloatingActionButton(
                onClick = {
                    if (textInput.trim().isNotEmpty()) {
                        viewModel.askAiQuery(textInput)
                        textInput = ""
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("ai_send_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==========================================
// SECTION 3: REVISION LAB
// ==========================================
@Composable
fun RevisionSection(
    viewModel: PointlyViewModel,
    revisionMaterial: String,
    selectedType: String,
    selectedSubject: String
) {
    val types = listOf("Flashcards", "Mind Maps", "Formulas", "Summary")
    val subjects = listOf("Physics", "Chemistry", "Biology", "Mathematics")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Revision Type:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.take(2).forEach { type ->
                        val isSelected = type == selectedType
                        Button(
                            onClick = { viewModel.generateAiRevision(type, selectedSubject) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF3EDF7),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(type, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.drop(2).forEach { type ->
                        val isSelected = type == selectedType
                        Button(
                            onClick = { viewModel.generateAiRevision(type, selectedSubject) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF3EDF7),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(type, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Select Subject:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.take(2).forEach { sub ->
                        val isSelected = sub == selectedSubject
                        Button(
                            onClick = { viewModel.generateAiRevision(selectedType, sub) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else Color(0xFFF3EDF7),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(sub, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjects.drop(2).forEach { sub ->
                        val isSelected = sub == selectedSubject
                        Button(
                            onClick = { viewModel.generateAiRevision(selectedType, sub) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else Color(0xFFF3EDF7),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(sub, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Action Button to force generate
        Button(
            onClick = { viewModel.generateAiRevision(selectedType, selectedSubject, forceRefresh = true) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("generate_revision_btn")
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generate $selectedType")
        }

        // Revision Deck Output
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedType Deck: $selectedSubject",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Room Cached", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (revisionMaterial.isNotEmpty()) {
                    Text(
                        text = revisionMaterial,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No revision card generated yet. Tap Generate to initialize.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 4: STUDY PLANS
// ==========================================
@Composable
fun PlansSection(
    viewModel: PointlyViewModel,
    studyPlan: String,
    selectedMode: String
) {
    val modes = listOf("Daily", "Weekly", "Revision Plan")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Adaptive Planner:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modes.forEach { mode ->
                        val isSelected = mode == selectedMode
                        Button(
                            onClick = { viewModel.generateAiPlan(mode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF3EDF7),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(mode, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.generateAiPlan(selectedMode, forceRefresh = true) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("generate_plan_btn")
        ) {
            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Regenerate $selectedMode Plan")
        }

        // Plan output
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plan: $selectedMode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Icon(Icons.Rounded.OfflinePin, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (studyPlan.isNotEmpty()) {
                    Text(
                        text = studyPlan,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Study schedule not computed yet. Select a mode to generate.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 5: PRACTICE HUB
// ==========================================
@Composable
fun PracticeSection(
    viewModel: PointlyViewModel,
    analytics: com.example.data.model.SyllabusAnalytics,
    challengesCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Core performance metrics
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Performance Trends", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(analytics.overallAccuracy * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Accuracy", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${analytics.totalQuestionsSolved}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text("Solved", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${analytics.averageTimeSeconds.toInt()}s",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF4CAF50)
                        )
                        Text("Avg Time", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Practice launching pad
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Targeted AI Practice Quizzes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.startSyllabusQuiz(QuizMode.RANDOM_REVISION) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("practice_weak_btn")
                ) {
                    Icon(Icons.Rounded.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Practice Weak Chapters")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.startSyllabusQuiz(QuizMode.BOOKMARKED) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("practice_bookmarks_btn")
                ) {
                    Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Practice Bookmarked Doubts")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.startSyllabusQuiz(QuizMode.WRONG_ANSWERS_PRACTICE) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("practice_mistakes_btn")
                ) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Review Previous Mistakes")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.startSyllabusQuiz(QuizMode.DAILY_QUIZ) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("practice_daily_blitz_btn")
                ) {
                    Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Daily 5-Question Blitz")
                }
            }
        }

        // Project Ideas & Collaboration (Phase 8)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE6E0EC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Collaborative Project Ideas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• **Friction Demo**: Collaborate with squadmates to build a digital friction tester using local resources and record your team findings!\n" +
                           "• **Litmus Lab**: Study indicadores changes. Share dynamic photos in the Community Showcase to earn badges.\n" +
                           "• **Integer War**: Duel high scores on Integers in your Study Squad and climb seasonal leaderboards.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
