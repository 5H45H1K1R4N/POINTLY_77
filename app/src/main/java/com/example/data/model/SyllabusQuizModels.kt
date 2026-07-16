package com.example.data.model

enum class QuizMode {
    DAILY_QUIZ,
    PRACTICE_MODE,
    CHAPTER_WISE,
    SUBJECT_WISE,
    WEEKLY_TEST,
    MOCK_EXAM,
    RANDOM_REVISION,
    BOOKMARKED,
    WRONG_ANSWERS_PRACTICE
}

data class SubjectAnalytics(
    val subject: String,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val accuracy: Float,
    val averageTimeSeconds: Float
)

data class ChapterAnalytics(
    val chapter: String,
    val subject: String,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val accuracy: Float
)

data class TopicAnalytics(
    val topic: String,
    val chapter: String,
    val subject: String,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val accuracy: Float
)

data class SyllabusAnalytics(
    val subjectAnalytics: List<SubjectAnalytics> = emptyList(),
    val chapterAnalytics: List<ChapterAnalytics> = emptyList(),
    val weakTopics: List<String> = emptyList(),
    val strongTopics: List<String> = emptyList(),
    val averageTimeSeconds: Float = 0f,
    val bestSubject: String = "None",
    val worstSubject: String = "None",
    val totalQuestionsSolved: Int = 0,
    val overallAccuracy: Float = 0f
)
