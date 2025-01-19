package com.enigma.catat_arey.util

import android.util.Log

object ExceptionHandler {
    fun getContextFromNetworkError(e: Exception): String {
        return when (e) {
            is java.net.UnknownHostException -> "Network Error"
            is java.net.SocketTimeoutException -> "Network Error"
            else -> {
                Log.d("AreyHandler", e.stackTraceToString())
                return "Unknown Error"
            }
        }
    }
}