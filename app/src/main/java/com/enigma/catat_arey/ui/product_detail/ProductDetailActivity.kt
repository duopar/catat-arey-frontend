package com.enigma.catat_arey.ui.product_detail

import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.enigma.catat_arey.R
import com.enigma.catat_arey.databinding.ActivityProductDetailBinding
import com.enigma.catat_arey.util.showCustomDialog

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setTheme(R.style.Theme_CatatArey)
        actionBar?.hide()

        binding = ActivityProductDetailBinding.inflate(layoutInflater)
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

        binding.clEditProduct.setOnClickListener {
            showCustomDialog(
                context = this,
                title = "Catat Perubahan",
                layoutId = R.layout.dialog_edit_product_layout,
                onPositiveAction = { dialogView, dialog ->
                    val edProductOut = dialogView.findViewById<EditText>(R.id.ed_product_out)
                    val edProductIn = dialogView.findViewById<EditText>(R.id.ed_product_in)

                    // TODO: Add product edit logic here
                    if (edProductOut.text.toString().isNotEmpty() && edProductIn.text.toString().isNotEmpty()) {
                        // update product
                        dialog.setCancelable(true)
                        dialog.create().dismiss()
                    } else {
                        edProductOut.error = "Wajib Diisi"
                        edProductIn.error = "Wajib Diisi"
                    }
                }
            )
        }
    }
}