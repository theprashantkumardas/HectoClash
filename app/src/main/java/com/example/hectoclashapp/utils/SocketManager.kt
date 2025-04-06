package com.example.hectoclashapp.utils

import android.util.Log
import com.example.hectoclash.data.models.OnlineUserResponse
import com.example.hectoclash.utils.Constants.BASE_URL
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException

//object SocketManager {
//    private const val SERVER_URL = BASE_URL // Change this
//
//    var socket: Socket? = null
//
//    fun connect(userId: String) {
//        try {
//            socket = IO.socket(SERVER_URL)
//            socket?.connect()
//
//            socket?.on(Socket.EVENT_CONNECT) {
//                Log.d("Socket", "Connected to server")
//                socket?.emit("user-online", userId) // Send user ID to server
//            }
//
//            socket?.on("update-online-users") { args ->
//                val onlineUsers = args[0] as? List<String> ?: emptyList()
//                Log.d("Socket", "Online Users: $onlineUsers")
//            }
//
//            socket?.on(Socket.EVENT_DISCONNECT) {
//                Log.d("Socket", "Disconnected from server")
//            }
//        } catch (e: Exception) {
//            Log.e("Socket", "Error: ${e.message}")
//        }
//    }
//
//    fun disconnect() {
//        socket?.disconnect()
//    }
//}

object SocketManager {
    private const val TAG = "SocketManager"

    var socket: Socket? = null
    private var isConnected = false

    // Callbacks
    private var onlineUsersCallback: ((List<OnlineUserResponse>) -> Unit)? = null // Changed type
    fun initialize() {
        try {
            val options = IO.Options()
            socket = IO.socket(Constants.BASE_URL, options)
            setupSocketListeners()
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Error initializing socket: ${e.message}")
        }
    }

    fun connect(userId: String) {
        if (socket == null) {
            initialize()
        }

        socket?.connect()

        // Once connected, emit the user-online event with userId
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected")
            isConnected = true
            socket?.emit("user-online", userId)

            // Start heartbeat to keep connection alive
            startHeartbeat(userId)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        isConnected = false
        Log.d(TAG, "Socket disconnected")
    }

    private fun setupSocketListeners() {
        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket disconnected")
            isConnected = false
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Connection error: ${args[0]}")
        }

        // Listen for online users updates
        socket?.on("update-online-users") { args ->
            Log.d(TAG, "Received update-online-users event")
            if (args.isNotEmpty() && args[0] != null) { // Check arg exists
                try {
                    // Assuming args[0] is a JSONArray represented as a String or JSONObject
                    val jsonData = args[0].toString() // Get the JSON string
                    Log.d(TAG, "Raw data: $jsonData") // Log raw data

                    // Use Gson to parse the JSON array string into a List<OnlineUserResponse>
                    val listType = object : TypeToken<List<OnlineUserResponse>>() {}.type
                    val userList: List<OnlineUserResponse> = Gson().fromJson(jsonData, listType)

                    Log.d(TAG, "Parsed online users: $userList")
                    // Execute the callback on the main thread if needed for UI updates
                    CoroutineScope(Dispatchers.Main).launch {
                        onlineUsersCallback?.invoke(userList)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing online users JSON: ${e.message}", e)
                }
            } else {
                Log.w(TAG, "Received empty or null data for update-online-users")
                // Handle case where list is empty (e.g., last user disconnected)
                CoroutineScope(Dispatchers.Main).launch {
                    onlineUsersCallback?.invoke(emptyList())
                }
            }
        }
    }

    // Change setter method type
    fun setOnlineUsersCallback(callback: (List<OnlineUserResponse>) -> Unit) { // Changed type
        onlineUsersCallback = callback
    }

    private fun startHeartbeat(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            while (isConnected) {
                try {
                    kotlinx.coroutines.delay(30000) // 30 seconds
                    if (isConnected) {
                        socket?.emit("heartbeat", userId)
                        Log.d(TAG, "Heartbeat sent")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat error: ${e.message}")
                }
            }
        }
    }
}