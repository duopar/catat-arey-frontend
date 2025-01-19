package com.enigma.catat_arey.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = "this_user")

class UserPreferences(appContext: Context) {
    private val datastore = appContext.datastore

    private val ACCESS_TOKEN = byteArrayPreferencesKey("utoken")
    private val USER_ID = stringPreferencesKey("uid")

    private val ACCESS_TOKEN_EXPIRY = longPreferencesKey("utoken_exp")
    private val REFRESH_TOKEN_EXPIRY = longPreferencesKey("rtoken_exp")

    suspend fun cleanup() {
        datastore.edit {
            it[ACCESS_TOKEN] = ByteArray(0)
            it[USER_ID] = ""
            it[ACCESS_TOKEN_EXPIRY] = -1
            it[REFRESH_TOKEN_EXPIRY] = -1
            it.clear() // Gotta be sure 101%
        }
    }

    val userToken: Flow<ByteArray> =
        datastore.data.map {
            it[ACCESS_TOKEN] ?: ByteArray(0)
        }

    suspend fun updateUserToken(token: ByteArray) {
        datastore.edit { data ->
            data[ACCESS_TOKEN] = token
        }
    }

    val userAccessTokenExpiry: Flow<Long> =
        datastore.data.map {
            it[ACCESS_TOKEN_EXPIRY] ?: -1
        }

    suspend fun updateAccessTokenExpiry(expiry: Long) {
        datastore.edit { data ->
            data[ACCESS_TOKEN_EXPIRY] = expiry
        }
    }

    val userId: Flow<String> =
        datastore.data.map {
            it[USER_ID] ?: ""
        }

    suspend fun updateUserId(userId: String) {
        datastore.edit { data ->
            data[USER_ID] = userId
        }
    }

    val userRefreshTokenExpiry: Flow<Long> =
        datastore.data.map {
            it[REFRESH_TOKEN_EXPIRY] ?: -1
        }

    suspend fun updateRefreshTokenExpiry(expiry: Long) {
        datastore.edit { data ->
            data[REFRESH_TOKEN_EXPIRY] = expiry
        }
    }
}