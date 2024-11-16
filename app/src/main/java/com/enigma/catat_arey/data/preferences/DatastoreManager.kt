package com.enigma.catat_arey.data.preferences

import kotlinx.coroutines.flow.Flow

class DatastoreManager(
    private val userPreferences: UserPreferences
) {
    var currentUserToken: Flow<ByteArray> = userPreferences.userToken

    suspend fun updateUserToken(token: ByteArray) {
        userPreferences.updateUserToken(token)
    }
}