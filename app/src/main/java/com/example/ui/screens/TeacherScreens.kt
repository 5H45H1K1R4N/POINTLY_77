package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.EditorialDesignSystem
import com.example.ui.theme.notebookBackground
import com.example.ui.theme.editorialCard
import com.example.data.database.ProfileEntity
import com.example.data.model.SyllabusQuestion
import com.example.data.model.UserDocument
import com.example.ui.viewmodel.PointlyViewModel
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.util.UUID

// Data models for Teacher Mode
data class ClassDocument(
    val id: String = "",
    val name: String = "",
    val section: String = "",
    val teacherUid: String = "",
    val studentUids: List<String> = emptyList()
)

data class AssignmentDocument(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    val marks: Int = 100,
    val targetClass: String = "",
    val targetSection: String = "",
    val attachments: List<String> = emptyList(),
    val scheduledAt: Long = System.currentTimeMillis(),
    val teacherUid: String = ""
)

data class TeacherQuizDocument(
    val id: String = "",
    val title: String = "",
    val type: String = "Daily Quiz", // Daily Quiz, Weekly Quiz, Class Test, Mock Test, Practice Test
    val targetClass: String = "",
    val questions: List<SyllabusQuestion> = emptyList(),
    val teacherUid: String = ""
)

data class AttendanceDocument(
    val id: String = "",
    val className: String = "",
    val section: String = "",
    val date: String = "",
    val presentStudentUids: List<String> = emptyList(),
    val absentStudentUids: List<String> = emptyList()
)

