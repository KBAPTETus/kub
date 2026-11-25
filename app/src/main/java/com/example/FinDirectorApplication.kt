package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.FinDirectorDatabase
import com.yandex.mobile.ads.common.YandexAds

class FinDirectorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize encrypted Room database with SQLCipher
        FinDirectorDatabase.getInstance(this)
        // Initialize Yandex Mobile Ads SDK
        try {
            YandexAds.initialize(this) {
                Log.d("FinDirectorApp", "Yandex Mobile Ads SDK initialized")
            }
        } catch (t: Throwable) {
            Log.w("FinDirectorApp", "Yandex Mobile Ads SDK init skipped: ${t.message}")
        }
    }
}
