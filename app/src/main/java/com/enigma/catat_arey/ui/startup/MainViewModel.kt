package com.enigma.catat_arey.ui.startup

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.enigma.catat_arey.data.network.NetworkRepository
import com.enigma.catat_arey.data.network.ResponseResult
import com.enigma.catat_arey.data.preferences.DatastoreManager
import com.enigma.catat_arey.util.AreyCrypto
import com.enigma.catat_arey.util.GCMEnvelope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.crypto.Cipher
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val datastoreManager: DatastoreManager,
    private val networkRepository: NetworkRepository
) : ViewModel() {

    /*
        Get encrypted user token from datastore (preferences).
        Might be deleted later
     */
    suspend fun getUserToken(): ByteArray {
        return datastoreManager.currentUserToken.first()
    }

    /*
        Call on email-pass login
     */
    fun getTokenFromLogin(username: String, password: String): LiveData<MainUiState<String>> =
        liveData {
            emit(MainUiState.Loading)

            when (val resp = networkRepository.login(username, password)) {
                is ResponseResult.Error -> {
                    emit(MainUiState.Error(resp.message))
                }

                is ResponseResult.Success -> {
                    val data = resp.data
                    datastoreManager.updateUserId(data.userId)
                    networkRepository.updateToken(data.token)
                    emit(MainUiState.Success(data.token))
                }
            }
        }

    /*
        Decrypt saved user token for later use.
        Call on successful biometric login
     */
    fun getTokenFromDatastore(cipher: Cipher, token: GCMEnvelope): LiveData<MainUiState<String>> =
        liveData {
            emit(MainUiState.Loading)
            try {
                val decToken = AreyCrypto.aesGcmDecrypt(cipher, token).decodeToString()
                networkRepository.updateToken(decToken)
                emit(MainUiState.Success(decToken))
            } catch (e: Exception) {
                e.cause?.message?.let { MainUiState.Error(it) }?.let { emit(it) }
            }
        }

    /*
        Check if user has logged in before, thus if user can login with biometric.
        Call on Activity start

        TODO: Check token validity, refresh if expired
     */
    fun canBiometricLogin(): Flow<MainUiState<Boolean>> = flow {
        emit(MainUiState.Loading)
        emit(
            MainUiState.Success(
                datastoreManager.currentUserToken.first().isNotEmpty()
            )
        )
    }

    /*
        Update user token in datastore for future login.
        Call on successful email-pass login
     */
    fun updateUserToken(cipher: Cipher, token: GCMEnvelope): LiveData<MainUiState<Nothing>> =
        liveData {
            emit(MainUiState.Loading)
            try {
                val encToken = AreyCrypto.aesGcmEncrypt(cipher, token)
                datastoreManager.updateUserToken(encToken.iv + encToken.data)
                emit(MainUiState.Success(null))
            } catch (e: Exception) {
                e.cause?.message?.let { MainUiState.Error(it) }?.let { emit(it) }
            }
        }
}

sealed interface MainUiState<out T> {
    data object Loading : MainUiState<Nothing>
    data class Error(val message: String) : MainUiState<Nothing>
    data class Success<T>(val data: T?) : MainUiState<T>
}