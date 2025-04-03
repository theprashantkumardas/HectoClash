package com.example.hectoclash.data.models

data class LeaderboardEntryResponse(
    val _id: String,
    val user: UserInfo,
    val totalgames: Int,
    val totalwins: Int,
    val score: Int,
    val country: String?,
    val avatar: String?,
    val rank: Int
)


data class LeaderboardEntry(
    val rank: Int,
    val playerId: String,
    val profilePicRes: Int,
    val matches: Int,
    val won: Int,
    val loss: Int,
    val points: Int
)

data class UserInfo(
    val _id: String,
    val name: String,
    val playerId: String,
    val email: String
)

data class LeaderboardResponse(
    val success: Boolean,
    val topPlayers: List<LeaderboardEntryResponse>
)