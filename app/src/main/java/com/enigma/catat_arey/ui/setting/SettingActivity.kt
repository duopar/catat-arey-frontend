package com.enigma.catat_arey.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.enigma.catat_arey.R
import com.enigma.catat_arey.databinding.ActivitySettingBinding
import com.enigma.catat_arey.ui.home.HomeUiState
import com.enigma.catat_arey.ui.startup.MainActivity
import com.enigma.catat_arey.util.ErrorPopupDialog
import com.enigma.catat_arey.util.GeneralUtil.isValidPassword
import com.enigma.catat_arey.util.LoadingDialog
import com.enigma.catat_arey.util.showCustomDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private val viewModel: SettingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setTheme(R.style.Theme_CatatArey)
        actionBar?.hide()

        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListener()

        viewModel.getCurrentUserData().observe(this) {
            when (it) {
                is HomeUiState.Error -> {
                    setGlobalLoading(false)
                    showToast(it.message)
                }

                HomeUiState.Loading -> {
                    setGlobalLoading(true)
                    binding.btnLogout.isEnabled = false
                    binding.btnChangePw.isEnabled = false
                }

                is HomeUiState.Success -> {
                    setGlobalLoading(false)
                    binding.btnLogout.isEnabled = true
                    binding.btnChangePw.isEnabled = true
                    binding.loadingIndicator.visibility = View.INVISIBLE
                    binding.tvGreetings.text =
                        getString(R.string.setting_greeting, it.data!!.username)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        runBlocking {
            if (viewModel.shouldRefreshApp()) {
                gracefulExit()
            }
        }
    }

    private fun setupListener() {
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnChangePw.setOnClickListener {
            showCustomDialog(
                context = this,
                title = "Ubah Password",
                layoutId = R.layout.dialog_change_pw_layout,
                onPositiveAction = { dialogView, dialog ->
                    val invalidPw =
                        "Password harus terdiri dari 8-30 karakter dan menyertakan setidaknya satu huruf besar, satu huruf kecil, satu angka, dan satu karakter khusus (!@#\$%^&*)"

                    val edCurrPw = dialogView.findViewById<EditText>(R.id.ed_current_pw)
                    val edNewPw = dialogView.findViewById<EditText>(R.id.ed_new_pw)
                    val edConfPw = dialogView.findViewById<EditText>(R.id.ed_confirm_pw)

                    val currPw = edCurrPw.text.toString()
                    val newPw = edNewPw.text.toString()

                    if (edCurrPw.validatePasswordInput(invalidPw) &&
                        edNewPw.validatePasswordInput(invalidPw) &&
                        edConfPw.validatePasswordInput(
                            invalidPw,
                            isMatchCheck = true,
                            matchValue = newPw
                        )
                    ) {
                        viewModel.updateUserData(currPw, newPw).observe(this) {
                            when (it) {
                                is SettingUiState.Error -> {
                                    setGlobalLoading(false)

                                    val msg = it.message

                                    if (msg.lowercase().contains("network error")) {
                                        showNetworkError { }
                                    } else {
                                        showToast("Error: ${it.message}")
                                    }
                                }

                                SettingUiState.Loading -> {
                                    setGlobalLoading(true)
                                }

                                is SettingUiState.Success -> {
                                    setGlobalLoading(false)
                                    showToast("Password berhasil diganti.")
                                    viewModel.wipeUserData()
                                    dialog.dismiss()
                                    moveToLogin()
                                }
                            }
                        }
                    }
                }
            )
        }

        binding.btnLogout.setOnClickListener {
            viewModel.wipeUserData().observe(this) {
                when (it) {
                    is SettingUiState.Error -> {
                        setGlobalLoading(false)

                        val msg = it.message

                        if (msg.lowercase().contains("network error")) {
                            showNetworkError { }
                        } else {
                            showToast("Error: ${it.message}")
                        }
                    }

                    SettingUiState.Loading -> {
                        setGlobalLoading(true)
                    }

                    is SettingUiState.Success -> {
                        setGlobalLoading(false)
                        showToast("Berhasil Log Out.")
                        moveToLogin()
                    }
                }
            }
        }
    }

    private fun EditText.validatePasswordInput(
        errorMessage: String,
        isMatchCheck: Boolean = false,
        matchValue: String? = null
    ): Boolean {
        error = when {
            text.isNullOrEmpty() -> {
                "Field ini tidak boleh kosong"
            }

            !isValidPassword(text.toString()) -> {
                errorMessage
            }

            isMatchCheck && text.toString() != matchValue -> {
                "Password harus sama"
            }

            else -> null
        }
        return error == null
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun moveToLogin() {
        Intent(this, MainActivity::class.java).run {
            startActivity(this)
            finishAffinity()
        }
    }

    private fun showNetworkError(onButtonClick: () -> Unit) {
        ErrorPopupDialog.showError(
            context = this,
            title = "Kesalahan Jaringan",
            message = "Silahkan coba lagi atau hubungi administrator jika berkelanjutan.",
            buttonText = "Coba Lagi",
            onButtonClick = onButtonClick
        )
    }

    private fun gracefulExit() {
        ErrorPopupDialog.showError(
            context = this,
            title = "Sesi Berakhir",
            message = "Silahkan lakukan login kembali atau mulai ulang aplikasi.",
            buttonText = "Tutup",
            onButtonClick = {
                Intent(this, MainActivity::class.java).run {
                    startActivity(this)
                }
                finishAffinity()
            }
        )
    }

    private fun setGlobalLoading(state: Boolean) {
        if (state) {
            LoadingDialog.showLoading(this)
        } else {
            LoadingDialog.hideLoading()
        }
    }
}