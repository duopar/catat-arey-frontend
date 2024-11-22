package com.enigma.catat_arey.data.network

import android.util.Log
import com.enigma.catat_arey.data.di.AuthenticatedApi
import com.enigma.catat_arey.data.di.DefaultApi
import com.google.gson.Gson
import javax.inject.Inject

class NetworkRepository @Inject constructor(
    private val tokenProvider: AreyTokenProvider,
    @DefaultApi private val defaultApiService: AreyApiService,
    @AuthenticatedApi private val authenticatedApiService: AreyApiService
) {
    fun updateToken(token: String) {
        tokenProvider.setToken(token)
    }

    suspend fun login(username: String, password: String): String? {
        val call = defaultApiService.login(LoginRequest(username, password))
        val resp = call.body()
        if (call.isSuccessful) {
            Log.d("NetworkRepo", resp!!.message)
            return resp.data!!.token
        } else {
            val errBody = Gson().fromJson(call.errorBody()!!.string(), ApiResponse::class.java)
            Log.d("NetworkRepo", errBody.message)
        }
        return null
    }

    suspend fun getUserInfo(userId: String): UserDataResponse? {
        val call = authenticatedApiService.getUser(userId)
        val resp = call.body()
        if (call.isSuccessful) {
            return resp!!.data
        } else {
            val errBody = Gson().fromJson(call.errorBody()!!.string(), ApiResponse::class.java)
            Log.d("NetworkRepo", errBody.message)
        }
        return null
    }
}