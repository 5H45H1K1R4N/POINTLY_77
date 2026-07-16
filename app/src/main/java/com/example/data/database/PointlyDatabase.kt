package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        StudyMissionEntity::class,
        AchievementEntity::class,
        ActivityEntity::class,
        PomodoroStateEntity::class,
        LeaderboardEntryEntity::class,
        RoomMessageEntity::class,
        RoomShowcasePostEntity::class,
        RoomSquadEntity::class,
        RoomFeedItemEntity::class,
        RoomNotificationEntity::class,
        SyllabusQuestionEntity::class,
        SyllabusQuizAttemptEntity::class,
        BadgeEntity::class,
        ChallengeEntity::class,
        DailyLoginRewardEntity::class,
        ShopItemEntity::class,
        AiCacheEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class PointlyDatabase : RoomDatabase() {
    abstract fun pointlyDao(): PointlyDao

    companion object {
        @Volatile
        private var INSTANCE: PointlyDatabase? = null

        fun getDatabase(context: Context): PointlyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PointlyDatabase::class.java,
                    "pointly_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
