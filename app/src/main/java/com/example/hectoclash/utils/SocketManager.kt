package com.example.hectoclash.utils

import android.util.Log
import com.example.hectoclash.data.models.ChallengeFailedData
import com.example.hectoclash.data.models.ChallengeOverData
import com.example.hectoclash.data.models.ChallengeRejectedData
import com.example.hectoclash.data.models.ChallengeStartData
import com.example.hectoclash.data.models.ChallengeUserData
import com.example.hectoclash.data.models.FriendRemovedData
import com.example.hectoclash.data.models.FriendRequestAcceptedData
import com.example.hectoclash.data.models.FriendRequestReceivedData
import com.example.hectoclash.data.models.FriendRequestRejectedData
import com.example.hectoclash.data.models.GameOverData
import com.example.hectoclash.data.models.GameStartData
import com.example.hectoclash.data.models.GameStartFailedData
import com.example.hectoclash.data.models.NewRoundData
import com.example.hectoclash.data.models.OnlineUserResponse
import com.example.hectoclash.data.models.ReceiveChallengeData
import com.example.hectoclash.data.models.RespondChallengeData
import com.example.hectoclash.data.models.RoundOverData
import com.example.hectoclash.data.models.SolutionInvalidData
import com.example.hectoclash.data.models.SolutionResultData
import com.example.hectoclash.data.models.SubmitSolutionData
import com.example.hectoclash.utils.Constants.BASE_URL
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException


object SocketManager {
    private const val TAG = "SocketManager"
    private val gson = Gson() // Reusable Gson instance

    var socket: Socket? = null
    private var isConnected = false
    private var currentUserId: String? = null // Keep track of the logged-in user

    // --- Flows for Broadcasting Events ---

    // Flow for Online Users List
    private val _onlineUsersFlow = MutableSharedFlow<List<OnlineUserResponse>>(replay = 1)
    val onlineUsersFlow = _onlineUsersFlow.asSharedFlow()

    // Flow for Incoming Challenges
    private val _challengeReceivedFlow = MutableSharedFlow<ReceiveChallengeData>(replay = 0) // No need to replay challenges
    val challengeReceivedFlow = _challengeReceivedFlow.asSharedFlow()

    // Flow for Challenge Rejections
    private val _challengeRejectedFlow = MutableSharedFlow<ChallengeRejectedData>(replay = 0)
    val challengeRejectedFlow = _challengeRejectedFlow.asSharedFlow()

    // Flow for Game Starting
    private val _gameStartFlow = MutableSharedFlow<GameStartData>(replay = 0)
    val gameStartFlow = _gameStartFlow.asSharedFlow()

    // Flow for Game Ending
    private val _gameOverFlow = MutableSharedFlow<GameOverData>(replay = 0)
    val gameOverFlow = _gameOverFlow.asSharedFlow()

    // Flow for Invalid Solution feedback
    private val _solutionInvalidFlow = MutableSharedFlow<SolutionInvalidData>(replay = 0)
    val solutionInvalidFlow = _solutionInvalidFlow.asSharedFlow()

    // Optional: Flows for failed actions
    private val _challengeFailedFlow = MutableSharedFlow<ChallengeFailedData>(replay = 0)
    val challengeFailedFlow = _challengeFailedFlow.asSharedFlow()

    private val _gameStartFailedFlow = MutableSharedFlow<GameStartFailedData>(replay = 0)
    val gameStartFailedFlow = _gameStartFailedFlow.asSharedFlow()

    // --- NEW Friend Flows ---
    private val _friendRequestReceivedFlow = MutableSharedFlow<FriendRequestReceivedData>(replay = 0)
    val friendRequestReceivedFlow = _friendRequestReceivedFlow.asSharedFlow()

    private val _friendRequestAcceptedFlow = MutableSharedFlow<FriendRequestAcceptedData>(replay = 0)
    val friendRequestAcceptedFlow = _friendRequestAcceptedFlow.asSharedFlow()

    private val _friendRequestRejectedFlow = MutableSharedFlow<FriendRequestRejectedData>(replay = 0)
    val friendRequestRejectedFlow = _friendRequestRejectedFlow.asSharedFlow()

    private val _friendRemovedFlow = MutableSharedFlow<FriendRemovedData>(replay = 0)
    val friendRemovedFlow = _friendRemovedFlow.asSharedFlow()

    // --- NEW Flows for Multi-Round Game ---
    private val _challengeStartFlow = MutableSharedFlow<ChallengeStartData>(replay = 1) // <--- CHANGE replay to 1
    val challengeStartFlow = _challengeStartFlow.asSharedFlow()

    private val _newRoundFlow = MutableSharedFlow<NewRoundData>(replay = 0)
    val newRoundFlow = _newRoundFlow.asSharedFlow()

    private val _solutionResultFlow = MutableSharedFlow<SolutionResultData>(replay = 0)
    val solutionResultFlow = _solutionResultFlow.asSharedFlow()

