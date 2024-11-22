package com.enigma.catat_arey.data.network

import okhttp3.Interceptor
import okhttp3.Response

object AreyApiConfig {
    class DefaultInterceptor(val apiKey: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val req = chain.request().newBuilder()
                .addHeader("X-API-Key", apiKey)
                .build()
            return chain.proceed(req)
        }
    }

    class AuthenticatedInterceptor(val apiKey: String, val tokenProvider: AreyTokenProvider) :
        Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val req = chain.request().newBuilder()
                .addHeader("X-API-Key", apiKey)
                .addHeader("Authorization", "Bearer ${tokenProvider.getToken()}")
                .build()
            return chain.proceed(req)
        }
    }
}