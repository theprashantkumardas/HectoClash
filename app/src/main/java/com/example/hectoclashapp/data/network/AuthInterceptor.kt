package com.example.hectoclash.data.network

import android.content.Context
import com.example.hectoclash.data.local.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    private val tokenManager = TokenManager.getInstance(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Get token synchronously from the DataStore
        val token = runBlocking {
            tokenManager.getToken.firstOrNull()
        }

        // If token is available, add it to the request
        return if (!token.isNullOrEmpty()) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}