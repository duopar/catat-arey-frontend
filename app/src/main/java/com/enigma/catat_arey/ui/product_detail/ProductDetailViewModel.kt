package com.enigma.catat_arey.ui.product_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.enigma.catat_arey.data.network.InventoryLogResponse
import com.enigma.catat_arey.data.network.NetworkRepository
import com.enigma.catat_arey.data.network.ProductDataResponse
import com.enigma.catat_arey.data.network.ProductLogEntryResponse
import com.enigma.catat_arey.data.network.ProductSaleForecastResponse
import com.enigma.catat_arey.data.network.ResponseResult
import com.enigma.catat_arey.data.network.UpdateProductResponse
import com.enigma.catat_arey.data.network.UserDataResponse
import com.enigma.catat_arey.data.preferences.DatastoreManager
import com.enigma.catat_arey.ui.home.HomeUiState
import com.enigma.catat_arey.util.GeneralUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val datastoreManager: DatastoreManager,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    private val _productForecast =
        MutableStateFlow<ProductDetailUiState<List<ProductSaleForecastResponse>>>(
            ProductDetailUiState.Loading
        )
    val productForecast: StateFlow<ProductDetailUiState<List<ProductSaleForecastResponse>>> =
        _productForecast.asStateFlow()

    private val _productLog =
        MutableStateFlow<ProductDetailUiState<List<InventoryLogResponse>>>(ProductDetailUiState.Loading)
    val productLog: StateFlow<ProductDetailUiState<List<InventoryLogResponse>>> =
        _productLog.asStateFlow()


    /*
        Direct user to refresh the app (token) for prolonged usage
     */
    suspend fun shouldRefreshApp(): Boolean {
        val currentTime = GeneralUtil.getCurrentEpoch()
        val expiry = datastoreManager.currentUserTokenExpiry.first()

        // Backend sanity test, ensure token validity regardless actual expiry
        val resp = networkRepository.getAllProduct(null)
        val isTokenInvalidated =
            resp is ResponseResult.Error && resp.message.lowercase().contains("token")

        return (expiry - currentTime) < 3600 || isTokenInvalidated
    }

    /*
        Used when activity startup and retries
     */
    fun getCurrentUserData(): LiveData<HomeUiState<UserDataResponse>> = liveData {
        emit(HomeUiState.Loading)

        when (val resp = networkRepository.getUserData(datastoreManager.currentUserId.first())) {
            is ResponseResult.Error -> emit(HomeUiState.Error(resp.message))
            is ResponseResult.Success -> emit(HomeUiState.Success(resp.data))
        }
    }

    /*
        Used when activity startup and retries
     */
    fun getProductData(productId: String): LiveData<ProductDetailUiState<ProductDataResponse>> =
        liveData {
            emit(ProductDetailUiState.Loading)

            when (val resp = networkRepository.getProductById(productId)) {
                is ResponseResult.Error -> emit(ProductDetailUiState.Error(resp.message))
                is ResponseResult.Success -> emit(ProductDetailUiState.Success(resp.data))
            }
        }

    /*
        Used when user wants to entry product log via form dialog
     */
    fun entryProductLog(
        productId: String,
        stockIn: String,
        stockOut: String
    ): LiveData<ProductDetailUiState<ProductLogEntryResponse>> = liveData {
        emit(ProductDetailUiState.Loading)

        when (val resp =
            networkRepository.entryProductLog(productId, stockIn.toInt(), stockOut.toInt())) {
            is ResponseResult.Error -> emit(ProductDetailUiState.Error(resp.message))
            is ResponseResult.Success -> emit(ProductDetailUiState.Success(resp.data))
        }
    }

    /*
        Used when user wants to delete the product via app bar
     */
    fun deleteProduct(productId: String): LiveData<ProductDetailUiState<Nothing>> = liveData {
        emit(ProductDetailUiState.Loading)

        when (val resp = networkRepository.deleteProduct(productId)) {
            is ResponseResult.Error -> emit(ProductDetailUiState.Error(resp.message))
            is ResponseResult.Success -> emit(ProductDetailUiState.Success(null))
        }
    }

    /*
        Get product forecast at activity startup and retries
     */
    fun getForecast(productId: String) {
        _productForecast.value = ProductDetailUiState.Loading

        viewModelScope.launch {
            when (val resp = networkRepository.getProductSaleForecast(productId)) {
                is ResponseResult.Error -> _productForecast.value =
                    ProductDetailUiState.Error(resp.message)

                is ResponseResult.Success -> _productForecast.value =
                    ProductDetailUiState.Success(resp.data)
            }
        }
    }

    /*
        Get product/inventory logs
     */
    fun getInventoryLogs(productId: String?) {
        _productLog.value = ProductDetailUiState.Loading

        viewModelScope.launch {
            when (val resp = networkRepository.getInventoryLogs(productId)) {
                is ResponseResult.Error -> _productLog.value =
                    ProductDetailUiState.Error(resp.message)

                is ResponseResult.Success -> _productLog.value =
                    ProductDetailUiState.Success(resp.data)
            }
        }
    }

    /*
       Used when user wants to update a selected product via form dialog
    */
    fun updateProduct(
        productId: String,
        name: String,
        category: String,
        price: String,
        stock: String,
        restock: String
    ): LiveData<ProductDetailUiState<UpdateProductResponse>> = liveData {
        emit(ProductDetailUiState.Loading)

        when (val resp = networkRepository.updateProduct(
            productId,
            name,
            category,
            price.toInt(),
            stock.toInt(),
            restock.toInt()
        )) {
            is ResponseResult.Error -> emit(ProductDetailUiState.Error(resp.message))
            is ResponseResult.Success -> emit(ProductDetailUiState.Success(resp.data))
        }
    }
}

sealed interface ProductDetailUiState<out T> {
    data object Loading : ProductDetailUiState<Nothing>
    data class Error(val message: String) : ProductDetailUiState<Nothing>
    data class Success<T>(val data: T?) : ProductDetailUiState<T>
}