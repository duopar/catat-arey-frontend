package com.enigma.catat_arey.data.network

import com.google.gson.annotations.SerializedName

//
// USER-RELATED //
//
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

data class UpdateUserRequest(
    @SerializedName("currentPassword")
    val currentPassword: String,

    @SerializedName("newPassword")
    val newPassword: String,

    @SerializedName("confirmPassword")
    val confirmPassword: String
)

//
// PRODUCT-RELATED //
//
data class AddProductRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("price")
    val price: Int,

    @SerializedName("stockLevel")
    val stockLevel: Int,

    @SerializedName("restockThreshold")
    val restockThreshold: Int
)

data class UpdateProductRequest(
    @SerializedName("category")
    val category: String?,

    @SerializedName("price")
    val price: Int,

    @SerializedName("stockLevel")
    val stockLevel: Int,

    @SerializedName("restockThreshold")
    val restockThreshold: Int
)

data class ProductLogEntryRequest(
    @SerializedName("productId")
    val productId: String,

    @SerializedName("stockIn")
    val stockIn: Int,

    @SerializedName("stockOut")
    val stockOut: Int
)