package com.enigma.catat_arey.util

object ExceptionHandler {
    fun getContextFromNetworkError(e: Exception): String {
        return when (e) {
            is java.net.UnknownHostException -> "Network Error"
            is java.net.SocketTimeoutException -> "Network Error"
            else -> "Unknown Error"
        }
    }
}