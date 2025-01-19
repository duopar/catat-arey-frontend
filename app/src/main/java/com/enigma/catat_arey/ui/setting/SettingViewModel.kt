package com.enigma.catat_arey.ui.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.enigma.catat_arey.data.network.NetworkRepository
import com.enigma.catat_arey.data.network.ResponseResult
import com.enigma.catat_arey.data.network.UserDataResponse
import com.enigma.catat_arey.data.preferences.DatastoreManager
import com.enigma.catat_arey.ui.home.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val datastoreManager: DatastoreManager,
    private val networkRepository: NetworkRepository
) : ViewModel() {

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
    fun getCurrentUserData(): LiveData<HomeUiState<UserDataResponse>> = liveData {
        emit(HomeUiState.Loading)

        when (val resp = networkRepository.getUserData(datastoreManager.currentUserId.first())) {
            is ResponseResult.Error -> emit(HomeUiState.Error(resp.message))
            is ResponseResult.Success -> emit(HomeUiState.Success(resp.data))
        }
    }

    /*
        Used when user wants to change password
     */
    fun updateUserData(
        currentPassword: String,
        newPassword: String
    ): LiveData<SettingUiState<Nothing>> = liveData {
        emit(SettingUiState.Loading)
        when (val resp = networkRepository.updateUser(
            datastoreManager.currentUserId.first(),
            currentPassword,
            newPassword
        )) {
            is ResponseResult.Error -> {
                emit(SettingUiState.Error(resp.message))
            }

            is ResponseResult.Success -> {
                emit(SettingUiState.Success(null))
            }
        }
    }

    /*
        Used when user logs out
     */
    fun wipeUserData(): LiveData<SettingUiState<Nothing>> = liveData {
        emit(SettingUiState.Loading)
        datastoreManager.cleanup()
        emit(SettingUiState.Success(null))
    }
}

sealed interface SettingUiState<out T> {
    data object Loading : SettingUiState<Nothing>
    data class Error(val message: String) : SettingUiState<Nothing>
    data class Success<T>(val data: T?) : SettingUiState<T>
}