package com.example.data.repository

import com.example.data.database.SyllabusQuestionEntity
import com.example.data.database.SyllabusQuizAttemptEntity
import com.example.data.model.*
import kotlin.random.Random

object SyllabusEngine {

    fun getSeededQuestions(): List<SyllabusQuestion> {
        val list = mutableListOf<SyllabusQuestion>()

        // Class 6 Questions
        list.add(
            SyllabusQuestion(
                id = "c6_p1", board = "CBSE", `class` = "Class 6", subject = "Physics",
                chapter = "Motion and Measurement", topic = "Types of Motion", difficulty = "Easy",
                question = "What type of motion does a falling apple exhibit?",
                options = listOf("Rectilinear motion", "Circular motion", "Periodic motion", "Rotational motion"),
                correctAnswer = "Rectilinear motion", correctAnswerIndex = 0,
                explanation = "An apple falling straight down from a tree moves along a straight line, which is rectilinear motion.",
                points = 10, xp = 50, estimatedTime = 15
            )
        )
        list.add(
            SyllabusQuestion(
                id = "c6_b1", board = "CBSE", `class` = "Class 6", subject = "Biology",
                chapter = "Food and its Sources", topic = "Herbivores", difficulty = "Easy",
                question = "Which of the following animals is a pure herbivore?",
                options = listOf("Tiger", "Cow", "Crow", "Human"),
                correctAnswer = "Cow", correctAnswerIndex = 1,
                explanation = "Cows feed entirely on plants and grass, making them herbivores.",
                points = 10, xp = 50, estimatedTime = 15
            )
        )

        // Class 7 Questions
        list.add(
            SyllabusQuestion(
                id = "c7_c1", board = "CBSE", `class` = "Class 7", subject = "Chemistry",
                chapter = "Acids, Bases and Salts", topic = "Indicators", difficulty = "Medium",
                question = "What color does blue litmus paper turn when dipped in an acidic solution?",
                options = listOf("Remains Blue", "Turns Red", "Turns Green", "Turns Yellow"),
                correctAnswer = "Turns Red", correctAnswerIndex = 1,
                explanation = "Acidic solutions turn blue litmus paper red, while basic solutions turn red litmus paper blue.",
                points = 15, xp = 75, estimatedTime = 20
            )
        )
        list.add(
            SyllabusQuestion(
                id = "c7_m1", board = "CBSE", `class` = "Class 7", subject = "Mathematics",
                chapter = "Integers", topic = "Properties of Integers", difficulty = "Medium",
                question = "What is the product of a negative integer and a positive integer?",
                options = listOf("Always positive", "Always negative", "Zero", "Depends on the values"),
                correctAnswer = "Always negative", correctAnswerIndex = 1,
                explanation = "The product of integers with opposite signs is always negative. (-a) * (+b) = -ab.",
                points = 15, xp = 75, estimatedTime = 20
            )
        )

        // Class 8 Questions
        list.add(
            SyllabusQuestion(
                id = "c8_p1", board = "CBSE", `class` = "Class 8", subject = "Physics",
                chapter = "Force and Pressure", topic = "Friction", difficulty = "Medium",
                question = "Which type of friction is the smallest in magnitude for the same set of surfaces?",
                options = listOf("Static friction", "Sliding friction", "Rolling friction", "Fluid friction"),
                correctAnswer = "Rolling friction", correctAnswerIndex = 2,
                explanation = "Rolling friction is always significantly smaller than sliding or static friction because wheels reduce the contact resistance.",
                points = 20, xp = 100, estimatedTime = 25
            )
        )
        list.add(
            SyllabusQuestion(
                id = "c8_p2", board = "CBSE", `class` = "Class 8", subject = "Physics",
                chapter = "Force and Pressure", topic = "Atmospheric Pressure", difficulty = "Hard",
                question = "As we move to higher altitudes, what happens to atmospheric pressure?",
                options = listOf("It increases", "It decreases", "It remains constant", "It fluctuates rapidly"),
                correctAnswer = "It decreases", correctAnswerIndex = 1,
                explanation = "Atmospheric pressure decreases at higher altitudes because the density of the air column above decreases.",
                points = 25, xp = 120, estimatedTime = 30
            )
        )
        list.add(
            SyllabusQuestion(
                id = "c8_b1", board = "CBSE", `class` = "Class 8", subject = "Biology",
                chapter = "Crop Production", topic = "Irrigation", difficulty = "Medium",
                question = "Which modern method of irrigation is highly efficient for water-scarce regions?",
                options = listOf("Drip System", "Sprinkler System", "Rahat System", "Chain Pump"),
                correctAnswer = "Drip System", correctAnswerIndex = 0,
                explanation = "The drip system delivers water directly drop-by-drop near the roots of the plants, preventing water wastage.",
                points = 20, xp = 100, estimatedTime = 25
            )
        )

        // Class 9 Questions
        list.add(
            SyllabusQuestion(
                id = "c9_p1", board = "CBSE", `class` = "Class 9", subject = "Physics",
                chapter = "Laws of Motion", topic = "Inertia", difficulty = "Hard",
                question = "Which law of Newton defines the concept of inertia?",
                options = listOf("Newton's First Law", "Newton's Second Law", "Newton's Third Law", "Law of Gravitation"),
                correctAnswer = "Newton's First Law", correctAnswerIndex = 0,
                explanation = "Newton's First Law of Motion, also known as the Law of Inertia, states that an object remains in its state of rest or uniform motion unless acted upon by an external force.",
                points = 25, xp = 120, estimatedTime = 30
            )
        )
        list.add(
            SyllabusQuestion(
                id = "c9_c1", board = "CBSE", `class` = "Class 9", subject = "Chemistry",
                chapter = "Matter in Our Surroundings", topic = "Evaporation", difficulty = "Medium",
                question = "What effect does increasing the surface area have on the rate of evaporation?",
                options = listOf("Decreases rate", "No change", "Increases rate", "First decreases then increases"),
                correctAnswer = "Increases rate", correctAnswerIndex = 2,
                explanation = "Evaporation is a surface phenomenon. Increasing the surface area increases the molecules exposed, accelerating evaporation.",
                points = 20, xp = 100, estimatedTime = 25
            )
        )
        list.add(
            SyllabusQuestion(
                id = "c9_m1", board = "CBSE", `class` = "Class 9", subject = "Mathematics",
                chapter = "Polynomials", topic = "Identities", difficulty = "Hard",
                question = "What is the value of (x + y)^3?",
                options = listOf("x^3 + y^3 + 3xy", "x^3 + y^3 + 3x^2y + 3xy^2", "x^3 + y^3", "x^3 - y^3"),
                correctAnswer = "x^3 + y^3 + 3x^2y + 3xy^2", correctAnswerIndex = 1,
                explanation = "Using algebraic identities, (x+y)^3 expands to x^3 + y^3 + 3xy(x+y) = x^3 + y^3 + 3x^2y + 3xy^2.",
                points = 25, xp = 120, estimatedTime = 35
            )
        )

        // Class 10 Questions
        list.add(
            SyllabusQuestion(
                id = "c10_p1", board = "CBSE", `class` = "Class 10", subject = "Physics",
                chapter = "Electricity", topic = "Ohm's Law", difficulty = "Hard",
                question = "If the resistance of a conductor is doubled while voltage remains constant, what happens to the current?",
                options = listOf("Doubles", "Halves", "Quadruples", "Remains same"),
                correctAnswer = "Halves", correctAnswerIndex = 1,
                explanation = "By Ohm's Law (I = V/R), current is inversely proportional to resistance. If resistance is doubled, current becomes half.",
                points = 25, xp = 130, estimatedTime = 30
            )
        )
        list.add(
            SyllabusQuestion(
                id = "c10_c1", board = "CBSE", `class` = "Class 10", subject = "Chemistry",
                chapter = "Chemical Reactions", topic = "Oxidation", difficulty = "Medium",
                question = "What is the chemical formula of rust?",
                options = listOf("Fe2O3", "Fe3O4", "Fe2O3 . xH2O", "Fe(OH)3"),
                correctAnswer = "Fe2O3 . xH2O", correctAnswerIndex = 2,
                explanation = "Rusting is a complex oxidation reaction forming hydrated ferric oxide, Fe2O3 . xH2O.",
                points = 20, xp = 100, estimatedTime = 25
            )
        )
        list.add(
            SyllabusQuestion(
                id = "c10_b1", board = "CBSE", `class` = "Class 10", subject = "Biology",
                chapter = "Life Processes", topic = "Photosynthesis", difficulty = "Medium",
                question = "Which pigment absorbs sunlight during the process of photosynthesis?",
                options = listOf("Carotenoids", "Chlorophyll", "Xanthophyll", "Anthocyanin"),
                correctAnswer = "Chlorophyll", correctAnswerIndex = 1,
                explanation = "Chlorophyll, found inside chloroplasts, is the green pigment responsible for capturing radiant light energy.",
                points = 20, xp = 100, estimatedTime = 20
            )
        )

        // Generate additional 20 diverse filler questions to reach a rich set
        val subjects = listOf("Physics", "Chemistry", "Biology", "Mathematics", "History")
        val classes = listOf("Class 6", "Class 7", "Class 8", "Class 9", "Class 10")
        val difficulties = listOf("Easy", "Medium", "Hard")

        for (i in 1..25) {
            val cls = classes[i % classes.size]
            val sub = subjects[i % subjects.size]
            val diff = difficulties[i % difficulties.size]
            val points = when (diff) {
                "Easy" -> 10
                "Medium" -> 15
                else -> 20
            }
            val xpVal = points * 5

            list.add(
                SyllabusQuestion(
                    id = "gen_${cls.lowercase().replace(" ", "")}_${sub.lowercase()}_$i",
                    board = "CBSE",
                    `class` = cls,
                    subject = sub,
                    chapter = "$sub Basics $i",
                    topic = "Core Concept $i",
                    difficulty = diff,
                    question = "A critical concept question on $sub (Topic $i) designed for $cls student.",
                    options = listOf("Option A - Accurate Choice", "Option B - Distractor", "Option C - Plausible Alt", "Option D - Basic Alt"),
                    correctAnswer = "Option A - Accurate Choice",
                    correctAnswerIndex = 0,
                    explanation = "This is the detailed explanation for $sub (Topic $i) to master this subject easily.",
                    points = points,
                    xp = xpVal,
                    estimatedTime = 15 + i
                )
            )
        }

        return list
    }

