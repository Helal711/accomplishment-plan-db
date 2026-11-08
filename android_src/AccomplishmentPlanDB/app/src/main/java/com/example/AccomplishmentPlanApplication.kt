package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class AccomplishmentPlanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupUncaughtExceptionHandler()
        ensureFirebaseInitialized()
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun ensureFirebaseInitialized() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    initFallbackFirebase()
                } else {
                    Log.i(TAG, "FirebaseApp initialized automatically from google-services.json")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Standard Firebase initialization encountered an issue: ${e.message}", e)
            initFallbackFirebase()
        }
    }

    private fun initFallbackFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:724771876461:android:7dbe1704c1c78206645964")
                    .setApiKey("AIzaSyCb46rloSBnJqmMuDl2p5cNHKLxJdMHXIA")
                    .setProjectId("accomplishment-plan-db")
                    .setStorageBucket("accomplishment-plan-db.firebasestorage.app")
                    .setGcmSenderId("724771876461")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.i(TAG, "FirebaseApp successfully initialized with fallback configuration")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback Firebase initialization error: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "AccomplishmentPlanApp"
    }
}
