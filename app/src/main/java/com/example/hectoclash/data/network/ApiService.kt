package com.example.hectoclash.data.network

import com.example.hectoclash.data.models.AuthResponse
import com.example.hectoclash.data.models.LeaderboardEntry
import com.example.hectoclash.data.models.FriendListItem
import com.example.hectoclash.data.models.OnlineUserResponse
import com.example.hectoclash.data.models.SignInRequest
import com.example.hectoclash.data.models.SignUpRequest
import com.example.hectoclash.data.models.User
import com.example.hectoclash.data.models.UserProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path

interface ApiService {
    // Public endpoints that don't require authorization
    @POST("api/auth/signin")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>

    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>

    // Protected endpoints that require authorization
    // Add your protected endpoints here
//            @GET("api/user/profile")
//            suspend fun getUserProfile(): Response<Any> // Replace 'Any' with your actual response type

    // Gets the LOGGED IN user's basic profile
    @GET("api/auth/profile")
    suspend fun getMyProfile(): Response<User> // Assuming User model is suitable

    // New endpoint to get online users
    @GET("api/users/online-users")
    suspend fun getOnlineUsers(): Response<List<OnlineUserResponse>>

    // --- Friends ---
    @POST("api/friends/{userId}/request")
    suspend fun sendFriendRequest(@Path("userId") userId: String): Response<GenericResponse> // Use a generic response

    @POST("api/friends/{userId}/accept")
    suspend fun acceptFriendRequest(@Path("userId") userId: String): Response<GenericResponse>

    @DELETE("api/friends/{userId}/remove")
    suspend fun removeFriend(@Path("userId") userId: String): Response<GenericResponse>

    @GET("api/friends/list")
    suspend fun getFriendsList(): Response<List<FriendListItem>> // New model needed

    // Gets a SPECIFIC user's public profile + friendship status relative to logged-in user
    @GET("api/friends/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: String): Response<UserProfileResponse> // New model needed

//    // *** ADD THIS ENDPOINT FOR LEADERBOARD ***
//    @GET("api/leaderboard/global") // Matches your backend route
//    suspend fun getGlobalLeaderboard(
//        @Query("limit") limit: Int? = 50 // Optional limit query parameter
//    ): Response<List<LeaderboardEntry>> // Returns a list of LeaderboardEntry

    // *** UPDATE THIS ENDPOINT FOR THE NEW LEADERBOARD ***
    @GET("api/leaderboard/hectoc-challenge") // Matches your new backend route
    suspend fun getHectocChallengeLeaderboard(
        @Query("limit") limit: Int? = 50 // Optional limit query parameter
    ): Response<List<LeaderboardEntry>> // Returns the updated LeaderboardEntry list

}

// Add GenericResponse if needed for simple messages
data class GenericResponse(val message: String)
