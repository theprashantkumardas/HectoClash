package com.example.hectoclash.data.network

import android.content.Context
import com.example.hectoclash.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Client without auth interceptor for auth endpoints (login/register)
    private val basicHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Base Retrofit instance for auth endpoints
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(basicHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // This API service is for auth endpoints
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // Create an authenticated client for protected endpoints
    private var authenticatedApiService: ApiService? = null

    fun getAuthenticatedApiService(context: Context): ApiService {
        if (authenticatedApiService == null) {
            val authInterceptor = AuthInterceptor(context)
            val authenticatedClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val authenticatedRetrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(authenticatedClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            authenticatedApiService = authenticatedRetrofit.create(ApiService::class.java)
        }

        return authenticatedApiService!!
    }
}