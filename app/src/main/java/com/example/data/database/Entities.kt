package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "John Doe",
    val level: Int = 14,
    val xp: Int = 2100, // XP out of 2500 for level up
    val coins: Int = 500, // Phase 3 Virtual Currency
    val gems: Int = 50,   // Phase 3 Virtual Currency
    val streak: Int = 77,
    val rank: Int = 4,
    val title: String = "Gold Tier",
    val weeklyStudyHours: Float = 15.0f,
    val weeklyGoalHours: Float = 20.0f,
    val currentSeason: String = "Season 1: Genesis",
    val avatarFrame: String = "none",
    val profileTheme: String = "default",
    val chatBubbleColor: String = "default",
    val badgeFrame: String = "none",
    val profileBackground: String = "default",
    val animatedDecoration: String = "none",
    val unlockedCosmetics: String = "", // Comma-separated list
    val lastLoginTimestamp: Long = 0L,
    val consecutiveLoginDays: Int = 1,
    val lastDailyRewardClaimedAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "missions")
data class StudyMissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val description: String,
    val xpReward: Int,
    val completed: Boolean = false,
    val timeRemaining: String = "04:12:45",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String, // Emoji representation
    val rarity: String = "Common", // Common, Rare, Epic, Legendary
    val category: String = "Study", // Study, Quiz, Community, Showcase, Leaderboard, Streak, Squads, Special Events
    val xpReward: Int = 100,
    val coinReward: Int = 50,
    val progress: Int = 0,
    val target: Int = 1,
    val completed: Boolean = false,
    val claimed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val earned: Boolean = false,
    val earnedAt: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val activityId: String,
    val uid: String = "",
    val title: String,
    val type: String, // Study, Reading, Workout, Meditation, Running, Custom
    val duration: Int, // duration in seconds
    val xpEarned: Int,
    val pointsEarned: Int,
    val startTime: Long,
    val endTime: Long,
    val completed: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pomodoro_state")
data class PomodoroStateEntity(
    @PrimaryKey val id: Int = 1,
    val durationSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val activityType: String = "Study",
    val lastTickTime: Long = 0L,
    val originalDurationSeconds: Int = 25 * 60,
    val skipBreak: Boolean = false
)

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntryEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val username: String,
    val className: String,
    val section: String,
    val profileImage: String,
    val points: Int,
    val xp: Int,
    val level: Int,
    val streak: Int,
    val weeklyPoints: Int,
    val monthlyPoints: Int,
    val todayPoints: Int,
    val activitiesCompleted: Int,
    val totalStudyTime: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_messages")
data class RoomMessageEntity(
    @PrimaryKey val messageId: String,
    val channelId: String,
    val senderUid: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val reactions: String = "",
    val readBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "showcase_posts")
data class RoomShowcasePostEntity(
    @PrimaryKey val postId: String,
    val authorUid: String,
    val authorName: String,
    val authorUsername: String,
    val title: String,
    val description: String,
    val category: String,
    val fileUrl: String,
    val likesCount: Int = 0,
    val likedBy: String = "",
    val commentsCount: Int = 0,
    val savedBy: String = "",
    val reportedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_squads")
data class RoomSquadEntity(
    @PrimaryKey val squadId: String,
    val name: String,
    val description: String,
    val inviteCode: String,
    val creatorUid: String,
    val memberUids: String,
    val points: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val weeklyMissionsCompleted: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_feed")
data class RoomFeedItemEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val name: String,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_notifications")
data class RoomNotificationEntity(
    @PrimaryKey val id: String,
    val recipientUid: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val rarity: String, // Common, Rare, Epic, Legendary, Animated, Seasonal
    val unlocked: Boolean = false,
    val unlockedAt: Long = 0L,
    val category: String = "General"
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val challengeId: String,
    val title: String,
    val description: String,
    val xpReward: Int = 0,
    val coinReward: Int = 0,
    val gemReward: Int = 0,
    val progress: Int = 0,
    val targetValue: Int = 1,
    val type: String = "daily", // daily, weekly, monthly, seasonal
    val completed: Boolean = false,
    val claimed: Boolean = false,
    val expiresAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_login_rewards")
data class DailyLoginRewardEntity(
    @PrimaryKey val day: Int, // 1 to 30
    val title: String,
    val coinReward: Int,
    val gemReward: Int,
    val xpReward: Int,
    val claimed: Boolean = false,
    val rewardType: String = "daily" // daily, 7day, 30day, mystery, weekly, monthly
)

@Entity(tableName = "shop_items")
data class ShopItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // avatar_frame, profile_theme, chat_bubble, badge_frame, profile_bg, decoration
    val cost: Int, // Coins only
    val purchased: Boolean = false,
    val icon: String = ""
)

@Entity(tableName = "ai_cache")
data class AiCacheEntity(
    @PrimaryKey val cacheKey: String,
    val cacheValue: String,
    val updatedAt: Long = System.currentTimeMillis()
)

