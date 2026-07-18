package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class PointlyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyAItra9SH3n6JoD-fX9_iBargRh7_eoej4")
                .setApplicationId("1:887869103231:android:be8c2fdbee73bfc2ecf34a")
                .setProjectId("pointly77")
                .setDatabaseUrl("https://pointly77-default-rtdb.firebaseio.com")
                .setStorageBucket("pointly77.firebasestorage.app")
                .build()

            val apps = FirebaseApp.getApps(this)
            if (apps.isNotEmpty()) {
                Log.d("PointlyApplication", "FirebaseApp default instance already exists. Re-initializing to ensure correct options.")
                try {
                    val existingApp = FirebaseApp.getInstance()
                    existingApp.delete()
                } catch (e: Exception) {
                    Log.e("PointlyApplication", "Error deleting existing FirebaseApp instance", e)
                }
            }

            FirebaseApp.initializeApp(this, options)
            Log.d("PointlyApplication", "FirebaseApp initialized successfully with correct programmatic options.")
        } catch (e: Exception) {
            Log.e("PointlyApplication", "FirebaseApp initialization failed", e)
        }
    }
}
