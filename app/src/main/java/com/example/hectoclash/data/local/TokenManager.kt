package com.example.hectoclash.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hectoclash.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//TokenManager to store and retrieve authentication data:

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

class TokenManager(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey(Constants.TOKEN_KEY)
        private val USER_ID_KEY = stringPreferencesKey(Constants.USER_ID_KEY)
        private val USER_NAME_KEY = stringPreferencesKey(Constants.USER_NAME_KEY)
        private val USER_EMAIL_KEY = stringPreferencesKey(Constants.USER_EMAIL_KEY)
        private val PLAYER_ID_KEY = stringPreferencesKey(Constants.PLAYER_ID_KEY)

        // Singleton instance
        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // Get the JWT token
    val getToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    // Get the user ID
    val getUserId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    // Get the user name
    val getUserName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    // Get the user email
    val getUserEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY]
    }

    // Get the player ID
    val getPlayerId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PLAYER_ID_KEY]
    }

    // Save user data
    suspend fun saveUserData(userId: String, name: String, email: String, playerId: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_NAME_KEY] = name
            preferences[USER_EMAIL_KEY] = email
            preferences[PLAYER_ID_KEY] = playerId
            preferences[TOKEN_KEY] = token
        }
    }

    // Clear the stored data
    suspend fun clearData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}