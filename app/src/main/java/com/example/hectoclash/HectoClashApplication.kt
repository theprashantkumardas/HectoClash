package com.example.hectoclash

import android.app.Application
import com.example.hectoclash.utils.SocketManager

class HectoClashApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Socket Manager
        SocketManager.initialize()
    }
}