package com.enigma.catat_arey.data.network

class AreyTokenProvider {
    @Volatile
    private var token: String? = null

    fun setToken(newToken: String) {
        token = newToken
    }

    fun getToken(): String? = token
}