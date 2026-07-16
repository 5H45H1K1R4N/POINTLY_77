package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "syllabus_questions")
data class SyllabusQuestionEntity(
    @PrimaryKey val id: String,
    val board: String,
    @ColumnInfo(name = "class") val className: String,
    val subject: String,
    val chapter: String,
    val topic: String,
    val difficulty: String,
    val question: String,
    val options: String, // Separated by "||"
    val correctAnswer: String,
    val correctAnswerIndex: Int,
    val explanation: String,
    val points: Int,
    val xp: Int,
    val estimatedTime: Int,
    val isBookmarked: Boolean = false,
    val wrongAttemptsCount: Int = 0,
    val skippedCount: Int = 0
)

@Entity(tableName = "syllabus_attempts")
data class SyllabusQuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questionId: String,
    val subject: String,
    val chapter: String,
    val topic: String,
    val isCorrect: Boolean,
    val timeSpentSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)
