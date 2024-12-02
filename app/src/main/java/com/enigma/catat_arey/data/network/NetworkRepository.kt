package com.enigma.catat_arey.data.network

import com.enigma.catat_arey.data.di.AuthenticatedApi
import com.enigma.catat_arey.data.di.DefaultApi
import com.enigma.catat_arey.util.ExceptionHandler
import com.google.gson.Gson
import retrofit2.Response
import javax.inject.Inject

class NetworkRepository @Inject constructor(
    private val tokenProvider: AreyTokenProvider,
    @DefaultApi private val defaultApiService: AreyApiService,
    @AuthenticatedApi private val authenticatedApiService: AreyApiService
) {
    fun updateToken(token: String) {
        tokenProvider.setToken(token)
    }

    suspend fun login(username: String, password: String): ResponseResult<LoginResponse> {
        return ApiCallWrapper.safeApiCall {
            defaultApiService.login(LoginRequest(username, password))
        }
    }

    suspend fun getUserData(userId: String): ResponseResult<UserDataResponse> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.getUser(userId)
        }
    }

    suspend fun getAllProduct(): ResponseResult<List<ProductsDataResponse>> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.getAllProducts()
        }
    }

    suspend fun addNewProduct(
        name: String,
        category: String,
        price: String,
        stock: String,
        restock: String
    ): ResponseResult<AddProductResponse> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.addProduct(
                AddProductRequest(
                    name, category, price.toInt(), stock.toInt(), restock.toInt()
                )
            )
        }
    }
}

object ApiCallWrapper {
    suspend fun <T : Any> safeApiCall(
        apiCall: suspend () -> Response<ApiResponse<T>>
    ): ResponseResult<T> {
        return try {
            val response = apiCall()

            if (response.isSuccessful) {
                val body = response.body()
                body?.data?.let {
                    ResponseResult.Success(it)
                } ?: ResponseResult.Error("Empty response body")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    Gson().fromJson(errorBody, ApiResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                ResponseResult.Error(errorResponse?.message ?: "Unknown error occurred")
            }
        } catch (e: Exception) {
            ResponseResult.Error(ExceptionHandler.getContextFromNetworkError(e))
        }
    }
}