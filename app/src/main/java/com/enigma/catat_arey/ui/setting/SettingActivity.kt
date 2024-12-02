package com.enigma.catat_arey.ui.setting

import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.enigma.catat_arey.R
import com.enigma.catat_arey.databinding.ActivitySettingBinding
import com.enigma.catat_arey.util.showCustomDialog

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
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
                    val edCurrPw = dialogView.findViewById<EditText>(R.id.ed_current_pw)
                    val edNewPw = dialogView.findViewById<EditText>(R.id.ed_new_pw)
                    val edConfPw = dialogView.findViewById<EditText>(R.id.ed_confirm_pw)

                    // TODO: Add change password logic here
                    if (edNewPw.text.toString() == edConfPw.text.toString()) {
                        // update pw
                        dialog.setCancelable(true)
                        dialog.dismiss()
                    } else {
                        edConfPw.error = "Password tidak sama"
                    }
                }
            )
        }
    }
}