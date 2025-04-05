package com.example.hectoclash.data.models

import com.google.gson.annotations.SerializedName

// --- Data Payloads for Socket Events ---

// For emitting challenge_user
data class ChallengeUserData(
    val opponentUserId: String
)

// For emitting respond_challenge
data class RespondChallengeData(
    val challengerId: String,
    val accepted: Boolean
)

// For emitting submit_solution
data class SubmitSolutionData(
    val gameId: String,
    val solution: String
)


// --- Data Received from Socket Events ---

// For receive_challenge event
data class ReceiveChallengeData(
    val challengerId: String,
    val challengerName: String
)

// For challenge_rejected event
data class ChallengeRejectedData(
    val opponentId: String,
    val opponentName: String
)

// For game_start event
data class GameStartData(
    val gameId: String,
    val puzzle: String,
    val timeLimitSeconds: Int,
    val player1: PlayerInfo,
    val player2: PlayerInfo
) {
    data class PlayerInfo(
        val id: String,
        val name: String
    )
}

// For game_over event
// From GameModels.kt (previous response)
data class GameOverData(
    val gameId: String,
    val winnerId: String?,
    val loserId: String?,
    val reason: String,
    val status: String,
    // Change to structured info
    val player1Info: PlayerSolutionInfo?, // Make nullable if player might not exist/submit
    val player2Info: PlayerSolutionInfo?
)
// For player_solution event
data class PlayerSolutionInfo(
    val id: String,
    val solution: String? // Solution itself can be null if not submitted
)

// For solution_invalid event
data class SolutionInvalidData(
    val gameId: String,
    val reason: String, // e.g., "digit_mismatch", "wrong_result", "evaluation_error", "game_already_over"
    val details: String? = null // Optional extra info like the error message
)

// For challenge_failed event (optional, if server emits it)
data class ChallengeFailedData(
    val reason: String // e.g., "opponent_offline"
)

// For game_start_failed event (optional, if server emits it)
data class GameStartFailedData(
    val reason: String // e.g., "server_error"
)