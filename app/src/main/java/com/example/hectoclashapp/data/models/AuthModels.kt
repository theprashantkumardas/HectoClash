package com.example.hectoclash.data.models

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
data class User(
    val id: String,
    val name: String,
    val playerId: String,
    val email: String
)

// Online User response model
data class OnlineUserResponse(
    val _id: String,
    val name: String,
    val playerId: String
)