    fun selectSyllabusQuiz(
        allQuestions: List<SyllabusQuestion>,
        attempts: List<SyllabusQuizAttemptEntity>,
        className: String,
        mode: QuizMode,
        subjectFilter: String? = null,
        chapterFilter: String? = null
    ): List<SyllabusQuestion> {
        // Filter by student's class
        var filtered = allQuestions.filter { it.`class`.equals(className, ignoreCase = true) }

        // Filter out recently solved questions (solved successfully in the last 12 hours)
        val twelveHoursAgo = System.currentTimeMillis() - (12 * 60 * 60 * 1000)
        val recentlySolvedIds = attempts
            .filter { it.timestamp > twelveHoursAgo && it.isCorrect }
            .map { it.questionId }
            .toSet()

        // Exclude recently solved from the pool if we have enough questions left
        val unattempted = filtered.filter { it.id !in recentlySolvedIds }
        if (unattempted.size >= 5) {
            filtered = unattempted
        }

        // Apply Mode-Specific Filters
        when (mode) {
            QuizMode.BOOKMARKED -> {
                return allQuestions.filter { it.isBookmarked }
            }
            QuizMode.WRONG_ANSWERS_PRACTICE -> {
                return allQuestions.filter { it.wrongAttemptsCount > 0 }
            }
            QuizMode.CHAPTER_WISE -> {
                if (chapterFilter != null) {
                    filtered = filtered.filter { it.chapter.equals(chapterFilter, ignoreCase = true) }
                }
            }
            QuizMode.SUBJECT_WISE -> {
                if (subjectFilter != null) {
                    filtered = filtered.filter { it.subject.equals(subjectFilter, ignoreCase = true) }
                }
            }
            QuizMode.RANDOM_REVISION -> {
                // Prioritize weak chapters (accuracy < 60%) or previously wrong
                val weakChapters = getWeakChapters(attempts)
                val weakQuestions = filtered.filter { it.chapter in weakChapters || it.wrongAttemptsCount > 0 }
                if (weakQuestions.size >= 3) {
                    filtered = weakQuestions
                }
            }
            else -> {}
        }

        // Determine size of quiz
        val targetSize = when (mode) {
            QuizMode.DAILY_QUIZ -> 5
            QuizMode.PRACTICE_MODE -> 10
            QuizMode.CHAPTER_WISE -> 5
            QuizMode.SUBJECT_WISE -> 10
            QuizMode.WEEKLY_TEST -> 15
            QuizMode.MOCK_EXAM -> 20
            QuizMode.RANDOM_REVISION -> 5
            else -> 10
        }

        // Balanced and Diverse Selection:
        // Try to mix chapters
        val chapterGroups = filtered.groupBy { it.chapter }
        val finalSelection = mutableListOf<SyllabusQuestion>()

        if (chapterGroups.isNotEmpty()) {
            val keys = chapterGroups.keys.toList()
            var index = 0
            val chapterUsedCounts = mutableMapOf<String, Int>()

            while (finalSelection.size < targetSize && finalSelection.size < filtered.size) {
                val ch = keys[index % keys.size]
                val chQuestions = chapterGroups[ch] ?: emptyList()
                val usedInChapter = chapterUsedCounts[ch] ?: 0

                val available = chQuestions.filter { it !in finalSelection }
                if (available.isNotEmpty()) {
                    // Balancing Difficulty (mix Easy, Medium, Hard)
                    val nextQuestion = available.shuffled().first()
                    finalSelection.add(nextQuestion)
                    chapterUsedCounts[ch] = usedInChapter + 1
                }
                index++
            }
        } else {
            finalSelection.addAll(filtered.shuffled().take(targetSize))
        }

        return finalSelection.take(targetSize)
    }

