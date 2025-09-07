package com.enigma.catat_arey.data.network

import android.util.Log
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
    fun getToken(): String? {
        return tokenProvider.getToken()
    }

    fun updateToken(token: String) {
        tokenProvider.setToken(token)
    }

    suspend fun login(username: String, password: String): ResponseResult<LoginResponse?> {
        return ApiCallWrapper.safeApiCall {
            defaultApiService.login(LoginRequest(username, password))
        }
    }

    suspend fun updateUser(
        userId: String,
        currentPassword: String,
        newPassword: String
    ): ResponseResult<Nothing?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.updateUser(
                userId, UpdateUserRequest(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmPassword = newPassword
                )
            )
        }
    }

    suspend fun getUserData(userId: String): ResponseResult<UserDataResponse?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.getUser(userId)
        }
    }

    suspend fun getAllProduct(name: String?): ResponseResult<List<ProductsDataResponse>?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.getAllProducts(name)
        }
    }

    suspend fun getProductById(productId: String): ResponseResult<ProductDataResponse?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.getProductById(productId)
        }
    }

    suspend fun deleteProduct(productId: String): ResponseResult<Nothing?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.deleteProduct(productId)
        }
    }

    suspend fun refreshToken(refreshToken: String): ResponseResult<RefreshTokenResponse?> {
        tokenProvider.setToken(refreshToken)

        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.refreshToken()
        }
    }

    suspend fun addNewProduct(
        name: String,
        category: String,
        price: String,
        stock: String,
        restock: String
    ): ResponseResult<AddProductResponse?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.addProduct(
                AddProductRequest(
                    name, category, price.toInt(), stock.toInt(), restock.toInt()
                )
            )
        }
    }

    suspend fun entryProductLog(
        productId: String,
        stockIn: Int,
        stockOut: Int
    ): ResponseResult<ProductLogEntryResponse?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.entryProductLog(
                ProductLogEntryRequest(productId, stockIn, stockOut)
            )
        }
    }

    suspend fun getProductSaleForecast(productId: String): ResponseResult<List<ProductSaleForecastResponse>?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.getSaleForecastById(productId)
        }
    }

    suspend fun getInventoryLogs(productId: String?): ResponseResult<List<InventoryLogResponse>?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.getInventoryLogs(productId)
        }
    }

    suspend fun updateProduct(
        productId: String,
        name: String,
        category: String,
        price: Int,
        stock: Int,
        restock: Int
    ): ResponseResult<UpdateProductResponse?> {
        return ApiCallWrapper.safeApiCall {
            authenticatedApiService.updateProduct(
                productId,
                UpdateProductRequest(category, price, stock, restock)
            )
        }
    }
}

object ApiCallWrapper {
    suspend fun <T : Any> safeApiCall(
        apiCall: suspend () -> Response<ApiResponse<T>>
    ): ResponseResult<T?> {
        return try {
            val response = apiCall()

            if (response.isSuccessful) {
                val body = response.body()

                if (response.isSuccessful) {
                    ResponseResult.Success(body?.data)
                } else {
                    ResponseResult.Error("Empty response body")
                }

            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = try {
                    Gson().fromJson(errorBody, ApiResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                Log.d("NetworkRepository", errorBody!!)

                ResponseResult.Error(errorResponse?.message ?: "Unknown error occurred")
            }
        } catch (e: Exception) {
            ResponseResult.Error(ExceptionHandler.getContextFromNetworkError(e))
        }
    }
}