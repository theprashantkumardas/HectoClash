package com.example.hectoclash.data.repository


import android.content.Context
import com.example.hectoclash.data.models.*
import com.example.hectoclash.data.network.GenericResponse
import com.example.hectoclash.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response

class FriendRepository(private val context: Context) {

    private val authenticatedApiService = RetrofitClient.getAuthenticatedApiService(context)

    companion object {
        @Volatile private var INSTANCE: FriendRepository? = null
        fun getInstance(context: Context): FriendRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FriendRepository(context).also { INSTANCE = it }
            }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = call()
                if (response.isSuccessful) {
                    response.body()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result.failure(HttpException(response)) // Propagate HttpException
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    suspend fun sendFriendRequest(userId: String): Result<GenericResponse> = safeApiCall {
        authenticatedApiService.sendFriendRequest(userId)
    }

    suspend fun acceptFriendRequest(userId: String): Result<GenericResponse> = safeApiCall {
        authenticatedApiService.acceptFriendRequest(userId)
    }

    suspend fun removeFriend(userId: String): Result<GenericResponse> = safeApiCall {
        authenticatedApiService.removeFriend(userId)
    }

    suspend fun getFriendsList(): Result<List<FriendListItem>> = safeApiCall {
        authenticatedApiService.getFriendsList()
    }

    suspend fun getUserProfile(userId: String): Result<UserProfileResponse> = safeApiCall {
        authenticatedApiService.getUserProfile(userId)
    }
}