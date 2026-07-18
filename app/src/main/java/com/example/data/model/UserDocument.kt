package com.example.data.model

import com.google.firebase.firestore.Exclude

data class UserDocument(
    val uid: String = "",
    val name: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val generatedEmail: String = "",
    val className: String = "",
    val `class`: String = "",
    val section: String = "",
    val school: String = "",
    val profileImage: String = "",
    val points: Int = 0,
    val xp: Int = 0,
    val coins: Int = 0,
    val gems: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val weeklyPoints: Int = 0,
    val monthlyPoints: Int = 0,
    val todayPoints: Int = 0,
    val activitiesCompleted: Int = 0,
    val activitiesCount: Int = 0,
    val totalStudyTime: Int = 0, // In minutes
    val studyMinutes: Int = 0,
    val quizAccuracy: Int = 0,
    val quizStats: Map<String, Int> = emptyMap(),
    val isTeacher: Boolean = false,
    val employeeId: String = "",
    val subjects: String = "",
    val classesAssigned: String = "",
    val sectionsAssigned: String = "",
    val isAdmin: Boolean = false,
    val adminId: String = "",
    val organizationId: String = "",
    val permissions: String = "",
    val adminRole: String = "",
    val createdAt: Any? = System.currentTimeMillis(),
    val updatedAt: Any? = System.currentTimeMillis()
) {
    @get:Exclude
    val createdAtLong: Long
        get() = convertToLong(createdAt)

    @get:Exclude
    val updatedAtLong: Long
        get() = convertToLong(updatedAt)

    private fun convertToLong(obj: Any?): Long {
        if (obj == null) return System.currentTimeMillis()
        return when (obj) {
            is Long -> obj
            is Int -> obj.toLong()
            is Double -> obj.toLong()
            is Float -> obj.toLong()
            is com.google.firebase.Timestamp -> obj.toDate().time
            is java.util.Date -> obj.time
            is Map<*, *> -> {
                val secObj = obj["seconds"] ?: obj["_seconds"]
                if (secObj is Number) {
                    secObj.toLong() * 1000L
                } else {
                    System.currentTimeMillis()
                }
            }
            else -> {
                val str = obj.toString()
                str.toLongOrNull() ?: System.currentTimeMillis()
            }
        }
    }
}

