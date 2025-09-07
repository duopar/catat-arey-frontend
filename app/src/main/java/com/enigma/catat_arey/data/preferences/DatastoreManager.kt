package com.enigma.catat_arey.data.preferences

import kotlinx.coroutines.flow.Flow

class DatastoreManager(
    private val userPreferences: UserPreferences
) {
    var currentUserToken: Flow<ByteArray> = userPreferences.userToken
    var currentUserId: Flow<String> = userPreferences.userId
    var currentUserTokenExpiry: Flow<Long> = userPreferences.userAccessTokenExpiry
    var currentRefreshTokenExpiry: Flow<Long> = userPreferences.userRefreshTokenExpiry

    suspend fun cleanup() {
        userPreferences.cleanup()
    }

    suspend fun updateUserToken(token: ByteArray) {
        userPreferences.updateUserToken(token)
    }

    suspend fun updateUserTokenExpiry(expiry: Long) {
        userPreferences.updateAccessTokenExpiry(expiry)
    }

    suspend fun updateUserId(userId: String) {
        userPreferences.updateUserId(userId)
    }

    suspend fun updateRefreshTokenExpiry(expiry: Long) {
        userPreferences.updateRefreshTokenExpiry(expiry)
    }
}