package com.enigma.catat_arey.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.enigma.catat_arey.data.network.AddProductResponse
import com.enigma.catat_arey.data.network.InventoryLogResponse
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

    /*
        Use continuous data container for search feature
     */
    private val _allProducts =
        MutableStateFlow<HomeUiState<List<ProductsDataResponse>>>(HomeUiState.Loading)
    val allProducts: StateFlow<HomeUiState<List<ProductsDataResponse>>> = _allProducts.asStateFlow()

    /*
        Direct user to refresh the app (token) for prolonged usage
     */
    suspend fun shouldRefreshApp(): Boolean {
        val expiry = datastoreManager.currentUserTokenExpiry.first()

        // if access token lifetime is less than 1 hour
        if ((expiry - System.currentTimeMillis() / 1000) < 3600) {
            return true
        }

        return false
    }

    /*
        Used when activity startup and retries
     */
    fun getInventoryLogs(): LiveData<HomeUiState<List<InventoryLogResponse>>> = liveData {
        emit(HomeUiState.Loading)

        when (val resp = networkRepository.getInventoryLogs()) {
            is ResponseResult.Error -> emit(HomeUiState.Error(resp.message))
            is ResponseResult.Success -> emit(HomeUiState.Success(resp.data))
        }
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
        Used when activity startup and retries, also search feature
     */
    fun retrieveAllProducts(name: String?) {
        _allProducts.value = HomeUiState.Loading

        viewModelScope.launch {
            when (val resp = networkRepository.getAllProduct(name)) {
                is ResponseResult.Error -> _allProducts.value = HomeUiState.Error(resp.message)
                is ResponseResult.Success -> _allProducts.value = HomeUiState.Success(resp.data)
            }
        }
    }

    /*
        Used when user wants to add a new product via form dialog
     */
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