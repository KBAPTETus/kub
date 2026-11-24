package com.example

import android.app.Application
import com.example.data.local.FinDirectorDatabase

class FinDirectorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize encrypted Room database with SQLCipher
        FinDirectorDatabase.getInstance(this)
    }
}
