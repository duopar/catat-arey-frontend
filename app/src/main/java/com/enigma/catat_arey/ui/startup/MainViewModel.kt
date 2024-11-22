package com.enigma.catat_arey.ui.startup

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.enigma.catat_arey.data.network.NetworkRepository
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
        TODO: Get token from backend
        in: email, password
        out: MutableLiveData<T> (so the state is reusable)
        Call on email-pass login
     */
    suspend fun getTokenFromLogin(username: String, password: String): String {
        val token = networkRepository.login(username, password)
        if (token != null) {
            networkRepository.updateToken(token)
            return token
        }

        return "getTokenFromLogin Failed"
    }

    suspend fun testAuthApi(): String {
        val username = networkRepository.getUserInfo("u1n0lbIRxsr7sWl1J3cv")

        if (username != null) {
            return username.username
        }

        return "testAuthApi Failed"
    }

    /*
        Decrypt saved user token for later use.
        Call on successful biometric login
     */
    fun getTokenFromDatastore(cipher: Cipher, token: GCMEnvelope): LiveData<LoginState> = liveData {
        try {
            val decToken = AreyCrypto.aesGcmDecrypt(cipher, token)
            emit(LoginState.Finished(decToken.decodeToString()))
        } catch (e: Exception) {
            e.cause?.message?.let { LoginState.Error(it) }?.let { emit(it) }
        }
    }

    /*
        Check if user has logged in before, thus if user can login with biometric.
        Call on Activity start
     */
    fun canBiometricLogin(): Flow<BiometricEligibilityState> = flow {
        emit(BiometricEligibilityState.Loading)
        emit(
            BiometricEligibilityState.Finished(
                datastoreManager.currentUserToken.first().isNotEmpty()
            )
        )
    }

    /*
        Update user token in datastore for future login.
        Call on successful email-pass login
     */
    fun updateUserToken(cipher: Cipher, token: GCMEnvelope): LiveData<UpdateTokenState> = liveData {
        emit(UpdateTokenState.Loading)
        try {
            val encToken = AreyCrypto.aesGcmEncrypt(cipher, token)
            datastoreManager.updateUserToken(encToken.iv + encToken.data)
            emit(UpdateTokenState.Finished)
        } catch (e: Exception) {
            e.cause?.message?.let { UpdateTokenState.Error(it) }?.let { emit(it) }
        }
    }
}

sealed interface BiometricEligibilityState {
    data object Loading : BiometricEligibilityState
    data class Finished(val canBiometricLogin: Boolean) : BiometricEligibilityState
}

sealed interface UpdateTokenState {
    data object Loading : UpdateTokenState
    data class Error(val message: String) : UpdateTokenState
    data object Finished : UpdateTokenState
}

sealed interface LoginState {
    data object Loading : LoginState
    data class Error(val message: String) : LoginState
    data class Finished(val message: String) : LoginState
}