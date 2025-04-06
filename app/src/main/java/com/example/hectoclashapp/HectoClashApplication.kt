package com.example.hectoclashapp

import android.app.Application

class HectoClashApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Socket Manager
        SocketManager.initialize()
    }
}