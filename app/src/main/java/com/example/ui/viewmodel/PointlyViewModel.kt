package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AchievementEntity
import com.example.data.database.ProfileEntity
import com.example.data.database.StudyMissionEntity
import com.example.data.database.ActivityEntity
import com.example.data.database.PomodoroStateEntity
import com.example.data.database.LeaderboardEntryEntity
import com.example.data.database.SyllabusQuestionEntity
import com.example.data.database.SyllabusQuizAttemptEntity
import com.example.data.database.BadgeEntity
import com.example.data.database.ChallengeEntity
import com.example.data.database.DailyLoginRewardEntity
import com.example.data.database.ShopItemEntity
import com.example.data.database.AiCacheEntity
import com.example.data.api.GeminiRequest
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.data.api.GeminiGenerationConfig
import com.example.data.api.RetrofitClient
import com.example.data.model.UserDocument
import com.example.data.model.SyllabusQuestion
import com.example.data.model.QuizMode
import com.example.data.model.SyllabusAnalytics
import com.example.data.repository.PointlyRepository
import com.example.data.repository.SyllabusEngine
import com.example.data.repository.AuthRepository
import com.example.data.api.QuizQuestion
import com.example.data.api.QuizResponse
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import com.example.ui.screens.AuditLogEntry
import com.example.ui.screens.SchoolDocument

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val error: String) : AuthUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class PointlyViewModel(application: Application) : AndroidViewModel(application) {
    val firestoreRepository = com.example.data.repository.FirestoreRepository()
    private val repository = PointlyRepository(application, firestoreRepository)
    private val authRepository = AuthRepository(application, firestoreRepository)
    val connectivityObserver: com.example.data.repository.ConnectivityObserver = com.example.data.repository.NetworkConnectivityObserver(application)
    val syncManager = com.example.data.repository.SyncManager(application, firestoreRepository, connectivityObserver)
    val communityRepository = com.example.data.repository.CommunityRepository(application, firestoreRepository)

    val connectionStatus: StateFlow<com.example.data.repository.ConnectivityObserver.Status> = connectivityObserver.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.data.repository.ConnectivityObserver.Status.Offline
        )

    val syncState: StateFlow<com.example.data.repository.SyncManager.SyncState> = syncManager.syncState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.data.repository.SyncManager.SyncState.Idle
        )

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUserState

    private val _isEmailVerified = MutableStateFlow(true)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // Theme preference state
    val appTheme = MutableStateFlow("System")

    // Username checking state
    val isUsernameAvailable = MutableStateFlow<Boolean?>(null)
    val isCheckingUsername = MutableStateFlow(false)
    private var usernameCheckJob: Job? = null

    // Room Database Flows
    val profileState: StateFlow<ProfileEntity?> = repository.profileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val missionsState: StateFlow<List<StudyMissionEntity>> = repository.missionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val achievementsState: StateFlow<List<AchievementEntity>> = repository.achievementsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val badgesState: StateFlow<List<BadgeEntity>> = repository.badgesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val challengesState: StateFlow<List<ChallengeEntity>> = repository.challengesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailyRewardsState: StateFlow<List<DailyLoginRewardEntity>> = repository.dailyRewardsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val shopItemsState: StateFlow<List<ShopItemEntity>> = repository.shopItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Tab State (0 = Home/Bento, 1 = Missions, 2 = Social/Leaderboard, 3 = Profile Achievements)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _isFocusZoneActive = MutableStateFlow(false)
    val isFocusZoneActive: StateFlow<Boolean> = _isFocusZoneActive.asStateFlow()

    fun setFocusZoneActive(active: Boolean) {
        _isFocusZoneActive.value = active
    }

    // Syllabus Engine State Flows
    private val _selectedSyllabusClass = MutableStateFlow("Class 8")
    val selectedSyllabusClass: StateFlow<String> = _selectedSyllabusClass.asStateFlow()

    private val _selectedSyllabusSubject = MutableStateFlow<String?>(null)
    val selectedSyllabusSubject: StateFlow<String?> = _selectedSyllabusSubject.asStateFlow()

    private val _selectedSyllabusChapter = MutableStateFlow<String?>(null)
    val selectedSyllabusChapter: StateFlow<String?> = _selectedSyllabusChapter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val syllabusQuestionsState = _selectedSyllabusClass.flatMapLatest { className ->
        repository.getSyllabusQuestionsFlow(className)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedQuestionsState = repository.getBookmarkedQuestionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wrongQuestionsState = repository.getWrongQuestionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syllabusAttemptsState = repository.getAttemptsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Syllabus Quiz States
    private val _activeSyllabusQuiz = MutableStateFlow<List<SyllabusQuestion>?>(null)
    val activeSyllabusQuiz: StateFlow<List<SyllabusQuestion>?> = _activeSyllabusQuiz.asStateFlow()

    private val _activeSyllabusQuizIndex = MutableStateFlow(0)
    val activeSyllabusQuizIndex: StateFlow<Int> = _activeSyllabusQuizIndex.asStateFlow()

    private val _activeSyllabusSelectedOption = MutableStateFlow<Int?>(null)
    val activeSyllabusSelectedOption: StateFlow<Int?> = _activeSyllabusSelectedOption.asStateFlow()

    private val _activeSyllabusIsChecked = MutableStateFlow(false)
    val activeSyllabusIsChecked: StateFlow<Boolean> = _activeSyllabusIsChecked.asStateFlow()

    private val _activeSyllabusIsCorrect = MutableStateFlow(false)
    val activeSyllabusIsCorrect: StateFlow<Boolean> = _activeSyllabusIsCorrect.asStateFlow()

    private val _activeSyllabusScore = MutableStateFlow(0)
    val activeSyllabusScore: StateFlow<Int> = _activeSyllabusScore.asStateFlow()

    private val _activeSyllabusXpEarned = MutableStateFlow(0)
    val activeSyllabusXpEarned: StateFlow<Int> = _activeSyllabusXpEarned.asStateFlow()

    private val _activeSyllabusPointsEarned = MutableStateFlow(0)
    val activeSyllabusPointsEarned: StateFlow<Int> = _activeSyllabusPointsEarned.asStateFlow()

    private val _activeSyllabusMode = MutableStateFlow<QuizMode?>(null)
    val activeSyllabusMode: StateFlow<QuizMode?> = _activeSyllabusMode.asStateFlow()

    private val _activeSyllabusTimeSpent = MutableStateFlow(0)
    val activeSyllabusTimeSpent: StateFlow<Int> = _activeSyllabusTimeSpent.asStateFlow()

    private var syllabusTimerJob: Job? = null

    val syllabusAnalyticsState = combine(syllabusQuestionsState, syllabusAttemptsState) { questions, attempts ->
        val qList = questions.map { SyllabusQuestion.fromEntity(it) }
        SyllabusEngine.calculateAnalytics(qList, attempts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyllabusAnalytics())

    // Super Admin Exclusive / Support Mode Impersonation states
    val isImpersonating = MutableStateFlow(false)
    val originalSuperAdminProfile = MutableStateFlow<ProfileEntity?>(null)

    // Global Feature Flags (e.g. AI Study Companion, Community, Showcase, etc.)
    val featureFlags = MutableStateFlow<Map<String, Boolean>>(mapOf(
        "AI Study Companion" to true,
        "Community" to true,
        "Showcase" to true,
        "Leaderboards" to true,
        "Parent Mode" to true,
        "Teacher Mode" to true,
        "Gamification" to true,
        "Portfolio" to true,
        "Resume Builder" to true,
        "Notifications" to true
    ))

    // Maintenance Mode States
    val maintenanceModeActive = MutableStateFlow(false)
    val maintenanceTarget = MutableStateFlow("None") // "Entire Platform", "Selected School", or "None"
    val maintenanceSchoolId = MutableStateFlow("")

    // Global Branding settings
    val brandingAppName = MutableStateFlow("Pointly")
    val brandingLogoUrl = MutableStateFlow("")
    val brandingDefaultTheme = MutableStateFlow("Dynamic Purple")
    val brandingAccentColor = MutableStateFlow("#6200EE")

    // Emergency Controls
    val emergencyForceLogoutAll = MutableStateFlow(false)
    val emergencyDisableRegistrations = MutableStateFlow(false)
    val emergencyLockCommunity = MutableStateFlow(false)
    val emergencyDisableAIRequests = MutableStateFlow(false)
    val emergencyPauseLeaderboards = MutableStateFlow(false)
    val emergencyPauseNotifications = MutableStateFlow(false)

    fun startImpersonating(targetUser: UserDocument) {
        viewModelScope.launch {
            val current = profileState.value
            if (current != null && originalSuperAdminProfile.value == null) {
                originalSuperAdminProfile.value = current
            }
            
            // Build the impersonated profile
            val impersonatedProfile = ProfileEntity(
                id = 1,
                name = targetUser.name,
                username = targetUser.username,
                school = targetUser.school,
                organizationId = targetUser.organizationId,
                className = targetUser.className,
                section = targetUser.section,
                streak = 77,
                xp = 1500,
                coins = 500,
                gems = 50,
                profileImage = targetUser.profileImage,
                isTeacher = targetUser.isTeacher,
                isAdmin = targetUser.isAdmin,
                adminRole = if (targetUser.isAdmin) "School Admin" else ""
            )
            
            // Log to Audit Logs in Firestore
            val auditLog = AuditLogEntry(
                id = "audit_${UUID.randomUUID().toString().take(6)}",
                timestamp = System.currentTimeMillis(),
                actorName = originalSuperAdminProfile.value?.name ?: "Super Admin",
                actorRole = "Super Admin",
                actionType = "USER_IMPERSONATION",
                description = "Started support impersonation of user ${targetUser.name} (${targetUser.username})"
            )
            firestoreRepository.db.collection("audit_logs").document(auditLog.id).set(auditLog)
            
            // Update local Room database
            repository.updateProfile(impersonatedProfile)
            isImpersonating.value = true
        }
    }

    fun stopImpersonating() {
        viewModelScope.launch {
            val original = originalSuperAdminProfile.value
            if (original != null) {
                // Log to Audit Logs in Firestore
                val auditLog = AuditLogEntry(
                    id = "audit_${UUID.randomUUID().toString().take(6)}",
                    timestamp = System.currentTimeMillis(),
                    actorName = "Super Admin",
                    actorRole = "Super Admin",
                    actionType = "END_IMPERSONATION",
                    description = "Ended support impersonation and restored Super Admin session"
                )
                firestoreRepository.db.collection("audit_logs").document(auditLog.id).set(auditLog)
                
                repository.updateProfile(original)
                originalSuperAdminProfile.value = null
            }
            isImpersonating.value = false
        }
    }

    // Study Panel & Session States
    private val _isStudySessionActive = MutableStateFlow(false)
    val isStudySessionActive: StateFlow<Boolean> = _isStudySessionActive.asStateFlow()

    private val _studyTimerSeconds = MutableStateFlow(0)
    val studyTimerSeconds: StateFlow<Int> = _studyTimerSeconds.asStateFlow()

    private val _activeStudySubject = MutableStateFlow("Physics")
    val activeStudySubject: StateFlow<String> = _activeStudySubject.asStateFlow()

    private val _activeStudyTopic = MutableStateFlow("Bernoulli's Principle")
    val activeStudyTopic: StateFlow<String> = _activeStudyTopic.asStateFlow()

    // Gemini API Quiz State
    private val _isQuizLoading = MutableStateFlow(false)
    val isQuizLoading: StateFlow<Boolean> = _isQuizLoading.asStateFlow()

    private val _currentQuiz = MutableStateFlow<QuizResponse?>(null)
    val currentQuiz: StateFlow<QuizResponse?> = _currentQuiz.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedAnswerIndex = MutableStateFlow<Int?>(null)
    val selectedAnswerIndex: StateFlow<Int?> = _selectedAnswerIndex.asStateFlow()

    private val _isAnswerChecked = MutableStateFlow(false)
    val isAnswerChecked: StateFlow<Boolean> = _isAnswerChecked.asStateFlow()

    private val _isAnswerCorrect = MutableStateFlow(false)
    val isAnswerCorrect: StateFlow<Boolean> = _isAnswerCorrect.asStateFlow()

    // Gamified Rewards Feedback State
    private val _celebrationMessage = MutableStateFlow<String?>(null)
    val celebrationMessage: StateFlow<String?> = _celebrationMessage.asStateFlow()

    private val _totalSessionXpEarned = MutableStateFlow(0)
    val totalSessionXpEarned: StateFlow<Int> = _totalSessionXpEarned.asStateFlow()

    // Editing Profile Dialog
    private val _isEditingProfile = MutableStateFlow(false)
    val isEditingProfile: StateFlow<Boolean> = _isEditingProfile.asStateFlow()

    // Simulated Collaborative Community Study Goal
    private val _communityStudyHours = MutableStateFlow(72.5f)
    val communityStudyHours: StateFlow<Float> = _communityStudyHours.asStateFlow()

    private var timerJob: Job? = null

    // Activities database flows
    val activitiesState: StateFlow<List<ActivityEntity>> = repository.activitiesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pomodoroState: StateFlow<PomodoroStateEntity?> = repository.pomodoroStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val statsState: StateFlow<ActivityStats> = activitiesState.map { list ->
        val calendar = java.util.Calendar.getInstance()
        
        // Today start
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        
        // Week start
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val weekStart = calendar.timeInMillis
        
        // Month start
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis
        
        val completed = list.filter { it.completed }
        
        val todayTime = completed.filter { it.endTime >= todayStart }.sumOf { it.duration } / 60
        val weeklyTime = completed.filter { it.endTime >= weekStart }.sumOf { it.duration } / 60
        val monthlyTime = completed.filter { it.endTime >= monthStart }.sumOf { it.duration } / 60
        val totalTime = completed.sumOf { it.duration } / 60
        
        val activitiesCompleted = completed.size
        val avgSession = if (completed.isNotEmpty()) (completed.sumOf { it.duration } / completed.size) / 60 else 0
        
        val streak = calculateStreak(completed)
        val focusScore = if (completed.isEmpty()) 0 else (completed.filter { it.duration >= 25 * 60 }.size * 5 + avgSession / 4).coerceIn(0, 100)
        
        ActivityStats(
            todayStudyMinutes = todayTime,
            weeklyStudyMinutes = weeklyTime,
            monthlyStudyMinutes = monthlyTime,
            totalStudyMinutes = totalTime,
            currentStreak = streak,
            longestStreak = streak,
            activitiesCompleted = activitiesCompleted,
            averageSessionMinutes = avgSession,
            focusScore = focusScore
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActivityStats()
    )

    private fun calculateStreak(activities: List<ActivityEntity>): Int {
        if (activities.isEmpty()) return 0
        val days = activities.map {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = it.endTime
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()
        
        if (days.isEmpty()) return 0
        
        val todayCal = java.util.Calendar.getInstance()
        todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        todayCal.set(java.util.Calendar.MINUTE, 0)
        todayCal.set(java.util.Calendar.SECOND, 0)
        todayCal.set(java.util.Calendar.MILLISECOND, 0)
        val today = todayCal.timeInMillis
        val yesterday = today - 24 * 60 * 60 * 1000
        
        if (days[0] < yesterday && days[0] != today) {
            return 0
        }
        
        var currentStreak = 1
        for (i in 0 until days.size - 1) {
            val diff = days[i] - days[i + 1]
            if (diff <= 25 * 60 * 60 * 1000) {
                currentStreak++
            } else {
                break
            }
        }
        return currentStreak
    }

    private var pomodoroJob: Job? = null

    enum class LeaderboardScope { CLASS, SECTION, SCHOOL }
    enum class LeaderboardFilter { TODAY, WEEKLY, MONTHLY, ALL_TIME }

    private val _leaderboardScope = MutableStateFlow(LeaderboardScope.SCHOOL)
    val leaderboardScope: StateFlow<LeaderboardScope> = _leaderboardScope.asStateFlow()

    private val _leaderboardFilter = MutableStateFlow(LeaderboardFilter.ALL_TIME)
    val leaderboardFilter: StateFlow<LeaderboardFilter> = _leaderboardFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLeaderboardRefreshing = MutableStateFlow(false)
    val isLeaderboardRefreshing: StateFlow<Boolean> = _isLeaderboardRefreshing.asStateFlow()

    private val _leaderboardLimit = MutableStateFlow(50)
    val leaderboardLimit: StateFlow<Int> = _leaderboardLimit.asStateFlow()

    private val _currentUserDocument = MutableStateFlow<UserDocument?>(null)
    val currentUserDocument: StateFlow<UserDocument?> = _currentUserDocument.asStateFlow()

    init {
        // Load stored theme preference
        try {
            val sharedPref = application.getSharedPreferences("pointly_prefs", Context.MODE_PRIVATE)
            appTheme.value = sharedPref.getString("app_theme", "System") ?: "System"
        } catch (e: Exception) {
            Log.e("PointlyViewModel", "Error loading theme preference", e)
        }

        // Initialize and pre-seed the Room DB if empty
        viewModelScope.launch {
            repository.initializeDatabase()
            // Initial local sync for default class
            syncSyllabus(_selectedSyllabusClass.value)
        }

        // Recovery check for Pomodoro
        checkAndRecoverPomodoro()

        // Listen for user state to trigger bidirectional sync and start snapshot listeners
        viewModelScope.launch {
            authRepository.currentUserState.collect { user ->
                _isEmailVerified.value = true
                if (user != null) {
                    try {
                        syncManager.performSync()
                    } catch (e: Exception) {
                        Log.e("PointlyViewModel", "Automatic bidirectional sync failed", e)
                    }
                    startLeaderboardListener()
                    // Real-time listener for current user document
                    firestoreRepository.listenDocument("users", user.uid, UserDocument::class.java)
                        .collect { doc ->
                            _currentUserDocument.value = doc
                            if (doc != null && doc.className.isNotEmpty()) {
                                _selectedSyllabusClass.value = doc.className
                                syncSyllabus(doc.className)
                            }
                        }
                } else {
                    _currentUserDocument.value = null
                }
            }
        }
    }

    val leaderboardEntries: StateFlow<List<LeaderboardEntryEntity>> = combine(
        repository.leaderboardFlow,
        _leaderboardScope,
        _leaderboardFilter,
        _searchQuery,
        _currentUserDocument
    ) { cachedEntries, scope, filter, query, currentUserDoc ->
        var filteredList = cachedEntries
        if (currentUserDoc != null) {
            filteredList = when (scope) {
                LeaderboardScope.SCHOOL -> cachedEntries
                LeaderboardScope.CLASS -> cachedEntries.filter { it.className.equals(currentUserDoc.className, ignoreCase = true) }
                LeaderboardScope.SECTION -> cachedEntries.filter { 
                    it.className.equals(currentUserDoc.className, ignoreCase = true) && 
                    it.section.equals(currentUserDoc.section, ignoreCase = true)
                }
            }
        }
        
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.username.contains(query, ignoreCase = true)
            }
        }
        
        filteredList.sortedWith { a, b ->
            val pointsA = when (filter) {
                LeaderboardFilter.TODAY -> a.todayPoints
                LeaderboardFilter.WEEKLY -> a.weeklyPoints
                LeaderboardFilter.MONTHLY -> a.monthlyPoints
                LeaderboardFilter.ALL_TIME -> a.points
            }
            val pointsB = when (filter) {
                LeaderboardFilter.TODAY -> b.todayPoints
                LeaderboardFilter.WEEKLY -> b.weeklyPoints
                LeaderboardFilter.MONTHLY -> b.monthlyPoints
                LeaderboardFilter.ALL_TIME -> b.points
            }
            if (pointsB != pointsA) return@sortedWith pointsB.compareTo(pointsA)
            if (b.xp != a.xp) return@sortedWith b.xp.compareTo(a.xp)
            if (b.activitiesCompleted != a.activitiesCompleted) return@sortedWith b.activitiesCompleted.compareTo(a.activitiesCompleted)
            b.totalStudyTime.compareTo(a.totalStudyTime)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setLeaderboardScope(scope: LeaderboardScope) {
        _leaderboardScope.value = scope
    }

    fun setLeaderboardFilter(filter: LeaderboardFilter) {
        _leaderboardFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private var leaderboardListenerJob: Job? = null

    fun startLeaderboardListener() {
        leaderboardListenerJob?.cancel()
        leaderboardListenerJob = viewModelScope.launch {
            try {
                val limit = _leaderboardLimit.value
                firestoreRepository.db.collection("users")
                    .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("PointlyViewModel", "Leaderboard listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val userDocs = snapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(UserDocument::class.java)
                                } catch (e: Exception) {
                                    Log.e("PointlyViewModel", "Error deserializing user leaderboard doc", e)
                                    null
                                }
                            }
                            viewModelScope.launch {
                                repository.cacheLeaderboard(userDocs)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to start leaderboard listener", e)
            }
        }
    }

    fun loadMoreLeaderboard() {
        if (_leaderboardLimit.value < 200) {
            _leaderboardLimit.value += 30
            startLeaderboardListener()
        }
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            _isLeaderboardRefreshing.value = true
            try {
                val limit = _leaderboardLimit.value
                val snapshot = firestoreRepository.db.collection("users")
                    .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                val userDocs = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(UserDocument::class.java)
                    } catch (e: Exception) {
                        Log.e("PointlyViewModel", "Error deserializing user leaderboard doc on refresh", e)
                        null
                    }
                }
                repository.cacheLeaderboard(userDocs)
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Manual leaderboard refresh failed", e)
            } finally {
                _isLeaderboardRefreshing.value = false
            }
        }
    }

    fun setTab(tab: Int) {
        _currentTab.value = tab
    }

    fun setEditingProfile(editing: Boolean) {
        _isEditingProfile.value = editing
    }

    // --- gamified Systems ViewModel Methods ---
    fun claimDailyReward(day: Int) {
        viewModelScope.launch {
            repository.claimDailyReward(day)
        }
    }

    fun purchaseShopItem(itemId: String) {
        viewModelScope.launch {
            repository.purchaseShopItem(itemId)
        }
    }

    fun completeChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.progressChallenge(challengeId, 100) // complete it instantly/claim
        }
    }

    fun claimChallengeReward(challengeId: String) {
        viewModelScope.launch {
            repository.claimChallengeReward(challengeId)
        }
    }

    fun claimAchievementReward(achievementId: String) {
        viewModelScope.launch {
            repository.claimAchievementReward(achievementId)
        }
    }

    fun progressChallenge(challengeId: String, delta: Int) {
        viewModelScope.launch {
            repository.progressChallenge(challengeId, delta)
        }
    }

    fun progressAchievement(id: String, delta: Int) {
        viewModelScope.launch {
            repository.progressAchievement(id, delta)
        }
    }

    fun earnCoins(amount: Int) {
        viewModelScope.launch {
            repository.earnCoins(amount)
        }
    }

    fun earnGems(amount: Int) {
        viewModelScope.launch {
            repository.earnGems(amount)
        }
    }

    fun earnXp(amount: Int) {
        viewModelScope.launch {
            repository.earnXp(amount)
        }
    }

    fun updateProfileName(newName: String, newTitle: String) {
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            repository.updateProfile(current.copy(name = newName, title = newTitle))
        }
    }

    fun updateFullProfile(
        name: String,
        username: String,
        bio: String,
        school: String,
        board: String,
        className: String,
        section: String,
        privacySetting: String,
        visibleAchievements: Boolean,
        visiblePortfolio: Boolean,
        visibleShowcase: Boolean,
        visibleStatistics: Boolean
    ) {
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            repository.updateProfile(
                current.copy(
                    name = name,
                    username = username,
                    bio = bio,
                    school = school,
                    board = board,
                    className = className,
                    section = section,
                    privacySetting = privacySetting,
                    visibleAchievements = visibleAchievements,
                    visiblePortfolio = visiblePortfolio,
                    visibleShowcase = visibleShowcase,
                    visibleStatistics = visibleStatistics,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateAcademicSkills(
        math: Float,
        science: Float,
        english: Float,
        coding: Float,
        comm: Float,
        creativity: Float,
        leadership: Float,
        problemSolving: Float
    ) {
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            repository.updateProfile(
                current.copy(
                    mathSkill = math,
                    scienceSkill = science,
                    englishSkill = english,
                    codingSkill = coding,
                    commSkill = comm,
                    creativitySkill = creativity,
                    leadershipSkill = leadership,
                    problemSolvingSkill = problemSolving,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleFollowUser(usernameToToggle: String, isFollowingCurrently: Boolean) {
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            val diffFollowers = if (isFollowingCurrently) -1 else 1
            val diffFollowing = if (isFollowingCurrently) -1 else 1
            repository.updateProfile(
                current.copy(
                    followersCount = (current.followersCount + diffFollowers).coerceAtLeast(0),
                    followingCount = (current.followingCount + diffFollowing).coerceAtLeast(0),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Toggle Study Session & Stopwatch
    fun startStudySession(subject: String, topic: String) {
        _activeStudySubject.value = subject
        _activeStudyTopic.value = topic
        _isStudySessionActive.value = true
        _studyTimerSeconds.value = 0
        _totalSessionXpEarned.value = 0
        _currentQuiz.value = null

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _studyTimerSeconds.value += 1
            }
        }
    }

    fun stopStudySession() {
        timerJob?.cancel()
        timerJob = null
        _isStudySessionActive.value = false

        // Update profile with study hours completed (1 sec = 1/3600 hour, let's amplify for UI satisfaction)
        viewModelScope.launch {
            val current = profileState.value ?: return@launch
            val extraHours = _studyTimerSeconds.value / 60.0f // Amplify: 1 min = 1 study hour for rewarding pacing!
            val updatedHours = (current.weeklyStudyHours + extraHours).coerceAtMost(24.0f)
            repository.updateProfile(current.copy(weeklyStudyHours = updatedHours))

            // Unlocks 'first_quiz' and potentially 'mission_master' if study completed
            if (_totalSessionXpEarned.value > 0) {
                repository.unlockAchievement("first_quiz")
            }
        }
    }

    // Syllabus Engine Controller Methods
    fun syncSyllabus(className: String) {
        viewModelScope.launch {
            repository.syncSyllabusQuestions(className)
        }
    }

    fun setSelectedSyllabusClass(className: String) {
        _selectedSyllabusClass.value = className
        syncSyllabus(className)
    }

    fun setSelectedSyllabusSubject(subject: String?) {
        _selectedSyllabusSubject.value = subject
    }

    fun setSelectedSyllabusChapter(chapter: String?) {
        _selectedSyllabusChapter.value = chapter
    }

    fun startSyllabusQuiz(mode: QuizMode, subject: String? = null, chapter: String? = null) {
        viewModelScope.launch {
            val allEntities = syllabusQuestionsState.value
            val attempts = syllabusAttemptsState.value
            val className = _selectedSyllabusClass.value

            val allQuestions = allEntities.map { SyllabusQuestion.fromEntity(it) }

            val selectedQuestions = SyllabusEngine.selectSyllabusQuiz(
                allQuestions = allQuestions,
                attempts = attempts,
                className = className,
                mode = mode,
                subjectFilter = subject,
                chapterFilter = chapter
            )

            if (selectedQuestions.isEmpty()) {
                val seeded = SyllabusEngine.getSeededQuestions()
                    .filter { it.`class`.equals(className, ignoreCase = true) }
                _activeSyllabusQuiz.value = seeded.take(5)
            } else {
                _activeSyllabusQuiz.value = selectedQuestions
            }

            _activeSyllabusQuizIndex.value = 0
            _activeSyllabusSelectedOption.value = null
            _activeSyllabusIsChecked.value = false
            _activeSyllabusIsCorrect.value = false
            _activeSyllabusScore.value = 0
            _activeSyllabusXpEarned.value = 0
            _activeSyllabusPointsEarned.value = 0
            _activeSyllabusMode.value = mode
            _activeSyllabusTimeSpent.value = 0

            startSyllabusTimer()
        }
    }

    private fun startSyllabusTimer() {
        syllabusTimerJob?.cancel()
        syllabusTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _activeSyllabusTimeSpent.value += 1
            }
        }
    }

    private fun stopSyllabusTimer() {
        syllabusTimerJob?.cancel()
    }

    fun selectSyllabusOption(index: Int) {
        if (!_activeSyllabusIsChecked.value) {
            _activeSyllabusSelectedOption.value = index
        }
    }

    fun checkSyllabusAnswer() {
        val quiz = _activeSyllabusQuiz.value ?: return
        val question = quiz.getOrNull(_activeSyllabusQuizIndex.value) ?: return
        val selected = _activeSyllabusSelectedOption.value ?: return

        _activeSyllabusIsChecked.value = true
        val correct = selected == question.correctAnswerIndex
        _activeSyllabusIsCorrect.value = correct

        val timeSpent = _activeSyllabusTimeSpent.value
        stopSyllabusTimer()

        if (correct) {
            _activeSyllabusScore.value += 1
            _activeSyllabusXpEarned.value += question.xp
            _activeSyllabusPointsEarned.value += question.points
            _celebrationMessage.value = "+${question.xp} XP Earned!"
        } else {
            _celebrationMessage.value = "Incorrect! Review explanation."
        }

        viewModelScope.launch {
            // 1. Record Attempt locally in Room
            val attempt = SyllabusQuizAttemptEntity(
                questionId = question.id,
                subject = question.subject,
                chapter = question.chapter,
                topic = question.topic,
                isCorrect = correct,
                timeSpentSeconds = timeSpent
            )
            repository.insertAttempt(attempt)

            // 2. Increment wrong/correct metrics for the question offline
            if (!correct) {
                repository.updateWrongAttempts(question.id, 1)
            } else {
                repository.updateWrongAttempts(question.id, -999) // clear wrong count on correct solve
            }

            // 3. Award XP/Points dynamically to the user's Profile
            val profile = profileState.value
            if (profile != null && correct) {
                val newXp = profile.xp + question.xp
                val newLevel = if (newXp >= 2500) profile.level + 1 else profile.level
                val finalXp = if (newXp >= 2500) newXp - 2500 else newXp

                val updated = profile.copy(
                    xp = finalXp,
                    level = newLevel,
                    weeklyStudyHours = (profile.weeklyStudyHours + (timeSpent / 3600f)).coerceAtMost(24.0f)
                )
                repository.updateProfile(updated)

                // Update in Firestore for Leaderboard sync
                authRepository.currentUserState.value?.let { user ->
                    val userDoc = _currentUserDocument.value
                    if (userDoc != null) {
                        val updatedDoc = userDoc.copy(
                            xp = userDoc.xp + question.xp,
                            level = newLevel,
                            points = userDoc.points + question.points
                        )
                        firestoreRepository.saveDocument("users", user.uid, updatedDoc)
                    }
                }
            }
        }
    }

    fun nextSyllabusQuestion() {
        val quiz = _activeSyllabusQuiz.value ?: return
        val currentIndex = _activeSyllabusQuizIndex.value

        if (currentIndex < quiz.size - 1) {
            _activeSyllabusQuizIndex.value = currentIndex + 1
            _activeSyllabusSelectedOption.value = null
            _activeSyllabusIsChecked.value = false
            _activeSyllabusIsCorrect.value = false
            _activeSyllabusTimeSpent.value = 0
            _celebrationMessage.value = null
            startSyllabusTimer()
        } else {
            stopSyllabusTimer()
            concludeSyllabusQuiz()
        }
    }

    fun skipSyllabusQuestion() {
        val quiz = _activeSyllabusQuiz.value ?: return
        val currentIndex = _activeSyllabusQuizIndex.value
        val question = quiz.getOrNull(currentIndex) ?: return

        viewModelScope.launch {
            repository.incrementSkippedCount(question.id)
            
            val attempt = SyllabusQuizAttemptEntity(
                questionId = question.id,
                subject = question.subject,
                chapter = question.chapter,
                topic = question.topic,
                isCorrect = false,
                timeSpentSeconds = _activeSyllabusTimeSpent.value
            )
            repository.insertAttempt(attempt)
        }

        nextSyllabusQuestion()
    }

    fun toggleSyllabusBookmark(questionId: String, currentBookmarked: Boolean) {
        viewModelScope.launch {
            repository.updateBookmarkStatus(questionId, !currentBookmarked)
        }
    }

    private fun concludeSyllabusQuiz() {
        val mode = _activeSyllabusMode.value ?: QuizMode.DAILY_QUIZ
        val xpEarned = _activeSyllabusXpEarned.value
        val pointsEarned = _activeSyllabusPointsEarned.value
        val score = _activeSyllabusScore.value
        val total = _activeSyllabusQuiz.value?.size ?: 0

        _celebrationMessage.value = "Quiz Completed! Score: $score/$total. Earned +$xpEarned XP, +$pointsEarned Points!"

        viewModelScope.launch {
            val user = authRepository.currentUserState.value
            val userDoc = _currentUserDocument.value
            if (user != null && userDoc != null) {
                try {
                    communityRepository.createFeedItem(
                        uid = user.uid,
                        name = userDoc.name,
                        type = "achievement",
                        title = "${mode.name.replace("_", " ")} Conquered",
                        message = "conquered the ${mode.name.replace("_", " ")} challenge scoring $score/$total!"
                    )
                    communityRepository.createNotification(
                        recipientUid = user.uid,
                        title = "Challenge Completed!",
                        message = "You scored $score/$total in the ${mode.name.replace("_", " ")}!",
                        type = "system"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("PointlyViewModel", "Failed to post feed/notification", e)
                }
            }
        }
    }

    fun postSystemNotification(title: String, message: String, type: String) {
        viewModelScope.launch {
            try {
                repository.postToCommunityFeedAndNotify(title, message, type)
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to post feed", e)
            }
        }
    }

    fun quitSyllabusQuiz() {
        stopSyllabusTimer()
        _activeSyllabusQuiz.value = null
        _activeSyllabusMode.value = null
        _celebrationMessage.value = null
    }

    // Dynamic Gemini-powered Quizzes
    fun fetchGeminiQuizForCurrentTopic() {
        viewModelScope.launch {
            _isQuizLoading.value = true
            _currentQuiz.value = null
            _currentQuestionIndex.value = 0
            _selectedAnswerIndex.value = null
            _isAnswerChecked.value = false

            val response = repository.generateQuiz(_activeStudyTopic.value, _activeStudySubject.value)
            _currentQuiz.value = response
            _isQuizLoading.value = false

            // Mark 'gemini_partner' achievement as earned!
            repository.unlockAchievement("gemini_partner")
        }
    }

    // Answer Quiz Question
    fun selectAnswer(index: Int) {
        if (!_isAnswerChecked.value) {
            _selectedAnswerIndex.value = index
        }
    }

    fun checkAnswer() {
        val quiz = _currentQuiz.value ?: return
        val currentQuestion = quiz.questions.getOrNull(_currentQuestionIndex.value) ?: return
        val selected = _selectedAnswerIndex.value ?: return

        _isAnswerChecked.value = true
        val correct = selected == currentQuestion.correctOption
        _isAnswerCorrect.value = correct

        if (correct) {
            _totalSessionXpEarned.value += 50
            _celebrationMessage.value = "+50 XP Earned!"
            viewModelScope.launch {
                repository.earnXp(50)
                // Trigger 'Academic Spark' badge
                repository.unlockAchievement("first_quiz")
            }
        } else {
            _celebrationMessage.value = "Incorrect. Read Explanation!"
        }
    }

    fun nextQuestion() {
        val quiz = _currentQuiz.value ?: return
        if (_currentQuestionIndex.value < quiz.questions.size - 1) {
            _currentQuestionIndex.value += 1
            _selectedAnswerIndex.value = null
            _isAnswerChecked.value = false
            _celebrationMessage.value = null
        } else {
            // Quiz completed! Award completion bonus XP!
            _totalSessionXpEarned.value += 100
            _celebrationMessage.value = "Quiz Completed! +100 XP Bonus!"
            viewModelScope.launch {
                repository.earnXp(100)
                // Complete the active fluid dynamics study mission if completed
                val missions = missionsState.value
                missions.find { it.title.contains(_activeStudySubject.value, ignoreCase = true) || it.subject.contains(_activeStudySubject.value, ignoreCase = true) }?.let { mission ->
                    repository.updateMission(mission.copy(completed = true))
                }
                
                // If all missions completed, unlock mission_master
                val updatedMissions = repository.getMissionsSync()
                if (updatedMissions.all { it.completed }) {
                    repository.unlockAchievement("mission_master")
                }
            }
            // Clear quiz to show summary
            _currentQuiz.value = null
        }
    }

    fun clearCelebrationMessage() {
        _celebrationMessage.value = null
    }

    // Increase community study hours
    fun contributeCommunityStudy() {
        _communityStudyHours.value += 2.5f
    }

    // --- COMPLETE GAMIFIED AUTHENTICATION ENGINE SYSTEM ---

    // --- Theme settings ---
    fun setAppTheme(theme: String) {
        appTheme.value = theme
        try {
            val sharedPref = getApplication<Application>().getSharedPreferences("pointly_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().putString("app_theme", theme).apply()
        } catch (e: Exception) {
            Log.e("PointlyViewModel", "Failed to save theme preference", e)
        }
    }

    // --- Remember User ID ---
    fun getRememberedUser(): String {
        return try {
            val sharedPref = getApplication<Application>().getSharedPreferences("pointly_prefs", Context.MODE_PRIVATE)
            sharedPref.getString("remembered_username", "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun saveRememberedUser(username: String) {
        try {
            val sharedPref = getApplication<Application>().getSharedPreferences("pointly_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().putString("remembered_username", username).apply()
        } catch (e: Exception) {
            Log.e("PointlyViewModel", "Failed to save remembered user", e)
        }
    }

    fun clearRememberedUser() {
        try {
            val sharedPref = getApplication<Application>().getSharedPreferences("pointly_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().remove("remembered_username").apply()
        } catch (e: Exception) {
            Log.e("PointlyViewModel", "Failed to clear remembered user", e)
        }
    }

    // --- Real-time Username Availability check ---
    fun checkUsernameAvailability(username: String) {
        val trimmed = username.trim()
        val regex = "^[a-z0-9_]{4,20}$".toRegex()
        if (trimmed.isEmpty()) {
            isUsernameAvailable.value = null
            return
        }
        if (!trimmed.matches(regex)) {
            isUsernameAvailable.value = false
            return
        }
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            isCheckingUsername.value = true
            delay(400) // debounce
            try {
                val querySnapshot = firestoreRepository.db.collection("users")
                    .whereEqualTo("username", trimmed)
                    .get()
                    .await()
                isUsernameAvailable.value = querySnapshot.isEmpty
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Error checking username availability", e)
                isUsernameAvailable.value = null
            } finally {
                isCheckingUsername.value = false
            }
        }
    }

    // --- User ID Authentication ---
    fun loginWithUserId(username: String, password: String, rememberMe: Boolean = false, isTeacherRequest: Boolean = false, isAdminRequest: Boolean = false) {
        val trimmedUsername = username.trim()
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            try {
                // 1. Query Firestore users collection by username field
                val querySnapshot = firestoreRepository.db.collection("users")
                    .whereEqualTo("username", trimmedUsername)
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    _authUiState.value = AuthUiState.Error("User ID / Username not found.")
                    return@launch
                }

                val doc = querySnapshot.documents.first()
                val email = doc.getString("email")
                val name = doc.getString("name") ?: "User"
                val isTeacherInDoc = doc.getBoolean("isTeacher") ?: false
                val isAdminInDoc = doc.getBoolean("isAdmin") ?: false

                if (isAdminRequest && !isAdminInDoc) {
                    _authUiState.value = AuthUiState.Error("Access Denied. This account is not a registered Admin.")
                    return@launch
                }
                if (isTeacherRequest && !isTeacherInDoc) {
                    _authUiState.value = AuthUiState.Error("Access Denied. This account is not a registered Teacher.")
                    return@launch
                }
                if (!isTeacherRequest && !isAdminRequest && (isTeacherInDoc || isAdminInDoc)) {
                    val role = if (isAdminInDoc) "Admin" else "Teacher"
                    _authUiState.value = AuthUiState.Error("Access Denied. Please switch to the $role tab to login.")
                    return@launch
                }
                
                if (email.isNullOrEmpty()) {
                    _authUiState.value = AuthUiState.Error("No email associated with this User ID.")
                    return@launch
                }

                // 2. Perform Firebase Login using email
                val result = authRepository.loginWithEmail(email, password)
                result.onSuccess { user ->
                    _isEmailVerified.value = true
                    
                    // Handle Remember Me preference storage
                    if (rememberMe) {
                        saveRememberedUser(trimmedUsername)
                    } else {
                        clearRememberedUser()
                    }

                    _authUiState.value = AuthUiState.Success("Welcome back, $name!")
                }.onFailure { error ->
                    _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Login failed. Please check password.")
                }
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Login lookup error", e)
                _authUiState.value = AuthUiState.Error(e.localizedMessage ?: "User ID lookup failed. Please try again.")
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.loginWithEmail(email, password)
            result.onSuccess { user ->
                _isEmailVerified.value = true
                _authUiState.value = AuthUiState.Success("Welcome back, ${user.email}!")
            }.onFailure { error ->
                _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Login failed. Please check credentials.")
            }
        }
    }

    fun signUpWithUserId(
        username: String,
        password: String,
        fullName: String,
        className: String,
        section: String,
        school: String = "",
        profileImage: String? = null,
        isTeacher: Boolean = false,
        employeeId: String = "",
        subjects: String = "",
        classesAssigned: String = "",
        sectionsAssigned: String = "",
        isAdmin: Boolean = false,
        adminId: String = "",
        organizationId: String = "",
        permissions: String = "",
        adminRole: String = ""
    ) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.signUpWithUserId(
                username = username,
                password = password,
                fullName = fullName,
                className = className,
                section = section,
                school = school,
                profileImage = profileImage,
                isTeacher = isTeacher,
                employeeId = employeeId,
                subjects = subjects,
                classesAssigned = classesAssigned,
                sectionsAssigned = sectionsAssigned,
                isAdmin = isAdmin,
                adminId = adminId,
                organizationId = organizationId,
                permissions = permissions,
                adminRole = adminRole
            )
            result.onSuccess { user ->
                _isEmailVerified.value = true
                _authUiState.value = AuthUiState.Success("Welcome to your sanctuary, $fullName!")
            }.onFailure { error ->
                _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Sign up failed. Please try again.")
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        name: String,
        username: String,
        className: String,
        section: String
    ) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.signUpWithEmail(email, password, name, username, className, section)
            result.onSuccess { user ->
                _isEmailVerified.value = true
                _authUiState.value = AuthUiState.Success("Registration successful! Verification email sent.")
            }.onFailure { error ->
                _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Sign up failed. Please try again.")
            }
        }
    }

    fun resetPasswordByUsername(username: String, school: String? = null) {
        val trimmed = username.trim()
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            try {
                // 1. Query Firestore users collection by username field
                var query = firestoreRepository.db.collection("users")
                    .whereEqualTo("username", trimmed)
                
                if (!school.isNullOrEmpty()) {
                    query = query.whereEqualTo("school", school)
                }
                
                val querySnapshot = query.get().await()

                if (querySnapshot.isEmpty) {
                    _authUiState.value = AuthUiState.Error("User ID / Username not found at this school.")
                    return@launch
                }

                val doc = querySnapshot.documents.first()
                val email = doc.getString("email") ?: "${trimmed.lowercase()}@pointly77.app"

                // 2. Perform Firebase Password Reset using email
                val result = authRepository.resetPassword(email)
                result.onSuccess {
                    _authUiState.value = AuthUiState.Success("Password reset link has been sent.")
                }.onFailure { error ->
                    _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Failed to send reset link.")
                }
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Reset password lookup error", e)
                _authUiState.value = AuthUiState.Error("Failed to look up User ID. If no recovery method exists, please contact your admin.")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.resetPassword(email)
            result.onSuccess {
                _authUiState.value = AuthUiState.Success("Password reset instructions have been sent to $email.")
            }.onFailure { error ->
                _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Failed to send reset email.")
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.sendEmailVerification()
            result.onSuccess {
                _authUiState.value = AuthUiState.Success("A fresh verification email has been sent!")
            }.onFailure { error ->
                _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Failed to send verification email.")
            }
        }
    }

    fun checkEmailVerificationStatus() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.reloadUserAndCheckVerification()
            result.onSuccess { verified ->
                _isEmailVerified.value = verified
                if (verified) {
                    _authUiState.value = AuthUiState.Success("Email verified successfully! Welcome to Pointly!")
                } else {
                    _authUiState.value = AuthUiState.Error("Email is still not verified. Please check your inbox.")
                }
            }.onFailure { error ->
                _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Verification check failed.")
            }
        }
    }

    fun showLocalNotification(title: String, message: String) {
        val context = getApplication<Application>().applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "pointly_focus",
                "Pointly Focus Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pomodoro and focus timer updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = NotificationCompat.Builder(context, "pointly_focus")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun updatePomodoroCategory(activityType: String) {
        viewModelScope.launch {
            val currentState = repository.getPomodoroState() ?: PomodoroStateEntity()
            val newState = currentState.copy(activityType = activityType)
            repository.savePomodoroState(newState)
        }
    }

    fun startPomodoro(durationMinutes: Int, isBreak: Boolean = false, activityType: String = "Study") {
        viewModelScope.launch {
            val durationSeconds = durationMinutes * 60
            val newState = PomodoroStateEntity(
                id = 1,
                durationSeconds = durationSeconds,
                remainingSeconds = durationSeconds,
                isRunning = true,
                isBreak = isBreak,
                activityType = activityType,
                lastTickTime = System.currentTimeMillis(),
                originalDurationSeconds = durationSeconds,
                skipBreak = false
            )
            repository.savePomodoroState(newState)
            startPomodoroTimerJob()
            if (isBreak) {
                showLocalNotification("Break Started", "Take a well-deserved break for $durationMinutes minutes!")
            } else {
                showLocalNotification("Focus Started", "Time to focus on $activityType for $durationMinutes minutes!")
            }
        }
    }

    fun pausePomodoro() {
        viewModelScope.launch {
            val currentState = repository.getPomodoroState() ?: return@launch
            val newState = currentState.copy(isRunning = false)
            repository.savePomodoroState(newState)
            pomodoroJob?.cancel()
            showLocalNotification("Timer Paused", "Your focus session is paused.")
        }
    }

    fun resumePomodoro() {
        viewModelScope.launch {
            val currentState = repository.getPomodoroState() ?: return@launch
            val newState = currentState.copy(isRunning = true, lastTickTime = System.currentTimeMillis())
            repository.savePomodoroState(newState)
            startPomodoroTimerJob()
            showLocalNotification("Timer Resumed", "Back in the zone!")
        }
    }

    fun skipBreak() {
        viewModelScope.launch {
            val currentState = repository.getPomodoroState() ?: return@launch
            val newState = currentState.copy(
                isBreak = false,
                remainingSeconds = currentState.originalDurationSeconds,
                durationSeconds = currentState.originalDurationSeconds,
                isRunning = false
            )
            repository.savePomodoroState(newState)
            pomodoroJob?.cancel()
            showLocalNotification("Break Skipped", "Ready to focus again!")
        }
    }

    fun stopPomodoro() {
        viewModelScope.launch {
            val currentState = repository.getPomodoroState() ?: return@launch
            val newState = currentState.copy(
                isRunning = false,
                remainingSeconds = currentState.originalDurationSeconds
            )
            repository.savePomodoroState(newState)
            pomodoroJob?.cancel()
            showLocalNotification("Timer Stopped", "Focus session stopped.")
        }
    }

    fun resetPomodoro() {
        viewModelScope.launch {
            val currentState = repository.getPomodoroState() ?: return@launch
            val newState = currentState.copy(
                isRunning = false,
                remainingSeconds = currentState.originalDurationSeconds
            )
            repository.savePomodoroState(newState)
            pomodoroJob?.cancel()
        }
    }

    fun checkAndRecoverPomodoro() {
        viewModelScope.launch {
            val state = repository.getPomodoroState()
            if (state == null) {
                // Seed initial state
                repository.savePomodoroState(PomodoroStateEntity())
            } else if (state.isRunning) {
                val now = System.currentTimeMillis()
                val elapsedSeconds = ((now - state.lastTickTime) / 1000).toInt()
                if (elapsedSeconds > 0) {
                    val newRemaining = (state.remainingSeconds - elapsedSeconds).coerceAtLeast(0)
                    val newState = state.copy(
                        remainingSeconds = newRemaining,
                        lastTickTime = now
                    )
                    repository.savePomodoroState(newState)
                    if (newRemaining == 0) {
                        handlePomodoroCompletion(newState)
                    } else {
                        startPomodoroTimerJob()
                    }
                } else {
                    startPomodoroTimerJob()
                }
            }
        }
    }

    private fun startPomodoroTimerJob() {
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentState = repository.getPomodoroState() ?: break
                if (!currentState.isRunning) break
                
                val now = System.currentTimeMillis()
                val newRemaining = (currentState.remainingSeconds - 1).coerceAtLeast(0)
                val newState = currentState.copy(
                    remainingSeconds = newRemaining,
                    lastTickTime = now
                )
                repository.savePomodoroState(newState)
                
                if (newRemaining == 0) {
                    handlePomodoroCompletion(newState)
                    break
                }
            }
        }
    }

    private fun handlePomodoroCompletion(state: PomodoroStateEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!state.isBreak) {
                // Focus complete! Create completed activity
                val durationSec = state.originalDurationSeconds
                val xpAmount = (durationSec / 300) * 10 // 10 XP per 5 min Focus
                val pointsAmount = (durationSec / 300) * 10 // 10 Points per 5 min Focus
                
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val newActivity = ActivityEntity(
                    activityId = UUID.randomUUID().toString(),
                    uid = user?.uid ?: "",
                    title = "Pomodoro ${state.activityType}",
                    type = state.activityType,
                    duration = durationSec,
                    xpEarned = xpAmount,
                    pointsEarned = pointsAmount,
                    startTime = now - durationSec * 1000L,
                    endTime = now,
                    completed = true,
                    createdAt = now,
                    updatedAt = now
                )
                
                repository.insertActivity(newActivity)
                repository.earnRewards(xpAmount, pointsAmount)
                
                showLocalNotification("Focus Session Completed", "Fantastic work! You earned $xpAmount XP & $pointsAmount Points!")
                
                // Unlock first activity achievement
                repository.unlockAchievement("first_quiz")
                
                // Set break state
                val breakMinutes = if (state.originalDurationSeconds >= 50 * 60) 10 else 5
                val breakSeconds = breakMinutes * 60
                val breakState = state.copy(
                    isBreak = true,
                    durationSeconds = breakSeconds,
                    remainingSeconds = breakSeconds,
                    isRunning = true,
                    lastTickTime = System.currentTimeMillis()
                )
                repository.savePomodoroState(breakState)
                startPomodoroTimerJob()
            } else {
                // Break complete!
                showLocalNotification("Break Finished", "Ready to focus again? Let's start a new session!")
                val focusState = state.copy(
                    isBreak = false,
                    durationSeconds = state.originalDurationSeconds,
                    remainingSeconds = state.originalDurationSeconds,
                    isRunning = false,
                    lastTickTime = 0L
                )
                repository.savePomodoroState(focusState)
            }
        }
    }

    // Direct Activities control (e.g. log direct activities / delete / edit)
    fun logActivityDirectly(title: String, type: String, durationMinutes: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val durationSec = durationMinutes * 60
            val xpAmount = durationMinutes * 2
            val pointsAmount = durationMinutes * 2
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            
            val newActivity = ActivityEntity(
                activityId = UUID.randomUUID().toString(),
                uid = user?.uid ?: "",
                title = title,
                type = type,
                duration = durationSec,
                xpEarned = xpAmount,
                pointsEarned = pointsAmount,
                startTime = now - durationSec * 1000L,
                endTime = now,
                completed = true,
                createdAt = now,
                updatedAt = now
            )
            repository.insertActivity(newActivity)
            repository.earnRewards(xpAmount, pointsAmount)
            showLocalNotification("Activity Logged", "You successfully logged $title!")
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
        }
    }

    fun updateActivityCustom(activity: ActivityEntity) {
        viewModelScope.launch {
            repository.updateActivity(activity)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.logout()
            result.onSuccess {
                _isEmailVerified.value = false
                _authUiState.value = AuthUiState.Idle
            }.onFailure { error ->
                _authUiState.value = AuthUiState.Error(error.localizedMessage ?: "Failed to log out.")
            }
        }
    }

    fun clearAuthUiState() {
        _authUiState.value = AuthUiState.Idle
    }

    // ==========================================
    // COMMUNITY MODULE INTEGRATION
    // ==========================================

    private val _currentChannelId = MutableStateFlow("school")
    val currentChannelId: StateFlow<String> = _currentChannelId.asStateFlow()

    val channelMessages: StateFlow<List<com.example.data.database.RoomMessageEntity>> = _currentChannelId.flatMapLatest { channelId ->
        viewModelScope.launch {
            try {
                communityRepository.listenFirestoreMessages(channelId).collect { messages ->
                    communityRepository.saveCachedMessages(messages)
                }
            } catch (e: Exception) {
                Log.w("PointlyViewModel", "Failed to listen to messages for $channelId: ${e.message}")
            }
        }
        communityRepository.getMessagesFlow(channelId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userPresences: StateFlow<Map<String, com.example.data.repository.UserPresence>> = _currentChannelId.flatMapLatest { channelId ->
        communityRepository.listenPresence(channelId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val showcasePosts: StateFlow<List<com.example.data.database.RoomShowcasePostEntity>> = callbackFlow {
        val job = viewModelScope.launch {
            try {
                communityRepository.listenFirestoreShowcase().collect { posts ->
                    communityRepository.saveCachedShowcase(posts)
                }
            } catch (e: Exception) {
                Log.w("PointlyViewModel", "Failed to listen to showcase: ${e.message}")
            }
        }
        communityRepository.getShowcasePostsFlow().collect {
            trySend(it)
        }
        awaitClose { job.cancel() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val communityFeedItems: StateFlow<List<com.example.data.database.RoomFeedItemEntity>> = callbackFlow {
        val job = viewModelScope.launch {
            try {
                communityRepository.listenFirestoreFeed().collect { items ->
                    communityRepository.saveCachedFeed(items)
                }
            } catch (e: Exception) {
                Log.w("PointlyViewModel", "Failed to listen to feed: ${e.message}")
            }
        }
        communityRepository.getFeedFlow().collect {
            trySend(it)
        }
        awaitClose { job.cancel() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val studySquads: StateFlow<List<com.example.data.database.RoomSquadEntity>> = callbackFlow {
        val job = viewModelScope.launch {
            try {
                communityRepository.listenFirestoreSquads().collect { squads ->
                    communityRepository.saveCachedSquads(squads)
                }
            } catch (e: Exception) {
                Log.w("PointlyViewModel", "Failed to listen to squads: ${e.message}")
            }
        }
        communityRepository.getSquadsFlow().collect {
            trySend(it)
        }
        awaitClose { job.cancel() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setChannelId(channelId: String) {
        _currentChannelId.value = channelId
    }

    fun updateTypingStatus(isTyping: Boolean) {
        val user = currentUser.value ?: return
        val userDoc = _currentUserDocument.value ?: return
        viewModelScope.launch {
            try {
                communityRepository.updatePresence(
                    userUid = user.uid,
                    name = userDoc.name,
                    isOnline = true,
                    typingChannelId = if (isTyping) _currentChannelId.value else ""
                )
            } catch (e: Exception) {
                Log.w("PointlyViewModel", "Failed to update presence status: ${e.message}")
            }
        }
    }

    fun sendChatMessage(text: String, replyToId: String? = null, replyToText: String? = null) {
        val user = currentUser.value ?: return
        val userDoc = _currentUserDocument.value ?: return
        val channel = _currentChannelId.value
        viewModelScope.launch {
            try {
                communityRepository.sendMessage(
                    channelId = channel,
                    senderUid = user.uid,
                    senderName = userDoc.name,
                    text = text,
                    replyToId = replyToId,
                    replyToText = replyToText
                )
                if (text.contains("@")) {
                    val mentionText = text.substringAfter("@").substringBefore(" ")
                    val targetUser = leaderboardEntries.value.find { it.username.equals(mentionText, ignoreCase = true) }
                    if (targetUser != null) {
                        communityRepository.createNotification(
                            recipientUid = targetUser.uid,
                            title = "Mentioned in Chat",
                            message = "${userDoc.name} mentioned you in #${channel}: \"$text\"",
                            type = "MENTION"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to send chat message", e)
            }
        }
    }

    fun editChatMessage(messageId: String, newText: String) {
        val channel = _currentChannelId.value
        viewModelScope.launch {
            try {
                communityRepository.editMessage(channel, messageId, newText)
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to edit chat message", e)
            }
        }
    }

    fun deleteChatMessage(messageId: String) {
        val channel = _currentChannelId.value
        viewModelScope.launch {
            try {
                communityRepository.deleteMessage(channel, messageId)
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to delete chat message", e)
            }
        }
    }

    fun addChatReaction(messageId: String, emoji: String) {
        val user = currentUser.value ?: return
        val channel = _currentChannelId.value
        viewModelScope.launch {
            try {
                communityRepository.addReaction(channel, messageId, user.uid, emoji)
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to add reaction", e)
            }
        }
    }

    fun markMessageRead(messageId: String) {
        val user = currentUser.value ?: return
        val channel = _currentChannelId.value
        viewModelScope.launch {
            try {
                communityRepository.markMessageAsRead(channel, messageId, user.uid)
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to mark message read", e)
            }
        }
    }

    // SHOWCASE ACTIONS
    fun createShowcasePost(title: String, description: String, category: String, uri: android.net.Uri?, mockFileName: String) {
        val user = currentUser.value ?: return
        val userDoc = _currentUserDocument.value ?: return
        viewModelScope.launch {
            val postId = java.util.UUID.randomUUID().toString()
            val initialFileUrl = uri?.toString() ?: "https://firebasestorage.googleapis.com/v0/b/pointly-77/o/showcase%2Fmock.png?alt=media"
            
            val localPost = com.example.data.database.RoomShowcasePostEntity(
                postId = postId,
                authorUid = user.uid,
                authorName = userDoc.name,
                authorUsername = userDoc.username,
                title = title,
                description = description,
                category = category,
                fileUrl = initialFileUrl,
                timestamp = System.currentTimeMillis()
            )
            
            try {
                // 1. Store locally in Room first (Primary Database)
                communityRepository.saveCachedShowcase(listOf(localPost))
                Log.d("PointlyViewModel", "Saved showcase post locally first: $postId")
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to save showcase post locally", e)
            }

            // 2. Asynchronously upload to Firebase Storage & Sync with Firestore in background
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val finalFileUrl = if (uri != null) {
                        try {
                            communityRepository.uploadShowcaseFile(uri, "${System.currentTimeMillis()}_$mockFileName")
                        } catch (e: Exception) {
                            Log.e("PointlyViewModel", "Failed to upload file to cloud storage. Using local/fallback URI.", e)
                            initialFileUrl
                        }
                    } else {
                        initialFileUrl
                    }

                    communityRepository.createShowcasePost(
                        authorUid = user.uid,
                        authorName = userDoc.name,
                        authorUsername = userDoc.username,
                        title = title,
                        description = description,
                        category = category,
                        fileUrl = finalFileUrl,
                        postId = postId
                    )

                    // Update local cached post with the remote URL if changed
                    if (finalFileUrl != initialFileUrl) {
                        val updatedPost = localPost.copy(fileUrl = finalFileUrl)
                        communityRepository.saveCachedShowcase(listOf(updatedPost))
                    }
                    Log.d("PointlyViewModel", "Successfully synced showcase post to Firestore: $postId")
                } catch (e: Exception) {
                    Log.e("PointlyViewModel", "Failed to sync showcase post to cloud in background (running offline mode)", e)
                }
            }
        }
    }

    fun toggleLikePost(postId: String, postAuthorUid: String) {
        val user = currentUser.value ?: return
        val userDoc = _currentUserDocument.value ?: return
        viewModelScope.launch {
            try {
                communityRepository.toggleLikePost(postId, user.uid)
                if (user.uid != postAuthorUid) {
                    communityRepository.createNotification(
                        recipientUid = postAuthorUid,
                        title = "New Like",
                        message = "${userDoc.name} liked your showcase post!",
                        type = "LIKE"
                    )
                }
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to like post", e)
            }
        }
    }

    fun toggleSavePost(postId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                communityRepository.toggleSavePost(postId, user.uid)
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to save post", e)
            }
        }
    }

    fun reportPost(postId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                communityRepository.reportPost(postId, user.uid)
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to report post", e)
            }
        }
    }

    // COMMENTS ACTIONS
    fun getPostCommentsFlow(postId: String): Flow<List<com.example.data.repository.PostComment>> {
        return communityRepository.listenPostComments(postId)
    }

    fun addPostComment(postId: String, postAuthorUid: String, commentText: String) {
        val user = currentUser.value ?: return
        val userDoc = _currentUserDocument.value ?: return
        viewModelScope.launch {
            try {
                communityRepository.addComment(postId, user.uid, userDoc.name, commentText)
                if (user.uid != postAuthorUid) {
                    communityRepository.createNotification(
                        recipientUid = postAuthorUid,
                        title = "New Comment",
                        message = "${userDoc.name} commented on your showcase post: \"$commentText\"",
                        type = "COMMENT"
                    )
                }
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to add comment", e)
            }
        }
    }

    // SQUADS ACTIONS
    fun createStudySquad(name: String, description: String, onInviteCodeReady: (String) -> Unit) {
        val user = currentUser.value ?: return
        val userDoc = _currentUserDocument.value ?: return
        viewModelScope.launch {
            try {
                val code = communityRepository.createSquad(name, description, user.uid)
                onInviteCodeReady(code)
                // Add to community feed
                communityRepository.createFeedItem(
                    uid = user.uid,
                    name = userDoc.name,
                    type = "SQUAD",
                    title = "New Squad Created",
                    message = "${userDoc.name} created a new study squad: \"$name\"! Join with code: $code"
                )
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to create squad", e)
            }
        }
    }

    fun joinStudySquad(inviteCode: String, onResult: (Boolean) -> Unit) {
        val user = currentUser.value ?: return
        val userDoc = _currentUserDocument.value ?: return
        viewModelScope.launch {
            try {
                val success = communityRepository.joinSquad(inviteCode, user.uid)
                onResult(success)
                if (success) {
                    // Get the squad to find the name
                    val squads = studySquads.value
                    val squad = squads.find { it.inviteCode == inviteCode }
                    squad?.let {
                        communityRepository.createNotification(
                            recipientUid = it.creatorUid,
                            title = "Squad Member Joined",
                            message = "${userDoc.name} joined your study squad: \"${it.name}\"!",
                            type = "SQUAD_JOIN"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PointlyViewModel", "Failed to join squad", e)
                onResult(false)
            }
        }
    }

    // ==========================================
    // AI STUDY COMPANION FIELDS & METHODS
    // ==========================================
    val isAiLoading = MutableStateFlow(false)
    val aiRecommendations = MutableStateFlow("")
    val aiStudyPlan = MutableStateFlow("")
    val aiRevisionMaterial = MutableStateFlow("")
    val aiChatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val aiPlannerMode = MutableStateFlow("Daily")
    val aiRevisionType = MutableStateFlow("Flashcards")
    val aiSelectedSubject = MutableStateFlow("Physics")

    fun getStudentContextString(): String {
        val p = profileState.value
        val u = currentUserDocument.value
        val anal = syllabusAnalyticsState.value
        val challenges = challengesState.value
        val achievements = achievementsState.value
        
        return """
            Student Name: ${p?.name ?: u?.name ?: "John Doe"}
            Class: ${u?.className ?: "Class 8"}
            Section: ${u?.section ?: "A"}
            Board: CBSE
            Current Level: ${p?.level ?: 14}
            Current XP: ${p?.xp ?: 2100}
            Streak: ${p?.streak ?: 77} days
            Weak Chapters: ${anal.chapterAnalytics.filter { it.accuracy < 0.6f }.map { it.chapter }.joinToString()}
            Strong Chapters: ${anal.chapterAnalytics.filter { it.accuracy >= 0.8f }.map { it.chapter }.joinToString()}
            Weak Topics: ${anal.weakTopics.joinToString()}
            Strong Topics: ${anal.strongTopics.joinToString()}
            Worst Subject: ${anal.worstSubject.ifEmpty { "Physics" }}
            Best Subject: ${anal.bestSubject.ifEmpty { "Mathematics" }}
            Total Questions Solved: ${anal.totalQuestionsSolved}
            Overall Accuracy: ${(anal.overallAccuracy * 100).toInt()}%
            Achievements: ${achievements.filter { it.completed }.map { it.title }.joinToString()}
            Active Challenges: ${challenges.filter { !it.completed }.map { it.title }.joinToString()}
        """.trimIndent()
    }

    private suspend fun callGeminiDirect(prompt: String, fallback: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val key = try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            return@withContext fallback
        }
        try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(temperature = 0.7f)
            )
            val response = RetrofitClient.service.generateContent(key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: fallback
        } catch (e: Exception) {
            Log.e("PointlyViewModel", "Gemini API error during companion generation", e)
            fallback
        }
    }

    suspend fun generateTeacherContent(prompt: String, fallback: String): String {
        return callGeminiDirect(prompt, fallback)
    }

    fun fetchAiRecommendations(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val cacheKey = "ai_rec_${currentUserDocument.value?.uid ?: "guest"}"
            if (!forceRefresh) {
                val cached = repository.getAiCache(cacheKey)
                if (cached != null) {
                    aiRecommendations.value = cached
                    return@launch
                }
            }
            
            isAiLoading.value = true
            val context = getStudentContextString()
            val p = profileState.value
            val prompt = """
                You are Pointly 77's custom AI Study Companion.
                Analyze this student's context and provide 3 highly personalized, motivating daily recommendations (actions to take), identifying weak areas, streak retention tips, and what to study next.
                
                $context
                
                Provide your answer in a clear, scannable Markdown format with sections:
                ### 🎯 Today's Recommendations
                ### 📈 Weak Subjects & Action Plan
                ### 🔥 Streak Motivation (${p?.streak ?: 77} Days!)
            """.trimIndent()
            
            val result = callGeminiDirect(prompt, getRecommendationsFallback())
            aiRecommendations.value = result
            repository.insertAiCache(cacheKey, result)
            isAiLoading.value = false
        }
    }

    fun generateAiPlan(mode: String, forceRefresh: Boolean = false) {
        aiPlannerMode.value = mode
        viewModelScope.launch {
            val cacheKey = "ai_plan_${mode}_${currentUserDocument.value?.uid ?: "guest"}"
            if (!forceRefresh) {
                val cached = repository.getAiCache(cacheKey)
                if (cached != null) {
                    aiStudyPlan.value = cached
                    return@launch
                }
            }
            
            isAiLoading.value = true
            val context = getStudentContextString()
            val prompt = """
                You are Pointly 77's custom AI Study Companion.
                Generate a structured study plan (${mode}) tailored for this student:
                
                $context
                
                The plan must be realistic, adaptive (mentioning how to adjust if they missed a day), and break down specific chapters.
                Provide in Markdown format with rich emojis.
            """.trimIndent()
            
            val result = callGeminiDirect(prompt, getPlannerFallback(mode))
            aiStudyPlan.value = result
            repository.insertAiCache(cacheKey, result)
            isAiLoading.value = false
        }
    }

    fun generateAiRevision(type: String, subject: String, forceRefresh: Boolean = false) {
        aiRevisionType.value = type
        aiSelectedSubject.value = subject
        viewModelScope.launch {
            val cacheKey = "ai_rev_${type}_${subject}_${currentUserDocument.value?.uid ?: "guest"}"
            if (!forceRefresh) {
                val cached = repository.getAiCache(cacheKey)
                if (cached != null) {
                    aiRevisionMaterial.value = cached
                    return@launch
                }
            }
            
            isAiLoading.value = true
            val prompt = """
                You are Pointly 77's custom AI Study Companion.
                Generate premium quality academic revision material of type: "${type}" for subject: "${subject}" tailored to Class: ${currentUserDocument.value?.className ?: "Class 8"} (CBSE).
                
                Include Flashcards, Definitions, or Mind Map elements matching the requested type.
                Provide in clean, structured Markdown format.
            """.trimIndent()
            
            val result = callGeminiDirect(prompt, getRevisionFallback(type, subject))
            aiRevisionMaterial.value = result
            repository.insertAiCache(cacheKey, result)
            isAiLoading.value = false
        }
    }

    fun askAiQuery(query: String) {
        if (query.trim().isEmpty()) return
        val currentHistory = aiChatHistory.value.toMutableList()
        currentHistory.add(Pair(query, true))
        aiChatHistory.value = currentHistory
        
        viewModelScope.launch {
            isAiLoading.value = true
            val prompt = """
                You are Pointly 77's custom AI Study Companion.
                The student is asking a doubt or seeking concept explanation:
                
                "Can you explain: $query"
                
                Provide your response considering their current Class: ${currentUserDocument.value?.className ?: "Class 8"} (CBSE).
                Break down your answer step-by-step, explain with examples, and keep it encouraging.
            """.trimIndent()
            
            val result = callGeminiDirect(prompt, getChatFallback(query))
            
            val updatedHistory = aiChatHistory.value.toMutableList()
            updatedHistory.add(Pair(result, false))
            aiChatHistory.value = updatedHistory
            isAiLoading.value = false
        }
    }

    fun clearAiChat() {
        aiChatHistory.value = emptyList()
    }

    private fun getRecommendationsFallback(): String {
        val worstSubject = syllabusAnalyticsState.value.worstSubject.ifEmpty { "Physics" }
        return """
            ### 🎯 Today's Recommendations
            - **Master Force and Pressure**: You have 2 unanswered questions in the "Force and Pressure" chapter. Spend 15 minutes reviewing Friction.
            - **Daily Quiz Boost**: Complete today's 5-question Daily Quiz to maintain your streak and earn a **1.5x XP multiplier**.
            - **Squad Challenge**: Participate in the squad study activity to gain an extra +250 XP bonus for your team.
            
            ### 📈 Weak Subjects & Action Plan
            - **Subject Focus**: Your current accuracy in **$worstSubject** is below 60%.
            - **Action Item**: Use the AI Revision generator to review **Flashcards** and complete a targeted **Chapter Quiz**.
            
            ### 🔥 Streak Motivation (${profileState.value?.streak ?: 77} Days!)
            - You are on an outstanding **${profileState.value?.streak ?: 77}-day learning streak**! Keep the fire burning bright. One quick study session today is all it takes to keep it going.
        """.trimIndent()
    }

    private fun getPlannerFallback(mode: String): String {
        return when (mode) {
            "Daily" -> """
                ### 📅 Daily Adaptive Study Plan
                - **08:00 AM - 08:45 AM**: 🔭 Physics - Friction and Lubrication (Review weak topics)
                - **12:00 PM - 12:45 PM**: 🧪 Chemistry - Acids & Indicators (Practice Quiz)
                - **06:00 PM - 07:00 PM**: 📐 Mathematics - Integers (Solve bookmarked doubts)
                
                *🔄 Adaptive Rule: If you miss a session, double tomorrow's focus time on Physics and perform a 5-question quick review.*
            """.trimIndent()
            "Weekly" -> """
                ### 🗓️ Weekly Study Planner
                - **Monday & Tuesday**: 🔭 Physics - Atmospheric Pressure and Hydrostatics
                - **Wednesday & Thursday**: 🧬 Biology - Crop Production & Irrigation Systems
                - **Friday**: 🧪 Chemistry - Revision of Salts & Indicators
                - **Saturday**: 📝 Complete Weekly Mock Test & review weak chapters
                - **Sunday**: 🧠 Review bookmarked questions and plan for next week
                
                *🔄 Adaptive Rule: Missing a day automatically moves 30 mins of practice to Saturday's review session.*
            """.trimIndent()
            else -> """
                ### 🎯 Revision & Exam Preparation Schedule
                - **Day 1-2**: High-yield formulas sheet review & defining key terms
                - **Day 3-4**: Focused chapter practice on weak topics (accuracy < 60%)
                - **Day 5**: Full syllabus Mock Exam under timed conditions
                
                *🔄 Adaptive Rule: If score is below 75%, schedule a 1-on-1 AI revision card generation session.*
            """.trimIndent()
        }
    }

    private fun getRevisionFallback(type: String, subject: String): String {
        return when (type) {
            "Flashcards" -> """
                ### 📇 Flashcards: $subject
                
                **Card 1**: What is Bernoulli's Principle?
                *Answer*: It states that an increase in the speed of a fluid occurs simultaneously with a decrease in static pressure or fluid potential energy.
                
                **Card 2**: What turns blue litmus paper red?
                *Answer*: Acidic solutions.
                
                **Card 3**: Why is rolling friction less than sliding friction?
                *Answer*: Because the area of contact is extremely small during rolling, reducing mechanical resistance.
            """.trimIndent()
            "Mind Maps" -> """
                ### 🗺️ Textual Mind Map: $subject
                
                - **$subject Core Concept**
                  - 🔹 **Sub-branch 1**: Primary Definitions
                    - *Details*: Core formulas, baseline physical laws.
                  - 🔹 **Sub-branch 2**: Key Experiments
                    - *Details*: Real-world applications and indicators.
                  - 🔹 **Sub-branch 3**: Practice & Pitfalls
                    - *Details*: Common mistakes in previous mock tests.
              """.trimIndent()
            "Formulas" -> """
                ### 🧮 Formula & Key Definitions Sheet: $subject
                
                - **Pressure (P)**: P = F / A (Force / Area). Unit: Pascal (Pa)
                - **Frictional Force**: f = \mu * N (Coefficient of friction * Normal Force)
                - **Density**: d = m / V (Mass / Volume)
            """.trimIndent()
            else -> """
                ### 📝 One Page Summary: $subject
                - **Overview**: High-yield notes summarizing the most important elements of $subject for the Class syllabus.
                - **Key Takeaway**: Indicators change color depending on acidity; friction is essential for motion but consumes energy; pressure is inversely proportional to surface area.
            """.trimIndent()
        }
    }

    private fun getChatFallback(query: String): String {
        return """
            Here is a step-by-step explanation for your doubt: "**$query**"
            
            1. **Core Concept**: Let's first look at the baseline logic. Any physical or mathematical concept is best understood when broken down into its fundamental variables.
            2. **Step-by-Step Breakdown**:
               - *Step A*: Identify given values or initial states.
               - *Step B*: Apply relevant theorems, formulas, or chemical rules.
               - *Step C*: Conclude with the direct outcome.
            3. **Real-world Example**: Think of indicators in a chemistry lab or how sliding down a slide depends entirely on friction.
            
            *💡 Tip: Go to the AI Revision tab to generate full flashcards for this topic!*
        """.trimIndent()
    }

    fun getLocalDatabaseSize(): Long = syncManager.getLocalDatabaseSize()
    val lastSyncTimeFlow: StateFlow<Long> = syncManager.lastSyncTime
    val pendingUploadsCountFlow: StateFlow<Int> = syncManager.pendingUploadsCount
    val pendingDownloadsCountFlow: StateFlow<Int> = syncManager.pendingDownloadsCount

    fun triggerSync() {
        syncManager.triggerSync()
    }

    fun backupAccount() {
        viewModelScope.launch {
            syncManager.backupAccountData()
        }
    }

    fun restoreAccount() {
        viewModelScope.launch {
            syncManager.restoreAccountData()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            syncManager.clearCache()
        }
    }

    fun rebuildCache() {
        viewModelScope.launch {
            syncManager.rebuildCache()
        }
    }

    fun resetLocalDatabase() {
        viewModelScope.launch {
            syncManager.resetLocalDatabase()
        }
    }
}

data class ActivityStats(
    val todayStudyMinutes: Int = 0,
    val weeklyStudyMinutes: Int = 0,
    val monthlyStudyMinutes: Int = 0,
    val totalStudyMinutes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val activitiesCompleted: Int = 0,
    val averageSessionMinutes: Int = 0,
    val focusScore: Int = 0
)

