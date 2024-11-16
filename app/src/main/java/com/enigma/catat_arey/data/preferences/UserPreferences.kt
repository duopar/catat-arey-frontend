package com.enigma.catat_arey.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = "this_user")

class UserPreferences(appContext: Context) {
    private val datastore = appContext.datastore
    private val USER_TOKEN = byteArrayPreferencesKey("utoken")

    val userToken: Flow<ByteArray> =
        datastore.data.map {
            it[USER_TOKEN] ?: ByteArray(0)
        }

    suspend fun updateUserToken(token: ByteArray) {
        datastore.edit { data ->
            data[USER_TOKEN] = token
        }
    }
}