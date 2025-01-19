package com.enigma.catat_arey.ui.startup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.enigma.catat_arey.R
import com.enigma.catat_arey.databinding.ActivityMainBinding
import com.enigma.catat_arey.ui.home.HomeActivity
import com.enigma.catat_arey.util.AreyCrypto
import com.enigma.catat_arey.util.AreyCrypto.getBiometricDecryptionCipher
import com.enigma.catat_arey.util.AreyCrypto.getBiometricEncryptionCipher
import com.enigma.catat_arey.util.AreyCrypto.getDefaultDecryptionCipher
import com.enigma.catat_arey.util.AreyCrypto.getDefaultEncryptionCipher
import com.enigma.catat_arey.util.GCMEnvelope
import com.enigma.catat_arey.util.GeneralUtil
import com.enigma.catat_arey.util.LoadingDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.crypto.Cipher

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Biometric operation requirements
    private lateinit var biometricManager: BiometricManager
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: PromptInfo

    // Biometric cryptography operation requirements
    private lateinit var encryptedToken: GCMEnvelope
    private lateinit var tokenCipher: Cipher
    private lateinit var newTokenEnvelope: GCMEnvelope
    private var tokenCipherMode = -1

    // Checking
    private var deviceSupportBiometric = false
    private var canAutoLogin = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(R.style.Theme_CatatArey)
        actionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        promptInfo = PromptInfo.Builder()
            .setTitle("Login Biometrik")
            .setSubtitle("Silahkan gunakan biometrik untuk akses aplikasi")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Batalkan")
            .build()

        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    when (tokenCipherMode) {

                        // Save the token from backend
                        Cipher.ENCRYPT_MODE -> {
                            viewModel.updateUserToken(
                                result.cryptoObject!!.cipher!!,
                                newTokenEnvelope
                            )
                                .observe(this@MainActivity) {
                                    when (it) {
                                        is MainUiState.Error -> {
                                            showToast("[Debug] ERROR - ${it.message}")
                                        }

                                        is MainUiState.Success -> {
                                            showToast("[Debug] Token Saved (biometric)")
                                            moveToHome()
                                        }

                                        MainUiState.Loading -> {
                                        }
                                    }
                                }
                        }

                        // Biometric login, retrieve encrypted token from datastore
                        Cipher.DECRYPT_MODE -> {
                            viewModel.getTokenFromDatastore(
                                result.cryptoObject!!.cipher!!,
                                encryptedToken
                            )
                                .observe(this@MainActivity) {
                                    when (it) {
                                        is MainUiState.Error -> {
                                            setGlobalLoading(false)
                                            showToast("[Debug] ERROR - ${it.message}")
                                        }

                                        is MainUiState.Success -> {
                                            setGlobalLoading(false)
                                            showToast("[Debug] Ok")
                                            if (viewModel.isTokenReencryptionRequired()) {
                                                tokenCipherMode = Cipher.ENCRYPT_MODE
                                                tokenCipher = getBiometricEncryptionCipher()
                                                newTokenEnvelope = GCMEnvelope(
                                                    data = it.data!!.encodeToByteArray(),
                                                    aad = AreyCrypto.getDefaultAAD()
                                                        .encodeToByteArray()
                                                )
                                                if (tokenCipher.iv != null) {
                                                    viewModel.updateUserToken(
                                                        tokenCipher,
                                                        newTokenEnvelope
                                                    ).observe(this@MainActivity) {
                                                        when (it) {
                                                            is MainUiState.Error -> {
                                                                setGlobalLoading(false)
                                                                showToast("[Debug] ${it.message}")
                                                            }

                                                            MainUiState.Loading -> {
                                                                setGlobalLoading(true)
                                                                showToast("[Debug] Loading updateUserToken")
                                                            }

                                                            is MainUiState.Success -> {
                                                                setGlobalLoading(false)
                                                                showToast("[Debug] Ok - reencryption without reprompting")
                                                                moveToHome()
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    biometricPrompt.authenticate(
                                                        promptInfo,
                                                        BiometricPrompt.CryptoObject(tokenCipher)
                                                    )
                                                }
                                            } else {
                                                moveToHome()
                                            }
                                        }

                                        MainUiState.Loading -> {
                                            setGlobalLoading(true)
                                        }
                                    }
                                }
                        }

                        -1 -> Toast.makeText(
                            applicationContext,
                            "[Debug] Authentication successful - Crypto Failure, shouldn't happen.",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            })

        biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "Biometric available")
                deviceSupportBiometric = true
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.d(TAG, "No biometric available")
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.d(TAG, "Biometric currently not available")
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.d(TAG, "Biometric is not set up on this device")
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.d(TAG, "Need security update")
            }

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                Log.d(TAG, "Android version not supported")
            }

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.d(TAG, "Unknown biometric error")
            }
        }

        // use lottie for splashscreen?
        splashScreen.setOnExitAnimationListener {
            runBlocking {
                viewModel.canBiometricLogin().collect { state ->
                    when (state) {
                        is MainUiState.Success -> {
                            canAutoLogin = state.data!!
                            if (canAutoLogin) { // User has logged in before, feasible biometric
                                runBlocking {
                                    val _userToken = viewModel.getUserToken()

                                    encryptedToken = GCMEnvelope(
                                        iv = _userToken.copyOfRange(0, AreyCrypto.GCM_IV_SIZE),
                                        data = _userToken.copyOfRange(
                                            AreyCrypto.GCM_IV_SIZE,
                                            _userToken.size
                                        ),
                                        aad = AreyCrypto.getDefaultAAD().encodeToByteArray()
                                    )
                                }

                                tokenCipherMode = Cipher.DECRYPT_MODE

                                if (deviceSupportBiometric) { // Device can do biometric
                                    tokenCipher =
                                        getBiometricDecryptionCipher(encryptedToken.iv)
                                    biometricPrompt.authenticate(
                                        promptInfo,
                                        BiometricPrompt.CryptoObject(tokenCipher)
                                    )
                                } else {
                                    tokenCipher =
                                        getDefaultDecryptionCipher(encryptedToken.iv)
                                    viewModel.getTokenFromDatastore(
                                        tokenCipher,
                                        encryptedToken
                                    )
                                        .observe(this@MainActivity) {
                                            when (it) {
                                                is MainUiState.Error -> {
                                                    setGlobalLoading(false)
                                                    showToast("[Debug] ERROR - ${it.message}")
                                                }

                                                is MainUiState.Success -> {
                                                    setGlobalLoading(false)
                                                    showToast("[Debug] Ok (non-biometric)")
                                                    if (viewModel.isTokenReencryptionRequired()) {
                                                        tokenCipherMode = Cipher.ENCRYPT_MODE
                                                        tokenCipher = getDefaultEncryptionCipher()
                                                        val accessEnvelope = GCMEnvelope(
                                                            data = it.data!!.encodeToByteArray(),
                                                            aad = AreyCrypto.getDefaultAAD()
                                                                .encodeToByteArray()
                                                        )
                                                        viewModel.updateUserToken(
                                                            tokenCipher,
                                                            accessEnvelope
                                                        )
                                                    }
                                                    moveToHome()
                                                }

                                                MainUiState.Loading -> {
                                                    setGlobalLoading(true)
                                                }
                                            }
                                        }
                                }
                            }
                            it.remove()
                        }

                        MainUiState.Loading -> { /* Keep splashscreen on screen */
                        }

                        is MainUiState.Error -> {
                            showToast("[Debug] error - $it")
                        }
                    }
                }
            }
        }

        setupListener()
    }

    private fun setupListener() {
        binding.btnLogin.setOnClickListener {
            binding.btnLogin.isEnabled = false

            val username = binding.edName.text.toString()
            val password = binding.edPw.text.toString()

            if (username.isEmpty() || username.isEmpty()) {
                binding.btnLogin.isEnabled = true
                showToast("Fields can't be empty.")
            } else {
                viewModel.getTokenFromLogin(username, password).observe(this) { it ->
                    when (it) {
                        is MainUiState.Error -> {
                            setGlobalLoading(false)
                            showToast(it.message)
                            binding.btnLogin.isEnabled = true
                        }

                        is MainUiState.Success -> {
                            setGlobalLoading(false)

                            val data = (it.data as List<String>)
                            val accessToken = data[0]
                            val refreshToken = data[1]

                            val userToken =
                                GeneralUtil.createUserTokenString(accessToken, refreshToken)

                            tokenCipherMode = Cipher.ENCRYPT_MODE

                            newTokenEnvelope = GCMEnvelope(
                                data = userToken.encodeToByteArray(),
                                aad = AreyCrypto.getDefaultAAD().encodeToByteArray()
                            )

                            if (deviceSupportBiometric) { // Device can do biometric
                                tokenCipher = getBiometricEncryptionCipher()
                                biometricPrompt.authenticate(
                                    promptInfo,
                                    BiometricPrompt.CryptoObject(tokenCipher)
                                )
                            } else {
                                tokenCipher = getDefaultEncryptionCipher()
                                viewModel.updateUserToken(tokenCipher, newTokenEnvelope)
                                    .observe(this@MainActivity) {
                                        when (it) {
                                            is MainUiState.Error -> {
                                                setGlobalLoading(false)
                                                showToast("[Debug] ERROR - ${it.message}")
                                            }

                                            is MainUiState.Success -> {
                                                setGlobalLoading(false)
                                                showToast("[Debug] Token Saved (non-biometric)")
                                                moveToHome()
                                            }

                                            MainUiState.Loading -> {
                                                setGlobalLoading(true)
                                            }
                                        }
                                    }
                            }

                            binding.btnLogin.isEnabled = true
                        }

                        MainUiState.Loading -> {
                            setGlobalLoading(true)
                        }
                    }
                }
            }
        }
    }

    private fun moveToHome() {
        Intent(this, HomeActivity::class.java).run {
            startActivity(this)
        }
        finishAffinity()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setGlobalLoading(state: Boolean) {
        if (state) {
            LoadingDialog.showLoading(this)
        } else {
            LoadingDialog.hideLoading()
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}