package com.example.hectoclash.data.network

import com.example.hectoclash.data.models.AuthResponse
import com.example.hectoclash.data.models.OnlineUserResponse
import com.example.hectoclash.data.models.SignInRequest
import com.example.hectoclash.data.models.SignUpRequest
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
    // Add your protected endpoints here
    @GET("api/user/profile")
    suspend fun getUserProfile(): Response<Any> // Replace 'Any' with your actual response type

    // New endpoint to get online users
    @GET("api/users/online-users")
    suspend fun getOnlineUsers(): Response<List<OnlineUserResponse>>
}