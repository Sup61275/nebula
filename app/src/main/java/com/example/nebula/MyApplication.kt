package com.example.nebula

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // In your Application class or MainActivity
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}