package com.example.data.model

import com.example.data.database.SyllabusQuestionEntity

data class SyllabusQuestion(
    val id: String = "",
    val board: String = "CBSE",
    val `class`: String = "Class 8",
    val subject: String = "Physics",
    val chapter: String = "Force and Pressure",
    val topic: String = "Contact Forces",
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val correctAnswerIndex: Int = 0,
    val explanation: String = "",
    val points: Int = 10,
    val xp: Int = 50,
    val estimatedTime: Int = 30, // in seconds
    val isBookmarked: Boolean = false,
    val wrongAttemptsCount: Int = 0,
    val skippedCount: Int = 0
) {
    fun toEntity(): SyllabusQuestionEntity {
        return SyllabusQuestionEntity(
            id = id,
            board = board,
            className = `class`,
            subject = subject,
            chapter = chapter,
            topic = topic,
            difficulty = difficulty,
            question = question,
            options = options.joinToString("||"),
            correctAnswer = correctAnswer,
            correctAnswerIndex = correctAnswerIndex,
            explanation = explanation,
            points = points,
            xp = xp,
            estimatedTime = estimatedTime,
            isBookmarked = isBookmarked,
            wrongAttemptsCount = wrongAttemptsCount,
            skippedCount = skippedCount
        )
    }

    companion object {
        fun fromEntity(entity: SyllabusQuestionEntity): SyllabusQuestion {
            return SyllabusQuestion(
                id = entity.id,
                board = entity.board,
                `class` = entity.className,
                subject = entity.subject,
                chapter = entity.chapter,
                topic = entity.topic,
                difficulty = entity.difficulty,
                question = entity.question,
                options = entity.options.split("||"),
                correctAnswer = entity.correctAnswer,
                correctAnswerIndex = entity.correctAnswerIndex,
                explanation = entity.explanation,
                points = entity.points,
                xp = entity.xp,
                estimatedTime = entity.estimatedTime,
                isBookmarked = entity.isBookmarked,
                wrongAttemptsCount = entity.wrongAttemptsCount,
                skippedCount = entity.skippedCount
            )
        }
    }
}
