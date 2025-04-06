package com.example.hectoclash.data.models

import com.google.gson.annotations.SerializedName


data class User(
    @SerializedName("_id") val id: String, // Match MongoDB's _id
    val name: String,
    val playerId: String,
    val email: String,

    // Stats fields matching your UserSchema and API response
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val totalGamesPlayed: Int = 0,
    val points: Int = 1000, // Use default from schema
    val rating: Int? = 1000, // Use default from schema, make nullable if it might not exist
    val createdAt: String? = null // Store ISO date string from backend
)

// Request model for sign-in
data class SignInRequest(
    val email: String,
    val password: String
)

// Request model for sign-up
data class SignUpRequest(
    val name: String,
    val playerId: String,
    val email: String,
    val password: String
)

// Response model for authentication
data class AuthResponse(
    val _id: String,
    val name: String,
    val playerId: String,
    val email: String,
    val token: String,
    val message: String? = null,
    val error: String? = null
)

// User model
//data class User(
//    val id: String,
//    val name: String,
//    val playerId: String,
//    val email: String
//)

// Online User response model
data class OnlineUserResponse(
    val _id: String,
    val name: String,
    val playerId: String
)


// Ensure this is a TOP-LEVEL class, not nested
data class PlayerInfo(
    val id: String,
    val name: String
)

// ChallengeStartData uses the top-level PlayerInfo
data class ChallengeStartData(
    val gameId: String,
    val totalRounds: Int,
    val roundTimeLimitSeconds: Int,
    val overallTimeLimitSeconds: Int,
    val player1: PlayerInfo, // Uses the top-level PlayerInfo
    val player2: PlayerInfo, // Uses the top-level PlayerInfo
    val currentRound: Int,
    val puzzle: String,
    val player1Score: Int,
    val player2Score: Int
)

// NEW: Data for starting subsequent rounds
data class NewRoundData(
    val gameId: String,
    val roundNumber: Int,
    val puzzle: String,
    val player1Score: Int,
    val player2Score: Int,
    val roundTimeLimitSeconds: Int
)

// NEW: Result of a player's submission attempt
data class SolutionResultData(
    val gameId: String,
    val round: Int,
    val status: String, // "correct", "incorrect", "invalid"
    val reason: String? = null, // e.g., "digit_mismatch", "wrong_result", "already_submitted_this_round"
    val details: String? = null,
    val timeTakenMs: Long? = null // Time taken for this specific submission
)

// NEW: Data when a single round ends
data class RoundOverData(
    val gameId: String,
    val roundNumber: Int,
    val roundWinnerId: String?, // Null if timeout or draw for the round
    val reason: String, // "solved" or "timeout"
    val player1Score: Int,
    val player2Score: Int,
    val player1RoundInfo: RoundPlayerInfo?, // Nullable if player didn't participate/submit
    val player2RoundInfo: RoundPlayerInfo?,
    val puzzle: String? // The puzzle for this round (useful for display)
) {
    data class RoundPlayerInfo(
        val solution: String?,
        val timeTakenMs: Long?,
        val correct: Boolean?
    )
}

// NEW: Challenge Over replaces GameOverData
data class ChallengeOverData(
    val gameId: String,
    val finalStatus: String, // "completed", "timeout", "abandoned", "error"
    val reason: String?,
    val challengeWinnerId: String?, // Null if draw or abandoned/error
    val challengeLoserId: String?,
    val isDraw: Boolean,
    val player1Score: Int,
    val player2Score: Int,
    val roundsData: List<RoundDetailData>? // Detailed breakdown (optional on client)
) {
    // Optional: Define RoundDetailData if you need to display full history
    data class RoundDetailData(
        val roundNumber: Int,
        val puzzle: String,
        val startTime: String?, // Server sends Date string
        val endTime: String?,
        val player1: RoundOverData.RoundPlayerInfo?,
        val player2: RoundOverData.RoundPlayerInfo?,
        val roundWinnerId: String?,
        val endedReason: String?
    )
    // Define RoundPlayerInfo here if not reusing the one from RoundOverData
    // data class RoundPlayerInfo(...)
}

// For receiving searching_for_match confirmation (can be simple)
data class SearchingForMatchData( // Optional: could just be a status update
    val status: String = "searching" // Or server could just send the event name
)

// For receiving left_matchmaking confirmation (can be simple)
data class LeftMatchmakingData( // Optional
    val status: String = "left_queue"
)

// For receiving matchmaking_failed event
data class MatchmakingFailedData(
    val reason: String // e.g., 'not_logged_in', 'already_in_game', 'opponent_left_queue'
)

// OLD: GameOverData - Can be removed or kept for reference
/*
data class GameOverData(...)
*/

// OLD: SolutionInvalidData - Replaced by SolutionResultData
/*
data class SolutionInvalidData(...)
*/

// ChallengeFailedData remains the same
// GameStartFailedData remains the same

// --- API Response Models ---

// Modify LeaderboardEntry for the new HectoClash Leaderboard
data class LeaderboardEntry(
    // @SerializedName("_id") val id: String, // Use userId if available and unique
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String?, // Make nullable if user might be deleted
    @SerializedName("playerId") val playerId: String,
    @SerializedName("totalChallengesPlayed") val totalChallengesPlayed: Int,
    @SerializedName("wins") val wins: Int,
    @SerializedName("losses") val losses: Int,
    @SerializedName("draws") val draws: Int,
    @SerializedName("accuracy") val accuracy: Double?, // Nullable if player has no games
    @SerializedName("avgSpeedMs") val avgSpeedMs: Double?, // Nullable if no rounds won/calculated
    @SerializedName("totalRoundsWon") val totalRoundsWon: Int,
    // Note: 'points' might not be part of this specific leaderboard response anymore
    // @SerializedName("points") val points: Int,
)

// Other models (SignInRequest, SignUpRequest, AuthResponse, User, OnlineUserResponse, Friend models) remain the same.