    private fun getWeakChapters(attempts: List<SyllabusQuizAttemptEntity>): Set<String> {
        val groups = attempts.groupBy { it.chapter }
        val weak = mutableSetOf<String>()
        groups.forEach { (chapter, list) ->
            val correct = list.count { it.isCorrect }
            val total = list.size
            if (total > 0) {
                val accuracy = correct.toFloat() / total
                if (accuracy < 0.6f) {
                    weak.add(chapter)
                }
            }
        }
        return weak
    }

    fun calculateAnalytics(
        allQuestions: List<SyllabusQuestion>,
        attempts: List<SyllabusQuizAttemptEntity>
    ): SyllabusAnalytics {
        if (attempts.isEmpty()) return SyllabusAnalytics()

        val subjectGroups = attempts.groupBy { it.subject }
        val subjectAnalList = mutableListOf<SubjectAnalytics>()
        var bestSubject = "None"
        var bestAccuracy = -1f
        var worstSubject = "None"
        var worstAccuracy = 2f

        subjectGroups.forEach { (subject, list) ->
            val total = list.size
            val correct = list.count { it.isCorrect }
            val accuracy = if (total > 0) correct.toFloat() / total else 0f
            val avgTime = if (total > 0) list.map { it.timeSpentSeconds }.average().toFloat() else 0f
            subjectAnalList.add(SubjectAnalytics(subject, total, correct, accuracy, avgTime))

            if (accuracy > bestAccuracy) {
                bestAccuracy = accuracy
                bestSubject = subject
            }
            if (accuracy < worstAccuracy) {
                worstAccuracy = accuracy
                worstSubject = subject
            }
        }

        val chapterGroups = attempts.groupBy { it.chapter }
        val chapterAnalList = chapterGroups.map { (chapter, list) ->
            val total = list.size
            val correct = list.count { it.isCorrect }
            val accuracy = if (total > 0) correct.toFloat() / total else 0f
            ChapterAnalytics(chapter, list.firstOrNull()?.subject ?: "", total, correct, accuracy)
        }

        val topicGroups = attempts.groupBy { it.topic }
        val weakTopics = mutableListOf<String>()
        val strongTopics = mutableListOf<String>()

        topicGroups.forEach { (topic, list) ->
            val total = list.size
            val correct = list.count { it.isCorrect }
            val accuracy = if (total > 0) correct.toFloat() / total else 0f
            if (accuracy < 0.6f && total >= 1) {
                weakTopics.add(topic)
            } else if (accuracy >= 0.8f && total >= 1) {
                strongTopics.add(topic)
            }
        }

        val totalQuestionsSolved = attempts.distinctBy { it.questionId }.size
        val overallAccuracy = attempts.count { it.isCorrect }.toFloat() / attempts.size
        val avgSolvingTime = attempts.map { it.timeSpentSeconds }.average().toFloat()

        return SyllabusAnalytics(
            subjectAnalytics = subjectAnalList,
            chapterAnalytics = chapterAnalList,
            weakTopics = weakTopics.take(5),
            strongTopics = strongTopics.take(5),
            averageTimeSeconds = avgSolvingTime,
            bestSubject = bestSubject,
            worstSubject = worstSubject,
            totalQuestionsSolved = totalQuestionsSolved,
            overallAccuracy = overallAccuracy
        )
    }
}
