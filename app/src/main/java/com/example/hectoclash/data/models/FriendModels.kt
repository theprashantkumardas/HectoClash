package com.example.hectoclash.data.models

// For API response: /api/friends/list
data class FriendListItem(
    val _id: String,
    val name: String,
    val playerId: String,
    val status: String, // 'pending', 'requested', 'accepted'
    val isOnline: Boolean? = false // Include online status
)

// For API response: /api/friends/{userId}/profile
data class UserProfileResponse(
    val _id: String,
    val name: String,
    val playerId: String,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val rating: Int,
    val totalGamesPlayed: Int,
    val createdAt: String, // Or parse to Date/LocalDateTime
    val friendshipStatus: String // 'none', 'friends', 'request_sent', 'request_received'
)

// For Socket Event: friend_request_received
data class FriendRequestReceivedData(
    val senderId: String,
    val senderName: String,
    val senderPlayerId: String
)

// For Socket Event: friend_request_accepted
data class FriendRequestAcceptedData(
    val acceptorId: String,
    val acceptorName: String,
    val acceptorPlayerId: String
)

// For Socket Event: friend_request_rejected
data class FriendRequestRejectedData(
    val rejectorId: String,
    val rejectorName: String
)
// For Socket Event: friend_removed
data class FriendRemovedData(
    val removerId: String,
    val removerName: String
)