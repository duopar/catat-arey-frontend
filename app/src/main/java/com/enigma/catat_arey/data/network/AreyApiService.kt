package com.enigma.catat_arey.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AreyApiService {
    @POST("/api/v1/auth/login")
    suspend fun login(
        @Body req: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    @GET("/api/v1/users/{id}")
    suspend fun getUser(@Path("id") userId: String): Response<ApiResponse<UserDataResponse>>
}

/*
    Base response format
*/
data class ApiResponse<T>(
    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: T?
)

data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("token")
    val token: String
)

data class UserDataResponse(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("username")
    val username: String
)