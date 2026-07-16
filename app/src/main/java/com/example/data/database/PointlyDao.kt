package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PointlyDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun getProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfileSync(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("SELECT * FROM missions")
    fun getMissionsFlow(): Flow<List<StudyMissionEntity>>

    @Query("SELECT * FROM missions")
    suspend fun getMissionsSync(): List<StudyMissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<StudyMissionEntity>)

    @Update
    suspend fun updateMission(mission: StudyMissionEntity)

    @Query("SELECT * FROM achievements")
    fun getAchievementsFlow(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAchievementsSync(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM activities ORDER BY startTime DESC")
    fun getActivitiesFlow(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY startTime DESC")
    suspend fun getActivitiesSync(): List<ActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE activityId = :activityId")
    suspend fun deleteActivityById(activityId: String)

    @Query("SELECT * FROM pomodoro_state WHERE id = 1")
    fun getPomodoroStateFlow(): Flow<PomodoroStateEntity?>

    @Query("SELECT * FROM pomodoro_state WHERE id = 1")
    suspend fun getPomodoroStateSync(): PomodoroStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPomodoroState(state: PomodoroStateEntity)

    @Query("SELECT * FROM leaderboard_entries")
    fun getLeaderboardEntriesFlow(): Flow<List<LeaderboardEntryEntity>>

    @Query("SELECT * FROM leaderboard_entries")
    suspend fun getLeaderboardEntriesSync(): List<LeaderboardEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntries(entries: List<LeaderboardEntryEntity>)

    @Query("DELETE FROM leaderboard_entries")
    suspend fun clearLeaderboardEntries()

    // Community Messages
    @Query("SELECT * FROM community_messages WHERE channelId = :channelId ORDER BY timestamp DESC")
    fun getMessagesFlow(channelId: String): Flow<List<RoomMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<RoomMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: RoomMessageEntity)

    @Query("DELETE FROM community_messages WHERE channelId = :channelId")
    suspend fun clearMessages(channelId: String)

    // Showcase Posts
    @Query("SELECT * FROM showcase_posts ORDER BY timestamp DESC")
    fun getShowcasePostsFlow(): Flow<List<RoomShowcasePostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowcasePosts(posts: List<RoomShowcasePostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowcasePost(post: RoomShowcasePostEntity)

    @Query("DELETE FROM showcase_posts")
    suspend fun clearShowcasePosts()

    // Study Squads
    @Query("SELECT * FROM study_squads")
    fun getSquadsFlow(): Flow<List<RoomSquadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSquads(squads: List<RoomSquadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSquad(squad: RoomSquadEntity)

    @Query("DELETE FROM study_squads")
    suspend fun clearSquads()

    // Community Feed
    @Query("SELECT * FROM community_feed ORDER BY timestamp DESC")
    fun getFeedFlow(): Flow<List<RoomFeedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedItems(items: List<RoomFeedItemEntity>)

    @Query("DELETE FROM community_feed")
    suspend fun clearFeed()

    // Community Notifications
    @Query("SELECT * FROM community_notifications ORDER BY timestamp DESC")
    fun getNotificationsFlow(): Flow<List<RoomNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<RoomNotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: RoomNotificationEntity)

    // Syllabus Engine DAO methods
    @Query("SELECT * FROM syllabus_questions WHERE `class` = :className")
    fun getSyllabusQuestionsFlow(className: String): Flow<List<SyllabusQuestionEntity>>

    @Query("SELECT * FROM syllabus_questions WHERE `class` = :className")
    suspend fun getSyllabusQuestionsSync(className: String): List<SyllabusQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyllabusQuestions(questions: List<SyllabusQuestionEntity>)

    @Query("SELECT * FROM syllabus_questions WHERE isBookmarked = 1")
    fun getBookmarkedQuestionsFlow(): Flow<List<SyllabusQuestionEntity>>

    @Query("SELECT * FROM syllabus_questions WHERE wrongAttemptsCount > 0")
    fun getWrongQuestionsFlow(): Flow<List<SyllabusQuestionEntity>>

    @Query("UPDATE syllabus_questions SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: String, isBookmarked: Boolean)

    @Query("UPDATE syllabus_questions SET wrongAttemptsCount = wrongAttemptsCount + :delta WHERE id = :id")
    suspend fun updateWrongAttempts(id: String, delta: Int)

    @Query("UPDATE syllabus_questions SET skippedCount = skippedCount + 1 WHERE id = :id")
    suspend fun incrementSkippedCount(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: SyllabusQuizAttemptEntity)

    @Query("SELECT * FROM syllabus_attempts")
    fun getAttemptsFlow(): Flow<List<SyllabusQuizAttemptEntity>>

    @Query("SELECT * FROM syllabus_attempts")
    suspend fun getAttemptsSync(): List<SyllabusQuizAttemptEntity>

    // --- BADGES ---
    @Query("SELECT * FROM badges")
    fun getBadgesFlow(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges")
    suspend fun getBadgesSync(): List<BadgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<BadgeEntity>)

    @Update
    suspend fun updateBadge(badge: BadgeEntity)

    // --- CHALLENGES ---
    @Query("SELECT * FROM challenges")
    fun getChallengesFlow(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges")
    suspend fun getChallengesSync(): List<ChallengeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<ChallengeEntity>)

    @Update
    suspend fun updateChallenge(challenge: ChallengeEntity)

    // --- DAILY LOGIN REWARDS ---
    @Query("SELECT * FROM daily_login_rewards ORDER BY day ASC")
    fun getDailyLoginRewardsFlow(): Flow<List<DailyLoginRewardEntity>>

    @Query("SELECT * FROM daily_login_rewards ORDER BY day ASC")
    suspend fun getDailyLoginRewardsSync(): List<DailyLoginRewardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyLoginRewards(rewards: List<DailyLoginRewardEntity>)

    @Update
    suspend fun updateDailyLoginReward(reward: DailyLoginRewardEntity)

    // --- SHOP ITEMS ---
    @Query("SELECT * FROM shop_items")
    fun getShopItemsFlow(): Flow<List<ShopItemEntity>>

    @Query("SELECT * FROM shop_items")
    suspend fun getShopItemsSync(): List<ShopItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopItems(items: List<ShopItemEntity>)

    @Update
    suspend fun updateShopItem(item: ShopItemEntity)

    // --- AI CACHE ---
    @Query("SELECT * FROM ai_cache WHERE cacheKey = :key")
    suspend fun getAiCache(key: String): AiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiCache(cache: AiCacheEntity)

    @Query("DELETE FROM ai_cache WHERE cacheKey = :key")
    suspend fun deleteAiCache(key: String)

    @Query("DELETE FROM ai_cache")
    suspend fun clearAiCache()
}
