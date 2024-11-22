package com.enigma.catat_arey.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.enigma.catat_arey.R
import com.enigma.catat_arey.databinding.ActivityHomeBinding
import com.enigma.catat_arey.ui.setting.SettingActivity
import com.enigma.catat_arey.util.showCustomDialog

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setTheme(R.style.Theme_CatatArey)
        actionBar?.hide()
        binding = ActivityHomeBinding.inflate(layoutInflater)
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

        binding.clAddProduct.setOnClickListener {
            showCustomDialog(
                context = this,
                title = "Tambah Produk",
                layoutId = R.layout.dialog_add_product_layout,
                onPositiveAction = { dialogView, dialog ->
                    val edProductName = dialogView.findViewById<EditText>(R.id.ed_product_name)
                    val edProductPrice = dialogView.findViewById<EditText>(R.id.ed_product_price)
                    val edProductStock = dialogView.findViewById<EditText>(R.id.ed_product_stock)

                    // TODO: Add product edit logic here

                }
            )
        }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_setting -> {
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }
}