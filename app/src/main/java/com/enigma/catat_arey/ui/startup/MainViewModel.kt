package com.enigma.catat_arey.ui.startup

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.enigma.catat_arey.data.network.NetworkRepository
import com.enigma.catat_arey.data.network.ResponseResult
import com.enigma.catat_arey.data.preferences.DatastoreManager
import com.enigma.catat_arey.util.AreyCrypto
import com.enigma.catat_arey.util.GCMEnvelope
import com.enigma.catat_arey.util.GeneralUtil
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

    private var reencryptTokenRequired = false

    /*
        Get encrypted access token from datastore (preferences).
     */
    suspend fun getUserToken(): ByteArray {
        return datastoreManager.currentUserToken.first()
    }

    /*
        If access token expired, need to get a new one and encrypt it
     */
    fun isTokenReencryptionRequired(): Boolean {
        return reencryptTokenRequired
    }

    /*
        Call on email-pass login
     */
    fun getTokenFromLogin(username: String, password: String): LiveData<MainUiState<List<String>>> =
        liveData {
            emit(MainUiState.Loading)

            when (val resp = networkRepository.login(username, password)) {
                is ResponseResult.Error -> {
                    emit(MainUiState.Error(resp.message))
                }

                is ResponseResult.Success -> {
                    val data = resp.data!!
                    datastoreManager.updateUserId(data.userId)
                    datastoreManager.updateUserTokenExpiry(GeneralUtil.getJwtExpiry(data.accessToken))
                    datastoreManager.updateRefreshTokenExpiry(GeneralUtil.getJwtExpiry(data.refreshToken))
                    networkRepository.updateToken(data.accessToken)
                    emit(MainUiState.Success(listOf(data.accessToken, data.refreshToken)))
                }
            }
        }

    /*
        Decrypt saved user token for later use.
        Call on successful biometric login
     */
    fun getTokenFromDatastore(
        cipher: Cipher,
        userToken: GCMEnvelope
    ): LiveData<MainUiState<String>> =
        liveData {
            emit(MainUiState.Loading)
            try {
                var token = GeneralUtil.getUserToken(
                    AreyCrypto.aesGcmDecrypt(cipher, userToken).decodeToString()
                )
                var accessToken = token.accessToken
                val refreshToken = token.refreshToken

                // If Access token expired, refresh
                if (System.currentTimeMillis() / 1000 > GeneralUtil.getJwtExpiry(accessToken)) {
                    Log.d(
                        "MainViewModel",
                        "Access Token Expired ${GeneralUtil.getJwtExpiry(accessToken)}."
                    )
                    when (val refresh = networkRepository.refreshToken(refreshToken)) {
                        is ResponseResult.Error -> {
                            Log.d("MainViewModel", refresh.message)
                            emit(MainUiState.Error(refresh.message))
                        }

                        is ResponseResult.Success -> {
                            accessToken = refresh.data!!.accessToken
                            reencryptTokenRequired = true
                            networkRepository.updateToken(accessToken)
                            emit(
                                MainUiState.Success(
                                    GeneralUtil.createUserTokenString(
                                        accessToken,
                                        refreshToken
                                    )
                                )
                            )
                        }
                    }
                } else {
                    networkRepository.updateToken(accessToken)
                    emit(
                        MainUiState.Success(
                            GeneralUtil.createUserTokenString(
                                accessToken,
                                refreshToken
                            )
                        )
                    )
                }

            } catch (e: Exception) {
                e.cause?.message?.let { MainUiState.Error(it) }?.let { emit(it) }
            }
        }

    /*
        Check if user has logged in before, thus if user can login with biometric.
        Call on Activity start
     */
    fun canBiometricLogin(): Flow<MainUiState<Boolean>> = flow {
        emit(MainUiState.Loading)
        val token = datastoreManager.currentUserToken.first()

        if (token.isEmpty()) {
            emit(MainUiState.Success(false))
        } else {
            emit(
                MainUiState.Success(
                    (System.currentTimeMillis() / 1000 < datastoreManager.currentUserTokenExpiry.first()) ||
                            (System.currentTimeMillis() / 1000 < datastoreManager.currentRefreshTokenExpiry.first())
                )
            )
        }
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
                Log.d("MainViewModel", e.stackTraceToString())
                e.cause?.message?.let { MainUiState.Error(it) }?.let { emit(it) }
            }
        }
}

sealed interface MainUiState<out T> {
    data object Loading : MainUiState<Nothing>
    data class Error(val message: String) : MainUiState<Nothing>
    data class Success<T>(val data: T?) : MainUiState<T>
}