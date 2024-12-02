package com.enigma.catat_arey.data.network

import com.google.gson.annotations.SerializedName

/*
    Client side handling
 */
sealed interface ResponseResult<out T> {
    data class Error(val message: String) : ResponseResult<Nothing>
    data class Success<T>(val data: T) : ResponseResult<T>
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

data class EpochTimeResponse(
    @SerializedName("_seconds")
    var secondsInEpoch: Int,

    @SerializedName("_nanoseconds")
    var nanoSeconds: Int
)

//
// USER-RELATED //
//
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
    val username: String,

    @SerializedName("createdAt")
    val createdAt: EpochTimeResponse,

    @SerializedName("updatedAt")
    val updatedAt: EpochTimeResponse,
)

//
// PRODUCT-RELATED //
//
data class AddProductResponse(
    @SerializedName("productId")
    val productId: String
)

data class ProductsDataResponse(
    @SerializedName("productId")
    val productId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("price")
    val price: Int,

    @SerializedName("stockLevel")
    val stockLevel: Int,

    @SerializedName("restockThreshold")
    val restockThreshold: Int,

    @SerializedName("createdAt")
    val createdAt: EpochTimeResponse,

    @SerializedName("updatedAt")
    val updatedAt: EpochTimeResponse
)

data class ProductDataResponse(
    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("price")
    val price: Int,

    @SerializedName("stockLevel")
    val stockLevel: Int,

    @SerializedName("restockThreshold")
    val restockThreshold: Int,

    @SerializedName("createdAt")
    val createdAt: EpochTimeResponse,

    @SerializedName("updatedAt")
    val updatedAt: EpochTimeResponse
)