    private val _roundOverFlow = MutableSharedFlow<RoundOverData>(replay = 0)
    val roundOverFlow = _roundOverFlow.asSharedFlow()

    private val _challengeOverFlow = MutableSharedFlow<ChallengeOverData>(replay = 0)
    val challengeOverFlow = _challengeOverFlow.asSharedFlow()
    // --- END NEW Flows ---

    fun initialize() {
        // Prevent re-initialization if socket already exists
        if (socket != null) return
        try {
            val options = IO.Options().apply {
                // Optional: Add query params if needed for auth, but AuthInterceptor is better
                // query = "token=your_token_here" // Example if needed, but we use headers
                reconnection = true // Enable auto-reconnection
                reconnectionAttempts = 5
                reconnectionDelay = 1000
            }

            Log.d(TAG, "Initializing Socket connection to $BASE_URL")
            socket = IO.socket(BASE_URL, options)
            setupSocketListeners()
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Error initializing socket: ${e.message}", e)
        }
    }

    fun connect(userId: String) {
        if (socket == null) {
            initialize()
        }
        currentUserId = userId // Store the user ID

        if (socket?.connected() == true) {
            Log.d(TAG, "Socket already connected. Emitting user-online.")
            socket?.emit("user-online", userId)
            startHeartbeat(userId) // Ensure heartbeat starts even if already connected
            return
        }

        socket?.connect() // Attempt connection

        // EVENT_CONNECT listener is now setup in setupSocketListeners()
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting socket manually.")
        socket?.disconnect()
        isConnected = false
        currentUserId = null
        // Clear replay caches if needed on manual disconnect
        _onlineUsersFlow.resetReplayCache()

    }

    private fun setupSocketListeners() {
        socket?.on(Socket.EVENT_CONNECT) {
            isConnected = true
            Log.i(TAG, "Socket connected successfully! Socket ID: ${socket?.id()}")
            currentUserId?.let { userId ->
                Log.d(TAG, "Emitting user-online for user: $userId")
                socket?.emit("user-online", userId)
                startHeartbeat(userId)
            } ?: Log.w(TAG, "User ID not set when socket connected.")
        }

        socket?.on(Socket.EVENT_DISCONNECT) { args ->
            isConnected = false
            val reason = args.getOrNull(0)?.toString() ?: "unknown"
            Log.w(TAG, "Socket disconnected. Reason: $reason")
            // Handle disconnection logic if needed (e.g., show message to user)
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.getOrNull(0)?.toString() ?: "unknown error"
            Log.e(TAG, "Socket connection error: $error")
            // Handle connection errors (e.g., show error message, retry logic outside SDK)
        }

        // --- Custom Event Listeners ---

        // Update Online Users List
        socket?.on("update-online-users") { args ->
            parseAndEmit(
                args,
                _onlineUsersFlow,
                object : TypeToken<List<OnlineUserResponse>>() {}.type,
                "update-online-users"
            )
        }

        // Receive Challenge
        socket?.on("receive_challenge") { args ->
            parseAndEmit(
                args,
                _challengeReceivedFlow,
                ReceiveChallengeData::class.java,
                "receive_challenge"
            )
        }

        // Challenge Rejected
        socket?.on("challenge_rejected") { args ->
            parseAndEmit(
                args,
                _challengeRejectedFlow,
                ChallengeRejectedData::class.java,
                "challenge_rejected"
            )
        }

        // Game Start
        socket?.on("game_start") { args ->
            parseAndEmit(args, _gameStartFlow, GameStartData::class.java, "game_start")
        }

        // --- NEW Event Listeners ---
        socket?.on("challenge_start") { args -> // Listen for new event
            parseAndEmit(args, _challengeStartFlow, ChallengeStartData::class.java, "challenge_start")
        }

        // Game Over
        socket?.on("game_over") { args ->
            parseAndEmit(args, _gameOverFlow, GameOverData::class.java, "game_over")
        }

        socket?.on("new_round") { args ->
            parseAndEmit(args, _newRoundFlow, NewRoundData::class.java, "new_round")
        }

        socket?.on("solution_result") { args -> // Listen for new event
            parseAndEmit(args, _solutionResultFlow, SolutionResultData::class.java, "solution_result")
        }

        socket?.on("round_over") { args ->
            parseAndEmit(args, _roundOverFlow, RoundOverData::class.java, "round_over")
        }

        socket?.on("challenge_over") { args -> // Listen for new event
            parseAndEmit(args, _challengeOverFlow, ChallengeOverData::class.java, "challenge_over")
        }

        // Solution Invalid
        socket?.on("solution_invalid") { args ->
            parseAndEmit(
                args,
                _solutionInvalidFlow,
                SolutionInvalidData::class.java,
                "solution_invalid"
            )
        }

        // Optional: Challenge Failed
        socket?.on("challenge_failed") { args ->
            parseAndEmit(
                args,
                _challengeFailedFlow,
                ChallengeFailedData::class.java,
                "challenge_failed"
            )
        }
        // Optional: Game Start Failed
        socket?.on("game_start_failed") { args ->
            parseAndEmit(
                args,
                _gameStartFailedFlow,
                GameStartFailedData::class.java,
                "game_start_failed"
            )
        }

        // --- NEW Friend Event Listeners ---
        socket?.on("friend_request_received") { args ->
            parseAndEmit(
                args,
                _friendRequestReceivedFlow,
                FriendRequestReceivedData::class.java,
                "friend_request_received"
            )
        }

        socket?.on("friend_request_accepted") { args ->
            parseAndEmit(
                args,
                _friendRequestAcceptedFlow,
                FriendRequestAcceptedData::class.java,
                "friend_request_accepted"
            )
        }

        socket?.on("friend_request_rejected") { args ->
            parseAndEmit(
                args,
                _friendRequestRejectedFlow,
                FriendRequestRejectedData::class.java,
                "friend_request_rejected"
            )
        }

        socket?.on("friend_removed") { args ->
            parseAndEmit(args, _friendRemovedFlow, FriendRemovedData::class.java, "friend_removed")
        }


    }