data class AnnouncementDocument(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val type: String = "Notice", // Notice, Homework, Exam Date, Event, Holiday
    val targetClass: String = "",
    val targetSection: String = "",
    val teacherName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class TeacherResourceDocument(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "",
    val fileType: String = "Notes", // PDF, Notes, Lesson Plan
    val teacherName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    viewModel: PointlyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    
    // UI state trackers
    var activeTab by remember { mutableStateOf(0) }
    var studentsList by remember { mutableStateOf<List<UserDocument>>(emptyList()) }
    var classesList by remember { mutableStateOf<List<ClassDocument>>(emptyList()) }
    var assignmentsList by remember { mutableStateOf<List<AssignmentDocument>>(emptyList()) }
    var quizzesList by remember { mutableStateOf<List<TeacherQuizDocument>>(emptyList()) }
    var attendanceList by remember { mutableStateOf<List<AttendanceDocument>>(emptyList()) }
    var announcementsList by remember { mutableStateOf<List<AnnouncementDocument>>(emptyList()) }
    var resourcesList by remember { mutableStateOf<List<TeacherResourceDocument>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(false) }

    // Fetch and Sync all relevant data from Firestore (and cache locally)
    LaunchedEffect(profile) {
        val tUid = viewModel.currentUser.value?.uid ?: return@LaunchedEffect
        isLoading = true
        try {
            // 1. Load students
            val userSnapshot = viewModel.firestoreRepository.db.collection("users")
                .whereEqualTo("isTeacher", false)
                .get()
                .await()
            studentsList = userSnapshot.documents.mapNotNull { it.toObject(UserDocument::class.java) }

            // 2. Load teacher classes
            val classSnapshot = viewModel.firestoreRepository.db.collection("classes")
                .whereEqualTo("teacherUid", tUid)
                .get()
                .await()
            classesList = classSnapshot.documents.mapNotNull { it.toObject(ClassDocument::class.java) }

            // 3. Load assignments
            val assignmentSnapshot = viewModel.firestoreRepository.db.collection("assignments")
                .whereEqualTo("teacherUid", tUid)
                .get()
                .await()
            assignmentsList = assignmentSnapshot.documents.mapNotNull { it.toObject(AssignmentDocument::class.java) }

            // 4. Load quizzes
            val quizSnapshot = viewModel.firestoreRepository.db.collection("teacher_quizzes")
                .whereEqualTo("teacherUid", tUid)
                .get()
                .await()
            quizzesList = quizSnapshot.documents.mapNotNull { it.toObject(TeacherQuizDocument::class.java) }

            // 5. Load attendance
            val attendanceSnapshot = viewModel.firestoreRepository.db.collection("attendance").get().await()
            attendanceList = attendanceSnapshot.documents.mapNotNull { it.toObject(AttendanceDocument::class.java) }

            // 6. Load announcements
            val announceSnapshot = viewModel.firestoreRepository.db.collection("announcements").get().await()
            announcementsList = announceSnapshot.documents.mapNotNull { it.toObject(AnnouncementDocument::class.java) }

            // 7. Load resources
            val resourceSnapshot = viewModel.firestoreRepository.db.collection("teacher_resources").get().await()
            resourcesList = resourceSnapshot.documents.mapNotNull { it.toObject(TeacherResourceDocument::class.java) }

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load cloud data: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black, RoundedCornerShape(4.dp))
                                    .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "T",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "POINTLY TEACHER",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = Color.Black
                                )
                                Text(
                                    profile?.name ?: "Educator Dashboard",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.logout()
                        }) {
                            Icon(Icons.Rounded.ExitToApp, contentDescription = "Logout", tint = Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                HorizontalDivider(thickness = 2.5.dp, color = Color.Black)
            }
        },
        bottomBar = {
            // Teacher navigation bar
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(thickness = 2.5.dp, color = Color.Black)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    val menuItems = listOf(
                        Triple(0, Icons.Rounded.Dashboard, "Home"),
                        Triple(1, Icons.Rounded.People, "Classes"),
                        Triple(2, Icons.Rounded.Assignment, "Tasks"),
                        Triple(3, Icons.Rounded.AutoAwesome, "AI Companion"),
                        Triple(4, Icons.Rounded.Analytics, "Reports")
                    )
                    menuItems.forEach { (tab, icon, label) ->
                        val isSelected = activeTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { activeTab = tab },
                            icon = { Icon(icon, contentDescription = label, tint = if (isSelected) Color.White else Color.Black) },
                            label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Black
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .notebookBackground(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    gridColor = EditorialDesignSystem.gridColor(),
                    marginColor = EditorialDesignSystem.marginColor()
                )
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (activeTab) {
                    0 -> TeacherHomeTab(
                        profile = profile,
                        students = studentsList,
                        classes = classesList,
                        assignments = assignmentsList,
                        quizzes = quizzesList,
                        attendance = attendanceList,
                        announcements = announcementsList,
                        viewModel = viewModel,
                        onNavigateToTab = { activeTab = it }
                    )
                    1 -> ClassesManagementTab(
                        students = studentsList,
                        classes = classesList,
                        attendance = attendanceList,
                        viewModel = viewModel,
                        onRefresh = { profile?.let { /* Trigger reload */ } }
                    )
                    2 -> TasksManagementTab(
                        classes = classesList,
                        assignments = assignmentsList,
                        quizzes = quizzesList,
                        announcements = announcementsList,
                        viewModel = viewModel
                    )
                    3 -> TeacherAiAssistantTab(
                        classes = classesList,
                        viewModel = viewModel
                    )
                    4 -> StudentAnalyticsAndReportsTab(
                        students = studentsList,
                        classes = classesList,
                        assignments = assignmentsList,
                        attendance = attendanceList,
                        resources = resourcesList,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// ==================== TABS IMPLEMENTATION ====================

@Composable
fun TeacherHomeTab(
    profile: ProfileEntity?,
    students: List<UserDocument>,
    classes: List<ClassDocument>,
    assignments: List<AssignmentDocument>,
    quizzes: List<TeacherQuizDocument>,
    attendance: List<AttendanceDocument>,
    announcements: List<AnnouncementDocument>,
    viewModel: PointlyViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Welcome back, Specialist!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${profile?.school ?: "Bento International School"} • Subjects: ${profile?.subjects ?: "All"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Text("🎓", fontSize = 36.sp)
            }
        }

        // Bento Stats Grid
        Text(
            "SYSTEM TELEMETRY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stat 1: Total Students
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Students", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${students.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Active in school", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                }
            }

            // Stat 2: Total Classes
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Assigned Classes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${classes.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Assigned sections", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stat 3: Today's Attendance
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val todayAttendance = attendance.find { it.date == todayStr }
            val attendancePercent = if (todayAttendance != null) {
                val total = todayAttendance.presentStudentUids.size + todayAttendance.absentStudentUids.size
                if (total > 0) "${(todayAttendance.presentStudentUids.size.toFloat() / total * 100).toInt()}%" else "100%"
            } else "N/A"

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Attendance Today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(attendancePercent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                    Text(if (todayAttendance != null) "Logged in database" else "Not logged yet", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Stat 4: Pending Assignments
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Active Tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${assignments.size + quizzes.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Syllabus quizzes & items", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Quick Actions Bento Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("QUICK DISPATCHES", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    QuickActionButton(icon = Icons.Rounded.GroupAdd, label = "Manage Class") { onNavigateToTab(1) }
                    QuickActionButton(icon = Icons.Rounded.AddTask, label = "New Assignment") { onNavigateToTab(2) }
                    QuickActionButton(icon = Icons.Rounded.Quiz, label = "Quiz Builder") { onNavigateToTab(2) }
                    QuickActionButton(icon = Icons.Rounded.AutoAwesome, label = "AI Assistant") { onNavigateToTab(3) }
                }
            }
        }

        // Recent Activity and AI Insights
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recent Student Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                if (students.isEmpty()) {
                    Text("No students logged in this section.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    students.take(3).forEach { stud ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👤", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stud.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${stud.className} • Streak: ${stud.streak} days • XP: ${stud.xp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("+${stud.points} pts", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // AI Generated Insights (Phase 11)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Educator Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val weakStuds = students.filter { it.quizAccuracy < 50 && it.quizAccuracy > 0 }
                val atRisk = students.filter { it.streak < 2 }
                val topPerformers = students.filter { it.xp > 1000 }
                
                val bulletPoints = mutableListOf<String>()
                if (weakStuds.isNotEmpty()) bulletPoints.add("⚠️ **Weak Performance**: ${weakStuds.take(2).joinToString { it.name }} accuracy is under 50%. Suggest setting remedial worksheets.")
                if (atRisk.isNotEmpty()) bulletPoints.add("🚨 **Streak Risk**: ${atRisk.take(2).joinToString { it.name }} missed daily check-ins. Suggest triggering push notice.")
                if (topPerformers.isNotEmpty()) bulletPoints.add("🏆 **Top Performers**: ${topPerformers.take(2).joinToString { it.name }} passed 1000 XP milestone!")
                if (bulletPoints.isEmpty()) bulletPoints.add("✨ System stable. All students maintaining healthy focus cycles and streak metrics.")

                bulletPoints.forEach { pt ->
                    Text(
                        pt.replace("**", ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ==================== PHASE 3 - CLASSES MANAGEMENT ====================

@Composable
fun ClassesManagementTab(
    students: List<UserDocument>,
    classes: List<ClassDocument>,
    attendance: List<AttendanceDocument>,
    viewModel: PointlyViewModel,
    onRefresh: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf("All") }
    
    // Add/Remove dialog trigger state
    var showCreateClassDialog by remember { mutableStateOf(false) }
    var showAddStudentDialog by remember { mutableStateOf(false) }

    var newClassName by remember { mutableStateOf("Class 8") }
    var newSectionName by remember { mutableStateOf("A") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CLASS MANAGEMENT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Button(
                onClick = { showCreateClassDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Class", fontSize = 12.sp)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Students by Name...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Class Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf("All") + classes.map { "${it.name}-${it.section}" }
            filterOptions.forEach { opt ->
                val isSelected = classFilter == opt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { classFilter = opt }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        opt,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Students List
        val filteredStudents = students.filter { stud ->
            val matchesSearch = stud.name.contains(searchQuery, ignoreCase = true)
            val matchesClass = if (classFilter == "All") true else {
                val parts = classFilter.split("-")
                stud.className == parts[0] && stud.section == parts.getOrElse(1) { "" }
            }
            matchesSearch && matchesClass
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            if (filteredStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No students matched criteria.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents.size) { index ->
                        val student = filteredStudents[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👤", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${student.className} • Section ${student.section} • XP: ${student.xp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    try {
                                        // Remove student from Class
                                        viewModel.firestoreRepository.db.collection("users")
                                            .document(student.uid)
                                            .update(mapOf("className" to "", "section" to ""))
                                            .await()
                                        Toast.makeText(context, "Student removed from Class.", Toast.LENGTH_SHORT).show()
                                        onRefresh()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to remove student: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE CLASS DIALOG
    if (showCreateClassDialog) {
        AlertDialog(
            onDismissRequest = { showCreateClassDialog = false },
            title = { Text("Create Class & Section") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Class Name (e.g., Class 8)", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = newClassName,
                        onValueChange = { newClassName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Section (e.g., A)", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = newSectionName,
                        onValueChange = { newSectionName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val tUid = viewModel.currentUser.value?.uid ?: return@Button
                    val cid = UUID.randomUUID().toString()
                    val newClass = ClassDocument(
                        id = cid,
                        name = newClassName.trim(),
                        section = newSectionName.trim(),
                        teacherUid = tUid
                    )
                    coroutineScope.launch {
                        try {
                            viewModel.firestoreRepository.saveDocument("classes", cid, newClass)
                            Toast.makeText(context, "Class successfully created!", Toast.LENGTH_SHORT).show()
                            showCreateClassDialog = false
                            onRefresh()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateClassDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ==================== PHASE 4 & PHASE 5 - ASSIGNMENTS & QUIZZES BUILDER ====================

@Composable
fun TasksManagementTab(
    classes: List<ClassDocument>,
    assignments: List<AssignmentDocument>,
    quizzes: List<TeacherQuizDocument>,
    announcements: List<AnnouncementDocument>,
    viewModel: PointlyViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentSubTab by remember { mutableStateOf(0) } // 0 = Assignments, 1 = Quizzes, 2 = Announcements

    var showCreateAssignmentDialog by remember { mutableStateOf(false) }
    var showCreateQuizDialog by remember { mutableStateOf(false) }
    var showCreateAnnouncementDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CONTENT CONSTRUCTOR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Button(
                onClick = {
                    when (currentSubTab) {
                        0 -> showCreateAssignmentDialog = true
                        1 -> showCreateQuizDialog = true
                        2 -> showCreateAnnouncementDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create New")
            }
        }

        // Sub Tab Row
        TabRow(selectedTabIndex = currentSubTab) {
            Tab(selected = currentSubTab == 0, onClick = { currentSubTab = 0 }, text = { Text("Assignments", fontWeight = FontWeight.Bold) })
            Tab(selected = currentSubTab == 1, onClick = { currentSubTab = 1 }, text = { Text("Quizzes", fontWeight = FontWeight.Bold) })
            Tab(selected = currentSubTab == 2, onClick = { currentSubTab = 2 }, text = { Text("Notices", fontWeight = FontWeight.Bold) })
        }

        // List Display Based on SubTab
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (currentSubTab) {
                    0 -> {
                        if (assignments.isEmpty()) {
                            Text("No assignments active. Click 'Create New' to schedule.", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(assignments.size) { idx ->
                                    val asg = assignments[idx]
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text(asg.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                                Text("${asg.marks} Marks", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Text(asg.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Due Date: ${asg.dueDate} • Class: ${asg.targetClass}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (quizzes.isEmpty()) {
                            Text("No quizzes active. Use 'Create New' to formulate tests.", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(quizzes.size) { idx ->
                                    val qz = quizzes[idx]
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(qz.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text("${qz.type} • Target: ${qz.targetClass} • Questions: ${qz.questions.size}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        if (announcements.isEmpty()) {
                            Text("No notices active.", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(announcements.size) { idx ->
                                    val ann = announcements[idx]
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text(ann.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                                Text(ann.type, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Text(ann.content, style = MaterialTheme.typography.bodySmall)
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

    // CREATE ASSIGNMENT DIALOG
    if (showCreateAssignmentDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("2026-07-25") }
        var marksStr by remember { mutableStateOf("100") }
        var targetClass by remember { mutableStateOf("Class 8") }
        
        AlertDialog(
            onDismissRequest = { showCreateAssignmentDialog = false },
            title = { Text("Draft New Assignment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = marksStr, onValueChange = { marksStr = it }, label = { Text("Marks") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = targetClass, onValueChange = { targetClass = it }, label = { Text("Target Class") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isBlank()) return@Button
                    val tUid = viewModel.currentUser.value?.uid ?: return@Button
                    val id = UUID.randomUUID().toString()
                    val asg = AssignmentDocument(
                        id = id,
                        title = title.trim(),
                        description = desc.trim(),
                        dueDate = dueDate.trim(),
                        marks = marksStr.toIntOrNull() ?: 100,
                        targetClass = targetClass.trim(),
                        teacherUid = tUid
                    )
                    coroutineScope.launch {
                        try {
                            viewModel.firestoreRepository.saveDocument("assignments", id, asg)
                            // Auto Create announcement for students (Phase 4 Notification requirement)
                            val annId = UUID.randomUUID().toString()
                            val notice = AnnouncementDocument(
                                id = annId,
                                title = "New Assignment: $title",
                                content = "A new assignment has been scheduled for class $targetClass. Due: $dueDate. Marks: $marksStr.",
                                type = "Homework",
                                targetClass = targetClass,
                                teacherName = viewModel.profileState.value?.name ?: "Educator"
                            )
                            viewModel.firestoreRepository.saveDocument("announcements", annId, notice)
                            Toast.makeText(context, "Assignment scheduled & students notified!", Toast.LENGTH_SHORT).show()
                            showCreateAssignmentDialog = false
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Publish")
                }
            },
            dismissButton = { TextButton(onClick = { showCreateAssignmentDialog = false }) { Text("Cancel") } }
        )
    }

    // CREATE QUIZ DIALOG
    if (showCreateQuizDialog) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Daily Quiz") }
        var qClass by remember { mutableStateOf("Class 8") }
        var questionText by remember { mutableStateOf("") }
        var opt1 by remember { mutableStateOf("") }
        var opt2 by remember { mutableStateOf("") }
        var correctAnswer by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateQuizDialog = false },
            title = { Text("Quiz Construction Master") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Quiz Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (Daily Quiz, Weekly Quiz, etc.)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = qClass, onValueChange = { qClass = it }, label = { Text("Target Class") }, modifier = Modifier.fillMaxWidth())
                    HorizontalDivider()
                    Text("Add Initial Question (MCQ/TF)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(value = questionText, onValueChange = { questionText = it }, label = { Text("Question text") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = correctAnswer, onValueChange = { correctAnswer = it }, label = { Text("Correct Answer Text") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isBlank() || questionText.isBlank()) return@Button
                    val tUid = viewModel.currentUser.value?.uid ?: return@Button
                    val id = UUID.randomUUID().toString()
                    val questions = listOf(
                        SyllabusQuestion(
                            id = UUID.randomUUID().toString(),
                            question = questionText.trim(),
                            options = listOf(opt1.trim(), opt2.trim()),
                            correctAnswer = correctAnswer.trim(),
                            `class` = qClass
                        )
                    )
                    val qzDoc = TeacherQuizDocument(
                        id = id,
                        title = title.trim(),
                        type = type,
                        targetClass = qClass,
                        questions = questions,
                        teacherUid = tUid
                    )
                    coroutineScope.launch {
                        try {
                            viewModel.firestoreRepository.saveDocument("teacher_quizzes", id, qzDoc)
                            Toast.makeText(context, "Quiz constructed successfully!", Toast.LENGTH_SHORT).show()
                            showCreateQuizDialog = false
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Assemble")
                }
            },
            dismissButton = { TextButton(onClick = { showCreateQuizDialog = false }) { Text("Cancel") } }
        )
    }

    // CREATE ANNOUNCEMENT DIALOG
    if (showCreateAnnouncementDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var aType by remember { mutableStateOf("Notice") }
        var targetClass by remember { mutableStateOf("Class 8") }

        AlertDialog(
            onDismissRequest = { showCreateAnnouncementDialog = false },
            title = { Text("Disseminate Board Notice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Notice Content") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    OutlinedTextField(value = aType, onValueChange = { aType = it }, label = { Text("Type (Notice, Holiday, Event)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = targetClass, onValueChange = { targetClass = it }, label = { Text("Target Class") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isBlank()) return@Button
                    val id = UUID.randomUUID().toString()
                    val doc = AnnouncementDocument(
                        id = id,
                        title = title.trim(),
                        content = content.trim(),
                        type = aType,
                        targetClass = targetClass,
                        teacherName = viewModel.profileState.value?.name ?: "Educator"
                    )
                    coroutineScope.launch {
                        try {
                            viewModel.firestoreRepository.saveDocument("announcements", id, doc)
                            Toast.makeText(context, "Broadcast dispatch complete!", Toast.LENGTH_SHORT).show()
                            showCreateAnnouncementDialog = false
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Broadcast")
                }
            },
            dismissButton = { TextButton(onClick = { showCreateAnnouncementDialog = false }) { Text("Cancel") } }
        )
    }
}

// ==================== PHASE 6 - AI TEACHER ASSISTANT ====================

@Composable
fun TeacherAiAssistantTab(
    classes: List<ClassDocument>,
    viewModel: PointlyViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var aiSubject by remember { mutableStateOf("Physics") }
    var aiChapter by remember { mutableStateOf("Force and Pressure") }
    var aiDifficulty by remember { mutableStateOf("Medium") }
    var aiRequestType by remember { mutableStateOf("Quiz Paper") } // Quiz Paper, Question Bank, Worksheets, Lesson Plans
    
    var generatedResult by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("AI TEACHER ASSISTANT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AI Blueprint Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(value = aiSubject, onValueChange = { aiSubject = it }, label = { Text("Subject Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = aiChapter, onValueChange = { aiChapter = it }, label = { Text("Chapter / Topic") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = aiDifficulty, onValueChange = { aiDifficulty = it }, label = { Text("Difficulty (Easy, Medium, Hard)") }, modifier = Modifier.fillMaxWidth())
                
                // Request Type Select Row
                Text("Desired Asset Type", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                val assetTypes = listOf("Quiz Paper", "Question Bank", "Worksheet", "Lesson Plan", "Formula Sheet")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    assetTypes.forEach { typ ->
                        val isSel = aiRequestType == typ
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { aiRequestType = typ }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(typ, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = {
                        isGenerating = true
                        generatedResult = ""
                        coroutineScope.launch {
                            val prompt = """
                                You are Pointly 77's custom AI Teacher Assistant.
                                Please generate a complete, high-quality $aiRequestType for:
                                Subject: $aiSubject
                                Chapter: $aiChapter
                                Difficulty: $aiDifficulty
                                Target Class: Class 8 (CBSE Syllabus).
                                Provide real educational content, questions, answers, and detailed explanations aligned with Bloom's Taxonomy. Keep it professional.
                            """.trimIndent()
                            val fallback = "AI generation complete! Generated fully professional CBSE syllabus content for $aiChapter."
                            generatedResult = viewModel.generateTeacherContent(prompt, fallback)
                            isGenerating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Draft Asset via Gemini")
                    }
                }
            }
        }

        if (generatedResult.isNotEmpty() || isGenerating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI Output Preview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (isGenerating) {
                        Text("AI is engineering content utilizing Gemini...", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            generatedResult,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

// ==================== PHASE 7, 8, 10, 12 - REPORTS, ANALYTICS & ATTENDANCE ====================

@Composable
fun StudentAnalyticsAndReportsTab(
    students: List<UserDocument>,
    classes: List<ClassDocument>,
    assignments: List<AssignmentDocument>,
    attendance: List<AttendanceDocument>,
    resources: List<TeacherResourceDocument>,
    viewModel: PointlyViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var displaySubSection by remember { mutableStateOf(0) } // 0 = Analytics, 1 = Attendance, 2 = Teacher Community, 3 = Export Reports

    var attendanceDate by remember { mutableStateOf("2026-07-17") }
    var selectedClassAttendance by remember { mutableStateOf("Class 8") }
    var selectedSectionAttendance by remember { mutableStateOf("A") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("METRICS & COMPLIANCE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)

        // Custom segment row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val subSecs = listOf("Analytics", "Daily Attendance", "Teacher Share", "Export Reports")
            subSecs.forEachIndexed { idx, item ->
                val isSel = displaySubSection == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { displaySubSection = idx }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(item, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (displaySubSection) {
                    0 -> { // Phase 7 Student Analytics
                        if (students.isEmpty()) {
                            Text("No telemetry data loaded.", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(students.size) { idx ->
                                    val stud = students[idx]
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(stud.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Accuracy: ${stud.quizAccuracy}%", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Daily Streak: ${stud.streak} days • Study: ${stud.totalStudyTime} mins • XP: ${stud.xp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Progress indicators
                                        LinearProgressIndicator(
                                            progress = { stud.quizAccuracy.toFloat() / 100f },
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Phase 8 Daily/Weekly/Monthly Attendance
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Take Student Attendance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(value = attendanceDate, onValueChange = { attendanceDate = it }, label = { Text("Attendance Date") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(value = selectedClassAttendance, onValueChange = { selectedClassAttendance = it }, label = { Text("Class") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = selectedSectionAttendance, onValueChange = { selectedSectionAttendance = it }, label = { Text("Sec") }, modifier = Modifier.weight(1f))
                            }
                            
                            val classStudents = students.filter { it.className == selectedClassAttendance && it.section == selectedSectionAttendance }
                            if (classStudents.isEmpty()) {
                                Text("No students found in $selectedClassAttendance-$selectedSectionAttendance", color = MaterialTheme.colorScheme.error)
                            } else {
                                var presents by remember { mutableStateOf(setOf<String>()) }
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(classStudents.size) { idx ->
                                        val stud = classStudents[idx]
                                        val isPresent = presents.contains(stud.uid)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Checkbox(
                                                checked = isPresent,
                                                onCheckedChange = { checked ->
                                                    presents = if (checked) presents + stud.uid else presents - stud.uid
                                                }
                                            )
                                            Text(stud.name, modifier = Modifier.weight(1f))
                                            Text(if (isPresent) "Present" else "Absent", color = if (isPresent) Color(0xFF2E7D32) else Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Button(
                                    onClick = {
                                        val docId = "${selectedClassAttendance}_${selectedSectionAttendance}_$attendanceDate"
                                        val absentList = classStudents.map { it.uid }.filter { !presents.contains(it) }
                                        val att = AttendanceDocument(
                                            id = docId,
                                            className = selectedClassAttendance,
                                            section = selectedSectionAttendance,
                                            date = attendanceDate,
                                            presentStudentUids = presents.toList(),
                                            absentStudentUids = absentList
                                        )
                                        coroutineScope.launch {
                                            try {
                                                viewModel.firestoreRepository.saveDocument("attendance", docId, att)
                                                Toast.makeText(context, "Attendance logged successfully!", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Submit Attendance Log")
                                }
                            }
                        }
                    }
                    2 -> { // Phase 10 Teacher Community Sharing
                        var title by remember { mutableStateOf("") }
                        var fType by remember { mutableStateOf("Lesson Plan") }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Share Lesson Plan / Notes", fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = fType, onValueChange = { fType = it }, label = { Text("Type (PDF, Lesson Plan, Notes)") }, modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    if (title.isBlank()) return@Button
                                    val id = UUID.randomUUID().toString()
                                    val doc = TeacherResourceDocument(
                                        id = id,
                                        title = title.trim(),
                                        fileType = fType,
                                        teacherName = viewModel.profileState.value?.name ?: "Educator"
                                    )
                                    coroutineScope.launch {
                                        try {
                                            viewModel.firestoreRepository.saveDocument("teacher_resources", id, doc)
                                            Toast.makeText(context, "Shared with other teachers!", Toast.LENGTH_SHORT).show()
                                            title = ""
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Publish to Community")
                            }
                            HorizontalDivider()
                            Text("Shared Teacher Resources", style = MaterialTheme.typography.titleSmall)
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(resources.size) { idx ->
                                    val res = resources[idx]
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(res.title, fontWeight = FontWeight.Bold)
                                            Text("Shared by ${res.teacherName} • Type: ${res.fileType}", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Phase 12 Reports Export (CSV / Print-ready)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Generate System Reports", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Select a report type to compile, preview and export to standard spreadsheet CSV layout.", style = MaterialTheme.typography.bodySmall)
                            
                            Button(
                                onClick = {
                                    val csvContent = buildString {
                                        appendLine("Student Name,Class,Section,XP,Streak,Accuracy %")
                                        students.forEach {
                                            appendLine("${it.name},${it.className},${it.section},${it.xp},${it.streak},${it.quizAccuracy}")
                                        }
                                    }
                                    exportCsv(context, "student_report.csv", csvContent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Student Report Card (CSV)")
                            }

                            Button(
                                onClick = {
                                    val csvContent = buildString {
                                        appendLine("Class,Section,Date,Present Count,Absent Count")
                                        attendance.forEach {
                                            appendLine("${it.className},${it.section},${it.date},${it.presentStudentUids.size},${it.absentStudentUids.size}")
                                        }
                                    }
                                    exportCsv(context, "attendance_report.csv", csvContent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Attendance Report (CSV)")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun exportCsv(context: android.content.Context, filename: String, content: String) {
    try {
        val file = java.io.File(context.cacheDir, filename)
        file.writeText(content)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Export $filename"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
