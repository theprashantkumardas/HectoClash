package com.example.hectoclash.data.repository

import android.content.Context
import com.example.hectoclash.data.models.OnlineUserResponse
import com.example.hectoclash.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnlineUsersRepository(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: OnlineUsersRepository? = null

        fun getInstance(context: Context): OnlineUsersRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnlineUsersRepository(context).also { INSTANCE = it }
            }
        }
    }

    suspend fun getOnlineUsers(): Result<List<OnlineUserResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getAuthenticatedApiService(context).getOnlineUsers()

                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception(errorBody ?: "Unknown error occurred"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}