    // Generic helper to parse JSON and emit to a SharedFlow
    private fun <T> parseAndEmit(args: Array<Any>, flow: MutableSharedFlow<T>, typeToken: java.lang.reflect.Type, eventName: String) {
        if (args.isNotEmpty() && args[0] != null) {
            try {
                val jsonData = args[0].toString()
                Log.d(TAG, "Received raw data for $eventName: $jsonData")
                val parsedData: T = gson.fromJson(jsonData, typeToken)
                Log.d(TAG, "Parsed data for $eventName: $parsedData")
                // Emit on the main thread for UI safety
                CoroutineScope(Dispatchers.Main).launch {
                    flow.emit(parsedData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON for $eventName: ${e.message}", e)
            }
        } else {
            Log.w(TAG, "Received empty or null data for $eventName")
            // Handle empty list case specifically for online users
            if (flow == _onlineUsersFlow) {
                CoroutineScope(Dispatchers.Main).launch {
                    (flow as MutableSharedFlow<List<OnlineUserResponse>>).emit(emptyList())
                }
            }
        }
    }
    // Overload for Class type (simpler for non-generic types)
    private fun <T> parseAndEmit(args: Array<Any>, flow: MutableSharedFlow<T>, classOfT: Class<T>, eventName: String) {
        parseAndEmit(args, flow, classOfT as java.lang.reflect.Type, eventName)
    }


    // --- Emit Functions ---

    fun emitChallengeUser(opponentUserId: String) {
        if (socket?.connected() == true) {
            val data = ChallengeUserData(opponentUserId)
            val json = gson.toJson(data)
            Log.d(TAG, "Emitting challenge_user: $json")
            socket?.emit("challenge_user", JSONObject(json))
        } else {
            Log.w(TAG, "Cannot emit challenge_user: Socket not connected.")
        }
    }

    fun emitRespondChallenge(challengerId: String, accept: Boolean) {
        if (socket?.connected() == true) {
            val data = RespondChallengeData(challengerId, accept)
            val json = gson.toJson(data)
            Log.d(TAG, "Emitting respond_challenge: $json")
            socket?.emit("respond_challenge", JSONObject(json))
        } else {
            Log.w(TAG, "Cannot emit respond_challenge: Socket not connected.")
        }
    }

    fun emitSubmitSolution(gameId: String, solution: String) {
        if (socket?.connected() == true) {
            val data = SubmitSolutionData(gameId, solution)
            val json = gson.toJson(data)
            Log.d(TAG, "Emitting submit_solution: $json")
            socket?.emit("submit_solution", JSONObject(json))
        } else {
            Log.w(TAG, "Cannot emit submit_solution: Socket not connected.")
        }
    }

    // --- Deprecated Callbacks (Keep if needed, but Flows are preferred) ---
    @Deprecated("Use onlineUsersFlow instead", ReplaceWith("onlineUsersFlow"))
    fun setOnlineUsersCallback(callback: (List<OnlineUserResponse>) -> Unit) {
        // This could potentially bridge to the flow if needed for backward compatibility
        Log.w(TAG, "setOnlineUsersCallback is deprecated. Use onlineUsersFlow.")
    }


    // --- Heartbeat ---
    private fun startHeartbeat(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            while (isConnected && currentUserId == userId) { // Only run if connected and for the current user
                try {
                    kotlinx.coroutines.delay(30000) // 30 seconds
                    if (isConnected && currentUserId == userId) { // Double check before emitting
                        // Log.v(TAG, "Sending heartbeat for $userId") // Verbose logging
                        socket?.emit("heartbeat", userId)
                    }
                } catch (e: Exception) {
                    // Avoid crashing loop on error
                    Log.e(TAG, "Heartbeat error: ${e.message}")
                    // Consider adding a delay before retrying after an error
                    kotlinx.coroutines.delay(5000)
                }
            }
            Log.d(TAG, "Heartbeat stopped for userId: $userId (isConnected=$isConnected, currentUserId=$currentUserId)")
        }
    }
}