package com.example.hectoclash.data.repository

import android.content.Context
import com.example.hectoclash.data.local.TokenManager
import com.example.hectoclash.data.models.AuthResponse
import com.example.hectoclash.data.models.SignInRequest
import com.example.hectoclash.data.models.SignUpRequest
import com.example.hectoclash.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    private val apiService = RetrofitClient.apiService
    private val tokenManager = TokenManager.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(context).also { INSTANCE = it }
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SignInRequest(email, password)

                // Get existing token if available
                val existingToken = tokenManager.getToken.firstOrNull()

                // If we already have a token, we'll use AuthInterceptor
                val response = if (!existingToken.isNullOrEmpty()) {
                    RetrofitClient.getAuthenticatedApiService(context).signIn(request)
                } else {
                    apiService.signIn(request)
                }

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

    suspend fun signUp(
        name: String,
        playerId: String,
        email: String,
        password: String
    ): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SignUpRequest(name, playerId, email, password)
                val response = apiService.signUp(request)

                if (response.isSuccessful) {
                    response.body()?.let {
                        // Connect to socket
                        SocketManager.connect(it._id)

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

    // Add method for protected API calls
    suspend fun getProtectedData(): Result<Any> {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.getAuthenticatedApiService(context).getUserProfile()

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