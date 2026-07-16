package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.database.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class PointlyRepository(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository
) {
    private val database = PointlyDatabase.getDatabase(context)
    private val dao = database.pointlyDao()

    val profileFlow: Flow<ProfileEntity?> = dao.getProfileFlow()
    val missionsFlow: Flow<List<StudyMissionEntity>> = dao.getMissionsFlow()
    val achievementsFlow: Flow<List<AchievementEntity>> = dao.getAchievementsFlow()
    val leaderboardFlow: Flow<List<LeaderboardEntryEntity>> = dao.getLeaderboardEntriesFlow()
    val badgesFlow: Flow<List<BadgeEntity>> = dao.getBadgesFlow()
    val challengesFlow: Flow<List<ChallengeEntity>> = dao.getChallengesFlow()
    val dailyRewardsFlow: Flow<List<DailyLoginRewardEntity>> = dao.getDailyLoginRewardsFlow()
    val shopItemsFlow: Flow<List<ShopItemEntity>> = dao.getShopItemsFlow()

    suspend fun getAiCache(key: String): String? = withContext(Dispatchers.IO) {
        dao.getAiCache(key)?.cacheValue
    }

    suspend fun insertAiCache(key: String, value: String) = withContext(Dispatchers.IO) {
        dao.insertAiCache(AiCacheEntity(key, value))
    }

    suspend fun deleteAiCache(key: String) = withContext(Dispatchers.IO) {
        dao.deleteAiCache(key)
    }

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        val currentProfile = dao.getProfileSync()
        if (currentProfile == null) {
            // Seed Database on first launch
            Log.d("PointlyRepository", "Database empty. Seeding initial gamified data...")
            dao.insertProfile(
                ProfileEntity(
                    id = 1,
                    name = "John Doe",
                    level = 14,
                    xp = 2100, // 84% of 2500 needed to level up
                    coins = 500,
                    gems = 50,
                    streak = 77,
                    rank = 4,
                    title = "Gold Tier",
                    weeklyStudyHours = 15.0f,
                    weeklyGoalHours = 20.0f
                )
            )

            dao.insertMissions(
                listOf(
                    StudyMissionEntity(
                        title = "Mastery Challenge: Fluid Dynamics",
                        subject = "Physics",
                        description = "Master Bernoulli's principle, turbulent flow, and hydrodynamic pressure.",
                        xpReward = 250,
                        completed = false,
                        timeRemaining = "04:12:45"
                    ),
                    StudyMissionEntity(
                        title = "Stellar Evolution",
                        subject = "Astrophysics",
                        description = "Learn the lifecycles of main sequence stars, red giants, and supernovas.",
                        xpReward = 180,
                        completed = false,
                        timeRemaining = "18:45:10"
                    ),
                    StudyMissionEntity(
                        title = "Recursive Logic",
                        subject = "Computer Science",
                        description = "Complete 3 algorithmic structures using recursion and dynamic programming.",
                        xpReward = 300,
                        completed = true,
                        timeRemaining = "Completed"
                    )
                )
            )
        }

        // Always seed Achievements, Badges, Shop items, Challenges, and Login rewards if empty
        val achievements = dao.getAchievementsSync()
        if (achievements.isEmpty()) {
            dao.insertAchievements(
                listOf(
                    AchievementEntity(id = "streak_7", title = "Flame Apprentice", description = "Maintained a 7-day study streak.", icon = "🔥", rarity = "Common", category = "Streak", xpReward = 100, coinReward = 50, progress = 7, target = 7, completed = true, claimed = true, earned = true, earnedAt = System.currentTimeMillis()),
                    AchievementEntity(id = "streak_77", title = "Streak Champion", description = "Achieved the legendary 77-day streak!", icon = "☄️", rarity = "Legendary", category = "Streak", xpReward = 1000, coinReward = 500, progress = 77, target = 77, completed = true, claimed = true, earned = true, earnedAt = System.currentTimeMillis()),
                    AchievementEntity(id = "first_quiz", title = "Academic Spark", description = "Completed your first Gemini study quiz.", icon = "💡", rarity = "Common", category = "Quiz", xpReward = 150, coinReward = 75, progress = 0, target = 1, completed = false, claimed = false, earned = false, earnedAt = 0L),
                    AchievementEntity(id = "level_10", title = "Decathlon Scholar", description = "Reached Student Level 10.", icon = "👑", rarity = "Rare", category = "Study", xpReward = 300, coinReward = 150, progress = 14, target = 10, completed = true, claimed = true, earned = true, earnedAt = System.currentTimeMillis()),
                    AchievementEntity(id = "gemini_partner", title = "AI Synthesis", description = "Generated a custom smart study topic with Gemini.", icon = "🧠", rarity = "Rare", category = "Special Events", xpReward = 200, coinReward = 100, progress = 0, target = 1, completed = false, claimed = false, earned = false, earnedAt = 0L),
                    AchievementEntity(id = "mission_master", title = "Mission Accomplished", description = "Completed all active study challenges.", icon = "🏆", rarity = "Epic", category = "Showcase", xpReward = 500, coinReward = 250, progress = 0, target = 3, completed = false, claimed = false, earned = false, earnedAt = 0L),
                    AchievementEntity(id = "perfect_quiz", title = "Absolute Precision", description = "Score 100% on any practice quiz.", icon = "🎯", rarity = "Rare", category = "Quiz", xpReward = 200, coinReward = 100, progress = 0, target = 1, completed = false, claimed = false, earned = false, earnedAt = 0L),
                    AchievementEntity(id = "community_hero", title = "Community Helper", description = "Reply to 5 classmates' questions.", icon = "🤝", rarity = "Common", category = "Community", xpReward = 150, coinReward = 80, progress = 0, target = 5, completed = false, claimed = false, earned = false, earnedAt = 0L)
                )
            )
        }

        val badges = dao.getBadgesSync()
        if (badges.isEmpty()) {
            dao.insertBadges(
                listOf(
                    BadgeEntity("first_session", "First Study Session", "Took the first steps in studying", "📚", "Common"),
                    BadgeEntity("streak_7_badge", "7 Day Streak", "Studied 7 days in a row", "🔥", "Rare"),
                    BadgeEntity("streak_30_badge", "30 Day Streak", "Unstoppable 30 day learning streak", "⚡", "Epic"),
                    BadgeEntity("quiz_master", "Quiz Master", "Completed 10 quizzes", "🎓", "Epic"),
                    BadgeEntity("perfect_score", "Perfect Score", "Got 100% in a quiz", "🎯", "Rare"),
                    BadgeEntity("science_genius", "Science Genius", "Completed 5 science challenges", "🔬", "Epic"),
                    BadgeEntity("math_wizard", "Math Wizard", "Completed 5 algebra sessions", "📐", "Legendary"),
                    BadgeEntity("reading_champion", "Reading Champion", "Read for over 10 hours", "📖", "Common"),
                    BadgeEntity("coding_star", "Coding Star", "Solved 20 coding challenges", "💻", "Animated"),
                    BadgeEntity("creative_artist", "Creative Artist", "Created a showcase project", "🎨", "Rare"),
                    BadgeEntity("community_helper", "Community Helper", "Received 5 helpful feedback ratings", "🤝", "Common"),
                    BadgeEntity("top_10", "Top 10 Rank", "Reached top 10 in Weekly Leaderboard", "🏅", "Rare"),
                    BadgeEntity("top_3", "Top 3 Rank", "Reached top 3 in Weekly Leaderboard", "🥈", "Epic"),
                    BadgeEntity("rank_1", "Rank #1", "Ranked #1 overall in the school leaderboard", "🥇", "Legendary"),
                    BadgeEntity("squad_leader", "Squad Leader", "Led a squad in weekly achievements", "🎖️", "Seasonal"),
                    BadgeEntity("club_100", "100 Hour Club", "Spent 100 hours learning", "⏱️", "Animated"),
                    BadgeEntity("club_1000", "1000 XP Club", "Gained 1000 total XP", "✨", "Epic")
                )
            )
        }

        val shopItems = dao.getShopItemsSync()
        if (shopItems.isEmpty()) {
            dao.insertShopItems(
                listOf(
                    ShopItemEntity("frame_neon", "Neon Aura Frame", "avatar_frame", 150, false, "✨"),
                    ShopItemEntity("frame_cyber", "Cyberpunk Frame", "avatar_frame", 300, false, "🤖"),
                    ShopItemEntity("theme_dark", "Space Nebula Theme", "profile_theme", 200, false, "🌌"),
                    ShopItemEntity("theme_cherry", "Cherry Blossom Theme", "profile_theme", 250, false, "🌸"),
                    ShopItemEntity("bubble_emerald", "Emerald Speech", "chat_bubble", 100, false, "💬"),
                    ShopItemEntity("bubble_gold", "Golden Word", "chat_bubble", 180, false, "🔔"),
                    ShopItemEntity("bg_futuristic", "Retro Grid BG", "profile_background", 350, false, "🎛️"),
                    ShopItemEntity("bg_anime", "Manga Canvas BG", "profile_background", 400, false, "🎑"),
                    ShopItemEntity("decor_sparks", "Sparks Overlay", "animated_decoration", 500, false, "✨"),
                    ShopItemEntity("decor_confetti", "Confetti Flow", "animated_decoration", 600, false, "🎉")
                )
            )
        }

        val challenges = dao.getChallengesSync()
        if (challenges.isEmpty()) {
            dao.insertChallenges(
                listOf(
                    ChallengeEntity("daily_quiz", "Complete Daily Quiz", "Test your knowledge in any active subject", 100, 50, 0, 0, 1, "daily"),
                    ChallengeEntity("daily_help", "Help 5 Classmates", "Answer questions in the community board", 120, 60, 5, 0, 5, "daily"),
                    ChallengeEntity("weekly_study", "Study 60 Minutes", "Log continuous study sessions to reach 60m", 250, 150, 10, 0, 60, "weekly"),
                    ChallengeEntity("weekly_pomo", "Finish 3 Pomodoros", "Use the Pomodoro focus timer to learn", 180, 100, 5, 0, 3, "weekly"),
                    ChallengeEntity("monthly_upload", "Upload One Project", "Share a study helper project on Showcase", 500, 300, 25, 0, 1, "monthly")
                )
            )
        }

        val dailyRewards = dao.getDailyLoginRewardsSync()
        if (dailyRewards.isEmpty()) {
            dao.insertDailyLoginRewards(
                (1..30).map { day ->
                    val type = when (day) {
                        7 -> "7day"
                        30 -> "30day"
                        else -> "daily"
                    }
                    val coinRew = 20 + day * 5
                    val gemRew = if (day % 7 == 0) 5 else 0
                    val xpRew = 50 + day * 10
                    DailyLoginRewardEntity(
                        day = day,
                        title = "Day $day Reward",
                        coinReward = coinRew,
                        gemReward = gemRew,
                        xpReward = xpRew,
                        claimed = false,
                        rewardType = type
                    )
                }
            )
        }
    }

    suspend fun updateProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) {
        dao.updateProfile(profile)
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                val updates = mapOf(
                    "name" to profile.name,
                    "level" to profile.level,
                    "xp" to profile.xp,
                    "streak" to profile.streak,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestoreRepository.saveDocument("users", user.uid, updates)
            } catch (e: Exception) {
                Log.e("PointlyRepository", "Error updating Firestore profile", e)
            }
        }
    }

    suspend fun updateMission(mission: StudyMissionEntity) = withContext(Dispatchers.IO) {
        dao.updateMission(mission)
    }

    suspend fun getMissionsSync(): List<StudyMissionEntity> = withContext(Dispatchers.IO) {
        dao.getMissionsSync()
    }

    suspend fun updateAchievement(achievement: AchievementEntity) = withContext(Dispatchers.IO) {
        dao.updateAchievement(achievement)
    }

    suspend fun earnXp(amount: Int): ProfileEntity? = withContext(Dispatchers.IO) {
        val profile = dao.getProfileSync() ?: return@withContext null
        var newXp = profile.xp + amount
        var newLevel = profile.level
        val xpPerLevel = 2500 // XP threshold for leveling up

        while (newXp >= xpPerLevel) {
            newXp -= xpPerLevel
            newLevel++
        }

        val updatedProfile = profile.copy(xp = newXp, level = newLevel)
        dao.updateProfile(updatedProfile)
        
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                val updates = mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "points" to newXp,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestoreRepository.saveDocument("users", user.uid, updates)
            } catch (e: Exception) {
                Log.e("PointlyRepository", "Error updating Firestore XP", e)
            }
        }
        
        if (newLevel > profile.level) {
            postToCommunityFeedAndNotify(
                title = "Level Up!",
                message = "has advanced to Level $newLevel! Absolute champion performance!",
                type = "level_up"
            )
        }

        // Trigger level 10 achievement if applicable
        if (newLevel >= 10) {
            unlockAchievement("level_10")
        }

        // 1000 XP Club Badge Check
        val totalXpAccumulated = newLevel * xpPerLevel + newXp
        if (totalXpAccumulated >= 1000) {
            unlockBadge("club_1000")
        }

        updatedProfile
    }

    suspend fun unlockAchievement(id: String) = withContext(Dispatchers.IO) {
        progressAchievement(id, 1)
    }

    val activitiesFlow: Flow<List<ActivityEntity>> = dao.getActivitiesFlow()

    suspend fun insertActivity(activity: ActivityEntity) = withContext(Dispatchers.IO) {
        dao.insertActivity(activity)
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                firestoreRepository.saveDocument("activities", activity.activityId, activity)
            } catch (e: Exception) {
                Log.e("PointlyRepository", "Failed to upload activity to Firestore, cached locally.", e)
            }
        }
        
        // Accumulate weekly study hours in user profile dynamically
        val hours = activity.duration / 3600f
        val profile = dao.getProfileSync()
        if (profile != null) {
            val updatedHours = (profile.weeklyStudyHours + hours).coerceAtMost(24.0f)
            val updatedProfile = profile.copy(weeklyStudyHours = updatedHours, updatedAt = System.currentTimeMillis())
            dao.updateProfile(updatedProfile)
        }
    }

    suspend fun deleteActivity(activityId: String) = withContext(Dispatchers.IO) {
        dao.deleteActivityById(activityId)
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                firestoreRepository.deleteDocument("activities", activityId)
            } catch (e: Exception) {
                Log.e("PointlyRepository", "Failed to delete activity from Firestore.", e)
            }
        }
    }

    suspend fun updateActivity(activity: ActivityEntity) = withContext(Dispatchers.IO) {
        dao.updateActivity(activity)
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                firestoreRepository.saveDocument("activities", activity.activityId, activity)
            } catch (e: Exception) {
                Log.e("PointlyRepository", "Failed to update activity on Firestore.", e)
            }
        }
    }

    val pomodoroStateFlow: Flow<PomodoroStateEntity?> = dao.getPomodoroStateFlow()

    suspend fun getPomodoroState(): PomodoroStateEntity? = withContext(Dispatchers.IO) {
        dao.getPomodoroStateSync()
    }

    suspend fun savePomodoroState(state: PomodoroStateEntity) = withContext(Dispatchers.IO) {
        dao.insertPomodoroState(state)
    }

    private fun getStartOfToday(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfWeek(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    suspend fun getUserStatsMetrics(): Map<String, Any> = withContext(Dispatchers.IO) {
        val activities = dao.getActivitiesSync().filter { it.completed }
        
        val startOfToday = getStartOfToday()
        val startOfWeek = getStartOfWeek()
        val startOfMonth = getStartOfMonth()
        
        val todayPoints = activities.filter { it.endTime >= startOfToday }.sumOf { it.pointsEarned }
        val weeklyPoints = activities.filter { it.endTime >= startOfWeek }.sumOf { it.pointsEarned }
        val monthlyPoints = activities.filter { it.endTime >= startOfMonth }.sumOf { it.pointsEarned }
        val allTimePoints = activities.sumOf { it.pointsEarned } + 100 // include 100 registration points
        
        val totalStudyTimeMins = activities.sumOf { it.duration } / 60
        val totalActivities = activities.size
        
        mapOf(
            "todayPoints" to todayPoints,
            "weeklyPoints" to weeklyPoints,
            "monthlyPoints" to monthlyPoints,
            "allTimePoints" to allTimePoints,
            "totalStudyTime" to totalStudyTimeMins,
            "activitiesCompleted" to totalActivities
        )
    }

    suspend fun cacheLeaderboard(users: List<com.example.data.model.UserDocument>) = withContext(Dispatchers.IO) {
        dao.clearLeaderboardEntries()
        val entries = users.map { doc ->
            LeaderboardEntryEntity(
                uid = doc.uid,
                name = doc.name,
                username = doc.username,
                className = doc.className,
                section = doc.section,
                profileImage = doc.profileImage,
                points = doc.points,
                xp = doc.xp,
                level = doc.level,
                streak = doc.streak,
                weeklyPoints = doc.weeklyPoints,
                monthlyPoints = doc.monthlyPoints,
                todayPoints = doc.todayPoints,
                activitiesCompleted = doc.activitiesCompleted,
                totalStudyTime = doc.totalStudyTime,
                updatedAt = doc.updatedAtLong
            )
        }
        dao.insertLeaderboardEntries(entries)
    }

    suspend fun earnRewards(xpAmount: Int, pointsAmount: Int): ProfileEntity? = withContext(Dispatchers.IO) {
        val profile = dao.getProfileSync() ?: return@withContext null
        var newXp = profile.xp + xpAmount
        var newLevel = profile.level
        val xpPerLevel = 2500

        while (newXp >= xpPerLevel) {
            newXp -= xpPerLevel
            newLevel++
        }

        val updatedProfile = profile.copy(xp = newXp, level = newLevel, updatedAt = System.currentTimeMillis())
        dao.updateProfile(updatedProfile)

        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                val statsMetrics = getUserStatsMetrics()
                val todayPoints = statsMetrics["todayPoints"] as Int
                val weeklyPoints = statsMetrics["weeklyPoints"] as Int
                val monthlyPoints = statsMetrics["monthlyPoints"] as Int
                val allTimePoints = statsMetrics["allTimePoints"] as Int
                val totalStudyTime = statsMetrics["totalStudyTime"] as Int
                val activitiesCompleted = statsMetrics["activitiesCompleted"] as Int
                
                val updates = mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "points" to allTimePoints,
                    "todayPoints" to todayPoints,
                    "weeklyPoints" to weeklyPoints,
                    "monthlyPoints" to monthlyPoints,
                    "totalStudyTime" to totalStudyTime,
                    "activitiesCompleted" to activitiesCompleted,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestoreRepository.saveDocument("users", user.uid, updates)
            } catch (e: Exception) {
                Log.e("PointlyRepository", "Error updating Firestore rewards", e)
            }
        }

        if (newLevel >= 10) {
            unlockAchievement("level_10")
        }

        updatedProfile
    }

    /**
     * Fetch study quiz content from Gemini API or fallback to mock data.
     */
    suspend fun generateQuiz(topic: String, subject: String): QuizResponse = withContext(Dispatchers.IO) {
        val key = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            Log.w("PointlyRepository", "Gemini API key is empty/placeholder. Using local simulation.")
            return@withContext getMockQuiz(topic, subject)
        }

        val prompt = """
            You are Pointly 77's custom gamified AI Learning Companion.
            Create an exciting, high-quality, 3-question educational quiz for the topic: "$topic" in "$subject".
            Each question must have exactly 4 multiple-choice options and a 0-indexed correctOption index.
            Keep explanations encouraging and concise.
            
            Return ONLY a JSON object that adheres strictly to the following schema without any surrounding backticks, markdown markers, or explanatory text:
            {
              "subject": "$subject",
              "topic": "$topic",
              "explanation": "Brief overview of the concept.",
              "questions": [
                {
                  "id": 1,
                  "question": "A concise multiple choice question",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctOption": 1,
                  "explanation": "Why Option B is correct."
                }
              ]
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            generationConfig = GeminiGenerationConfig(responseMimeType = "application/json", temperature = 0.7f)
        )

        try {
            val response = RetrofitClient.service.generateContent(key, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from Gemini")
            
            // Clean up backticks or markdowns if returned by model
            val cleanedJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = RetrofitClient.moshiInstance.adapter(QuizResponse::class.java)
            adapter.fromJson(cleanedJson) ?: throw Exception("Failed to parse JSON")
        } catch (e: Exception) {
            Log.e("PointlyRepository", "Gemini API error: ${e.message}. Falling back to simulation.", e)
            getMockQuiz(topic, subject)
        }
    }

    private fun getMockQuiz(topic: String, subject: String): QuizResponse {
        // Fallback generator with premium hand-crafted educational content
        val normalizedTopic = topic.lowercase().trim()
        if (normalizedTopic.contains("fluid") || normalizedTopic.contains("bernoulli") || normalizedTopic.contains("physics")) {
            return QuizResponse(
                subject = "Physics",
                topic = "Bernoulli's Principle",
                explanation = "Bernoulli's principle states that for an inviscid flow of a nonconducting fluid, an increase in the speed of the fluid occurs simultaneously with a decrease in pressure or a decrease in the fluid's potential energy.",
                questions = listOf(
                    QuizQuestion(
                        id = 1,
                        question = "According to Bernoulli's principle, what happens to fluid pressure when fluid velocity increases?",
                        options = listOf(
                            "Pressure increases proportionally",
                            "Pressure decreases",
                            "Pressure remains completely static",
                            "Pressure drops to zero immediately"
                        ),
                        correctOption = 1,
                        explanation = "An increase in velocity is accompanied by a corresponding decrease in static pressure."
                    ),
                    QuizQuestion(
                        id = 2,
                        question = "Which real-world application directly relies on Bernoulli's principle for functioning?",
                        options = listOf(
                            "Hydroelectric dams",
                            "Aerodynamic wing lift in aircraft",
                            "Friction in automobile brakes",
                            "Convection currents in soup"
                        ),
                        correctOption = 1,
                        explanation = "The curved upper surface of an airplane wing forces air to travel faster over the top, reducing pressure and creating aerodynamic lift."
                    ),
                    QuizQuestion(
                        id = 3,
                        question = "What fluid model must be assumed for Bernoulli's equation to hold strictly true?",
                        options = listOf(
                            "Highly viscous and turbulent fluid",
                            "Incompressible and non-viscous fluid with steady flow",
                            "Supersonic gaseous plasma",
                            "Perfect vacuum without molecules"
                        ),
                        correctOption = 1,
                        explanation = "Bernoulli's equation assumes steady, incompressible, non-viscous (frictionless) laminar flow."
                    )
                )
            )
        } else {
            // General high-quality placeholder quiz
            return QuizResponse(
                subject = subject,
                topic = topic,
                explanation = "Exploring $topic helps build fundamental comprehension in $subject. This quiz tests core conceptual pillars.",
                questions = listOf(
                    QuizQuestion(
                        id = 1,
                        question = "What is the primary foundation when analyzing $topic?",
                        options = listOf(
                            "Observing repeatable empirical patterns",
                            "Ignoring mathematical formulas",
                            "Relying entirely on random intuition",
                            "None of the above"
                        ),
                        correctOption = 0,
                        explanation = "Scientific analysis begins with direct observation and repeatable empirical testing."
                    ),
                    QuizQuestion(
                        id = 2,
                        question = "Why is studying $topic essential to the broader domain of $subject?",
                        options = listOf(
                            "It simplifies memorization for grading",
                            "It unlocks deep conceptual connections and predictive frameworks",
                            "It is a filler topic without application",
                            "It replaces traditional research labs"
                        ),
                        correctOption = 1,
                        explanation = "Mastering core principles unlocks intuitive connections across multiple disciplines."
                    ),
                    QuizQuestion(
                        id = 3,
                        question = "Which action maximizes understanding of $topic?",
                        options = listOf(
                            "Passive reading without active review",
                            "Active study, interactive quiz retrieval, and AI synthesis",
                            "Rote memorization overnight",
                            "Postponing study sessions indefinitely"
                        ),
                        correctOption = 1,
                        explanation = "Active retrieval, testing, and concept explanation (Feynman Technique) are proven to solidify long-term memory."
                    )
                )
            )
        }
    }

    // Syllabus Engine functions
    fun getSyllabusQuestionsFlow(className: String): Flow<List<SyllabusQuestionEntity>> {
        return dao.getSyllabusQuestionsFlow(className)
    }

    suspend fun getSyllabusQuestionsSync(className: String): List<SyllabusQuestionEntity> = withContext(Dispatchers.IO) {
        dao.getSyllabusQuestionsSync(className)
    }

    suspend fun insertSyllabusQuestions(questions: List<SyllabusQuestionEntity>) = withContext(Dispatchers.IO) {
        dao.insertSyllabusQuestions(questions)
    }

    fun getBookmarkedQuestionsFlow(): Flow<List<SyllabusQuestionEntity>> {
        return dao.getBookmarkedQuestionsFlow()
    }

    fun getWrongQuestionsFlow(): Flow<List<SyllabusQuestionEntity>> {
        return dao.getWrongQuestionsFlow()
    }

    suspend fun updateBookmarkStatus(id: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateBookmarkStatus(id, isBookmarked)
    }

    suspend fun updateWrongAttempts(id: String, delta: Int) = withContext(Dispatchers.IO) {
        dao.updateWrongAttempts(id, delta)
    }

    suspend fun incrementSkippedCount(id: String) = withContext(Dispatchers.IO) {
        dao.incrementSkippedCount(id)
    }

    suspend fun insertAttempt(attempt: SyllabusQuizAttemptEntity) = withContext(Dispatchers.IO) {
        dao.insertAttempt(attempt)
    }

    fun getAttemptsFlow(): Flow<List<SyllabusQuizAttemptEntity>> {
        return dao.getAttemptsFlow()
    }

    suspend fun getAttemptsSync(): List<SyllabusQuizAttemptEntity> = withContext(Dispatchers.IO) {
        dao.getAttemptsSync()
    }

    // Dynamic Synchronization with Firestore for Syllabus Questions
    suspend fun syncSyllabusQuestions(className: String) = withContext(Dispatchers.IO) {
        try {
            // Fetch questions from local Room
            val local = dao.getSyllabusQuestionsSync(className)
            if (local.isEmpty()) {
                // If empty locally, check Firestore first
                val remoteSnap = firestoreRepository.db.collection("syllabus_questions")
                    .whereEqualTo("class", className)
                    .get()
                    .await()
                
                if (remoteSnap.isEmpty) {
                    // Seed questions locally, and then upload them to Firestore!
                    Log.d("PointlyRepository", "Syllabus empty both local and remote. Seeding defaults for $className.")
                    val seeded = SyllabusEngine.getSeededQuestions().filter { it.`class`.equals(className, ignoreCase = true) }
                    if (seeded.isNotEmpty()) {
                        dao.insertSyllabusQuestions(seeded.map { it.toEntity() })
                        // Upload seeded questions to Firestore for sync!
                        for (q in seeded) {
                            firestoreRepository.saveDocument("syllabus_questions", q.id, q)
                        }
                    }
                } else {
                    // Map Firestore documents back to SyllabusQuestion and save to Room
                    val questions = remoteSnap.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(com.example.data.model.SyllabusQuestion::class.java)
                        } catch (e: Exception) {
                            Log.e("PointlyRepository", "Error deserializing syllabus question from Firestore", e)
                            null
                        }
                    }
                    dao.insertSyllabusQuestions(questions.map { it.toEntity() })
                    Log.d("PointlyRepository", "Synced ${questions.size} syllabus questions from Firestore for $className.")
                }
            } else {
                Log.d("PointlyRepository", "Loaded ${local.size} syllabus questions from local cache for $className.")
            }
        } catch (e: Exception) {
            Log.e("PointlyRepository", "Error syncing syllabus questions for $className: ${e.message}")
            // Fallback: seed locally so it's robust and offline-ready!
            val seeded = SyllabusEngine.getSeededQuestions().filter { it.`class`.equals(className, ignoreCase = true) }
            val existing = dao.getSyllabusQuestionsSync(className)
            if (seeded.isNotEmpty() && existing.isEmpty()) {
                dao.insertSyllabusQuestions(seeded.map { it.toEntity() })
            }
        }
    }

    // --- gamified Systems: Achievements, Badges, Economy, Challenges, Shop ---

    suspend fun earnCoins(amount: Int) = withContext(Dispatchers.IO) {
        val profile = dao.getProfileSync() ?: return@withContext
        val updated = profile.copy(coins = profile.coins + amount, updatedAt = System.currentTimeMillis())
        dao.updateProfile(updated)
        syncProfileToFirestore(updated)
    }

    suspend fun earnGems(amount: Int) = withContext(Dispatchers.IO) {
        val profile = dao.getProfileSync() ?: return@withContext
        val updated = profile.copy(gems = profile.gems + amount, updatedAt = System.currentTimeMillis())
        dao.updateProfile(updated)
        syncProfileToFirestore(updated)
    }

    suspend fun claimDailyReward(day: Int) = withContext(Dispatchers.IO) {
        val reward = dao.getDailyLoginRewardsSync().find { it.day == day } ?: return@withContext
        if (reward.claimed) return@withContext
        
        val updatedReward = reward.copy(claimed = true)
        dao.updateDailyLoginReward(updatedReward)
        
        // Give resources
        earnCoins(reward.coinReward)
        earnGems(reward.gemReward)
        earnXp(reward.xpReward)
        
        // Update profile streak / login states
        val profile = dao.getProfileSync()
        if (profile != null) {
            val updatedProfile = profile.copy(
                lastDailyRewardClaimedAt = System.currentTimeMillis(),
                consecutiveLoginDays = day,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateProfile(updatedProfile)
            syncProfileToFirestore(updatedProfile)
        }
        
        postToCommunityFeedAndNotify(
            title = "Daily Reward Claimed!",
            message = "Claimed Day $day reward of ${reward.coinReward} coins, ${reward.gemReward} gems, and ${reward.xpReward} XP!",
            type = "daily_reward"
        )
    }

    suspend fun progressChallenge(challengeId: String, delta: Int) = withContext(Dispatchers.IO) {
        val challenge = dao.getChallengesSync().find { it.challengeId == challengeId } ?: return@withContext
        if (challenge.completed) return@withContext
        
        val newProgress = (challenge.progress + delta).coerceAtMost(challenge.targetValue)
        val isCompleted = newProgress >= challenge.targetValue
        val updated = challenge.copy(
            progress = newProgress,
            completed = isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateChallenge(updated)
        
        if (isCompleted) {
            earnCoins(challenge.coinReward)
            earnGems(challenge.gemReward)
            earnXp(challenge.xpReward)
            
            postToCommunityFeedAndNotify(
                title = "Challenge Completed!",
                message = "Completed challenge: ${challenge.title}! Earned ${challenge.coinReward} coins, ${challenge.gemReward} gems, and ${challenge.xpReward} XP!",
                type = "challenge"
            )
        }
    }

    suspend fun claimChallengeReward(challengeId: String) = withContext(Dispatchers.IO) {
        val challenge = dao.getChallengesSync().find { it.challengeId == challengeId } ?: return@withContext
        if (!challenge.completed || challenge.claimed) return@withContext
        
        val updated = challenge.copy(claimed = true, updatedAt = System.currentTimeMillis())
        dao.updateChallenge(updated)
        
        earnCoins(challenge.coinReward)
        earnGems(challenge.gemReward)
        earnXp(challenge.xpReward)
        
        postToCommunityFeedAndNotify(
            title = "Challenge Claimed!",
            message = "Claimed rewards for ${challenge.title}!",
            type = "challenge_claimed"
        )
    }

    suspend fun purchaseShopItem(itemId: String): Boolean = withContext(Dispatchers.IO) {
        val item = dao.getShopItemsSync().find { it.id == itemId } ?: return@withContext false
        if (item.purchased) return@withContext false
        
        val profile = dao.getProfileSync() ?: return@withContext false
        if (profile.coins < item.cost) return@withContext false
        
        // Deduct coins and update item
        val updatedItem = item.copy(purchased = true)
        dao.updateShopItem(updatedItem)
        
        // Equip item based on category:
        val list = profile.unlockedCosmetics.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (!list.contains(itemId)) {
            list.add(itemId)
        }
        val unlockedStr = list.joinToString(",")
        
        val updatedProfile = when (item.category) {
            "avatar_frame" -> profile.copy(coins = profile.coins - item.cost, avatarFrame = itemId, unlockedCosmetics = unlockedStr, updatedAt = System.currentTimeMillis())
            "profile_theme" -> profile.copy(coins = profile.coins - item.cost, profileTheme = itemId, unlockedCosmetics = unlockedStr, updatedAt = System.currentTimeMillis())
            "chat_bubble" -> profile.copy(coins = profile.coins - item.cost, chatBubbleColor = itemId, unlockedCosmetics = unlockedStr, updatedAt = System.currentTimeMillis())
            "badge_frame" -> profile.copy(coins = profile.coins - item.cost, badgeFrame = itemId, unlockedCosmetics = unlockedStr, updatedAt = System.currentTimeMillis())
            "profile_background" -> profile.copy(coins = profile.coins - item.cost, profileBackground = itemId, unlockedCosmetics = unlockedStr, updatedAt = System.currentTimeMillis())
            "animated_decoration" -> profile.copy(coins = profile.coins - item.cost, animatedDecoration = itemId, unlockedCosmetics = unlockedStr, updatedAt = System.currentTimeMillis())
            else -> profile.copy(coins = profile.coins - item.cost, unlockedCosmetics = unlockedStr, updatedAt = System.currentTimeMillis())
        }
        dao.updateProfile(updatedProfile)
        syncProfileToFirestore(updatedProfile)
        return@withContext true
    }

    suspend fun unlockBadge(id: String) = withContext(Dispatchers.IO) {
        val badge = dao.getBadgesSync().find { it.id == id } ?: return@withContext
        if (badge.unlocked) return@withContext
        
        val updated = badge.copy(unlocked = true, unlockedAt = System.currentTimeMillis())
        dao.updateBadge(updated)
        
        postToCommunityFeedAndNotify(
            title = "Badge Earned!",
            message = "Unlocked the legendary badge: ${badge.title} (${badge.rarity})!",
            type = "badge"
        )
    }

    suspend fun claimAchievementReward(id: String) = withContext(Dispatchers.IO) {
        val achievement = dao.getAchievementsSync().find { it.id == id } ?: return@withContext
        if (!achievement.completed || achievement.claimed) return@withContext
        
        val updated = achievement.copy(claimed = true, updatedAt = System.currentTimeMillis())
        dao.updateAchievement(updated)
        
        earnCoins(achievement.coinReward)
        earnXp(achievement.xpReward)
        
        postToCommunityFeedAndNotify(
            title = "Achievement Claimed!",
            message = "Claimed rewards for '${achievement.title}': ${achievement.coinReward} coins and ${achievement.xpReward} XP!",
            type = "achievement_claimed"
        )
    }

    suspend fun progressAchievement(id: String, delta: Int) = withContext(Dispatchers.IO) {
        val achievement = dao.getAchievementsSync().find { it.id == id } ?: return@withContext
        if (achievement.completed) return@withContext
        
        val newProgress = (achievement.progress + delta).coerceAtMost(achievement.target)
        val isCompleted = newProgress >= achievement.target
        val updated = achievement.copy(
            progress = newProgress,
            completed = isCompleted,
            earned = isCompleted,
            earnedAt = if (isCompleted) System.currentTimeMillis() else 0L,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateAchievement(updated)
        
        if (isCompleted) {
            postToCommunityFeedAndNotify(
                title = "Achievement Unlocked!",
                message = "Unlocked Achievement: ${achievement.title}! Claim your rewards in the Achievement panel.",
                type = "achievement"
            )
        }
    }

    suspend fun postToCommunityFeedAndNotify(title: String, message: String, type: String) = withContext(Dispatchers.IO) {
        val profile = dao.getProfileSync() ?: return@withContext
        val feedId = java.util.UUID.randomUUID().toString()
        val feedItem = RoomFeedItemEntity(
            id = feedId,
            uid = "user_1",
            name = profile.name,
            type = type,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        dao.insertFeedItems(listOf(feedItem))
        
        val notificationId = java.util.UUID.randomUUID().toString()
        val notification = RoomNotificationEntity(
            id = notificationId,
            recipientUid = "user_1",
            title = title,
            message = message,
            type = type,
            isRead = false,
            timestamp = System.currentTimeMillis()
        )
        dao.insertNotification(notification)
        
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                firestoreRepository.saveDocument("feed", feedId, feedItem.copy(uid = user.uid))
                firestoreRepository.saveDocument("notifications", notificationId, notification.copy(recipientUid = user.uid))
            } catch (e: Exception) {
                Log.e("PointlyRepository", "Error syncing community post/notification", e)
            }
        }
    }

    private suspend fun syncProfileToFirestore(profile: ProfileEntity) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                val updates = mapOf(
                    "name" to profile.name,
                    "level" to profile.level,
                    "xp" to profile.xp,
                    "streak" to profile.streak,
                    "coins" to profile.coins,
                    "gems" to profile.gems,
                    "avatarFrame" to profile.avatarFrame,
                    "profileTheme" to profile.profileTheme,
                    "chatBubbleColor" to profile.chatBubbleColor,
                    "badgeFrame" to profile.badgeFrame,
                    "profileBackground" to profile.profileBackground,
                    "animatedDecoration" to profile.animatedDecoration,
                    "unlockedCosmetics" to profile.unlockedCosmetics,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestoreRepository.saveDocument("users", user.uid, updates)
            } catch (e: Exception) {
                Log.e("PointlyRepository", "Error syncing profile to firestore", e)
            }
        }
    }
}
