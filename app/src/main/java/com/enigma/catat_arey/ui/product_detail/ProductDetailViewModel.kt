package com.enigma.catat_arey.ui.product_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.enigma.catat_arey.data.network.NetworkRepository
import com.enigma.catat_arey.data.network.ProductDataResponse
import com.enigma.catat_arey.data.network.ProductLogEntryResponse
import com.enigma.catat_arey.data.network.ProductSaleForecastResponse
import com.enigma.catat_arey.data.network.ResponseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val networkRepository: NetworkRepository
) : ViewModel() {

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
    fun getForecast(productId: String): LiveData<ProductDetailUiState<List<ProductSaleForecastResponse>>> =
        liveData {
            emit(ProductDetailUiState.Loading)

            when (val resp = networkRepository.getProductSaleForecast(productId)) {
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