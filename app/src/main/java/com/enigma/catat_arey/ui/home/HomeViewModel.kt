package com.enigma.catat_arey.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.enigma.catat_arey.data.network.AddProductResponse
import com.enigma.catat_arey.data.network.NetworkRepository
import com.enigma.catat_arey.data.network.ProductsDataResponse
import com.enigma.catat_arey.data.network.ResponseResult
import com.enigma.catat_arey.data.network.UserDataResponse
import com.enigma.catat_arey.data.preferences.DatastoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val datastoreManager: DatastoreManager,
    private val networkRepository: NetworkRepository
) : ViewModel() {
    private val _allProducts =
        MutableStateFlow<HomeUiState<List<ProductsDataResponse>>>(HomeUiState.Loading)
    val allProducts: StateFlow<HomeUiState<List<ProductsDataResponse>>> = _allProducts.asStateFlow()

    fun getCurrentUserData(): LiveData<HomeUiState<UserDataResponse>> = liveData {
        emit(HomeUiState.Loading)

        when (val resp = networkRepository.getUserData(datastoreManager.currentUserId.first())) {
            is ResponseResult.Error -> emit(HomeUiState.Error(resp.message))
            is ResponseResult.Success -> emit(HomeUiState.Success(resp.data))
        }
    }

    fun retrieveAllProducts() {
        viewModelScope.launch {
            val resp = networkRepository.getAllProduct()

            _allProducts.value = HomeUiState.Loading

            when (resp) {
                is ResponseResult.Error -> _allProducts.value = HomeUiState.Error(resp.message)
                is ResponseResult.Success -> _allProducts.value = HomeUiState.Success(resp.data)
            }
        }
    }

    fun addNewProduct(
        name: String,
        category: String,
        price: String,
        stock: String,
        restock: String
    ): LiveData<HomeUiState<AddProductResponse>> = liveData {
        emit(HomeUiState.Loading)

        when (val resp = networkRepository.addNewProduct(name, category, price, stock, restock)) {
            is ResponseResult.Error -> emit(HomeUiState.Error(resp.message))
            is ResponseResult.Success -> emit(HomeUiState.Success(resp.data))
        }
    }
}

sealed interface HomeUiState<out T> {
    data object Loading : HomeUiState<Nothing>
    data class Error(val message: String) : HomeUiState<Nothing>
    data class Success<T>(val data: T?) : HomeUiState<T>
}