package com.example.hectoclash.data.network

import com.example.hectoclash.data.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    // Public endpoints that don't require authorization
    @POST("api/auth/signin")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>

    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>

    // Protected endpoints that require authorization
    @GET("api/user/profile")
    suspend fun getUserProfile(): Response<Any>

    // New endpoint to get online users
    @GET("api/users/online-users")
    suspend fun getOnlineUsers(): Response<List<OnlineUserResponse>>
    
    // Leaderboard endpoints
    @GET("api/leaderboard/leaderboard")
    suspend fun getLeaderboard(): Response<LeaderboardResponse>


}