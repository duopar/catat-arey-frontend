package com.enigma.catat_arey.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AreyApiService {
    @POST("/api/v1/auth/login")
    suspend fun login(
        @Body req: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    @POST("/api/v1/auth/refresh")
    suspend fun refreshToken(): Response<ApiResponse<RefreshTokenResponse>>

    @GET("/api/v1/users/{id}")
    suspend fun getUser(@Path("id") userId: String): Response<ApiResponse<UserDataResponse>>

    @PATCH("/api/v1/users/{id}")
    suspend fun updateUser(
        @Path("id") userId: String,
        @Body req: UpdateUserRequest
    ): Response<ApiResponse<Nothing>>

    @GET("/api/v1/products")
    suspend fun getAllProducts(@Query("name") productName: String? = null): Response<ApiResponse<List<ProductsDataResponse>>>

    @GET("/api/v1/products/{id}")
    suspend fun getProductById(@Path("id") productId: String): Response<ApiResponse<ProductDataResponse>>

    @POST("/api/v1/products")
    suspend fun addProduct(@Body req: AddProductRequest): Response<ApiResponse<AddProductResponse>>

    @PUT("/api/v1/products/{id}")
    suspend fun updateProduct(
        @Path("id") productId: String,
        @Body req: UpdateProductRequest
    ): Response<ApiResponse<UpdateProductResponse>>

    @DELETE("/api/v1/products/{id}")
    suspend fun deleteProduct(@Path("id") productId: String): Response<ApiResponse<Nothing>>

    @POST("/api/v1/inventory-logs")
    suspend fun entryProductLog(@Body req: ProductLogEntryRequest): Response<ApiResponse<ProductLogEntryResponse>>

    @GET("/api/v1/forecasts")
    suspend fun getSaleForecastById(@Query("productId") productId: String): Response<ApiResponse<List<ProductSaleForecastResponse>>>

    @GET("/api/v1/inventory-logs")
    suspend fun getInventoryLogs(@Query("productId") productId: String?): Response<ApiResponse<List<InventoryLogResponse>>>
}