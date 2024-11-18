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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.enigma.catat_arey.R
import com.enigma.catat_arey.databinding.ActivityMainBinding
import com.enigma.catat_arey.ui.home.HomeActivity
import com.enigma.catat_arey.util.AreyCrypto
import com.enigma.catat_arey.util.AreyCrypto.getBiometricDecryptionCipher
import com.enigma.catat_arey.util.AreyCrypto.getBiometricEncryptionCipher
import com.enigma.catat_arey.util.GCMEnvelope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private lateinit var userToken: GCMEnvelope
    private lateinit var biometricCipher: Cipher
    private var biometricCipherMode = -1

    // Checking
    private var deviceSupportBiometric = false
    private var canBiometricLogin = true

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

                    when (biometricCipherMode) {

                        // Save the token from backend
                        Cipher.ENCRYPT_MODE -> {
                            val env = GCMEnvelope(
                                data = viewModel.getTokenFromLogin().encodeToByteArray(),
                                aad = AreyCrypto.getDefaultAAD().encodeToByteArray()
                            )

                            viewModel.updateUserToken(result.cryptoObject!!.cipher!!, env)
                                .observe(this@MainActivity) {
                                    when (it) {
                                        is UpdateTokenState.Error -> {
                                            Toast.makeText(
                                                applicationContext,
                                                "ERROR - ${it.message}", Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        UpdateTokenState.Finished -> {
                                            binding.btnBiometricLogin.isEnabled = true
                                            binding.btnTestLogin.isEnabled = true
                                            Toast.makeText(
                                                applicationContext,
                                                "FINISHED", Toast.LENGTH_LONG
                                            ).show()
                                            recreate()
                                        }

                                        UpdateTokenState.Loading -> {
                                            binding.btnBiometricLogin.isEnabled = false
                                            binding.btnTestLogin.isEnabled = false
                                        }
                                    }
                                }
                        }

                        // Biometric login, retrieve encrypted token from datastore
                        Cipher.DECRYPT_MODE -> {
                            viewModel.getTokenFromDatastore(
                                result.cryptoObject!!.cipher!!,
                                userToken
                            )
                                .observe(this@MainActivity) {
                                    when (it) {
                                        is LoginState.Error -> {
                                            Toast.makeText(
                                                applicationContext,
                                                "ERROR - ${it.message}", Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        is LoginState.Finished -> {
                                            binding.btnBiometricLogin.isEnabled = true
                                            binding.btnTestLogin.isEnabled = true
                                            Toast.makeText(
                                                applicationContext,
                                                it.message, Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        LoginState.Loading -> {
                                            binding.btnBiometricLogin.isEnabled = false
                                            binding.btnTestLogin.isEnabled = false
                                        }
                                    }
                                }
                        }

                        -1 -> Toast.makeText(
                            applicationContext,
                            "Authentication successful - Crypto Failure, shouldn't happen.",
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
                        is BiometricEligibilityState.Finished -> {
                            canBiometricLogin = state.canBiometricLogin
                            it.remove()
                        }

                        BiometricEligibilityState.Loading -> { /* Keep splashscreen on screen */
                        }
                    }
                }
            }
        }

        setupListener()
    }

    private fun setupListener() {
        binding.btnBiometricLogin.setOnClickListener {
            if (deviceSupportBiometric) { // Device can do biometric
                if (canBiometricLogin) { // User has logged in before, feasible biometric
                    runBlocking {
                        val _userToken = viewModel.getUserToken()
                        userToken = GCMEnvelope(
                            iv = _userToken.copyOfRange(0, AreyCrypto.GCM_IV_SIZE),
                            data = _userToken.copyOfRange(AreyCrypto.GCM_IV_SIZE, _userToken.size),
                            aad = AreyCrypto.getDefaultAAD().encodeToByteArray()
                        )
                    }

                    biometricCipherMode = Cipher.DECRYPT_MODE
                    biometricCipher = getBiometricDecryptionCipher(userToken.iv)
                    biometricPrompt.authenticate(
                        promptInfo,
                        BiometricPrompt.CryptoObject(biometricCipher)
                    )
                } else { // User hasn't log in before, need written credential login
                    biometricCipherMode = Cipher.ENCRYPT_MODE
                    showAlertDialog(
                        "Biometric",
                        "Silahkan login untuk pertama kali agar dapat menggunakan fitur biometrik kedepannya."
                    )
                }
            } else { // Device can't do biometric
                showAlertDialog(
                    "Biometric",
                    "Perangkat yang digunakan tidak support biometric atau belum ada biometric yang terdaftar."
                )
            }
        }

        binding.btnTestLogin.setOnClickListener {
            if (deviceSupportBiometric) { // Device can do biometric
                biometricCipherMode = Cipher.ENCRYPT_MODE
                biometricCipher = getBiometricEncryptionCipher()
                biometricPrompt.authenticate(
                    promptInfo,
                    BiometricPrompt.CryptoObject(biometricCipher)
                )
            } else { // Device can't do biometric
                showAlertDialog(
                    "Biometric",
                    "Yahaha gapunya biometric, login biasa ajah (IN-PROGRESS)"
                )
            }
        }

        binding.btnLogin.setOnClickListener{
            val intent =
                Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showAlertDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Tutup") { _, _ ->
                closeOptionsMenu()
            }
            .show()
    }

    companion object {
        const val TAG = "MainActivity"
    }
}