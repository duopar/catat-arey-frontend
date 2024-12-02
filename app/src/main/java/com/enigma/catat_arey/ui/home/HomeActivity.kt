package com.enigma.catat_arey.ui.home

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.enigma.catat_arey.R
import com.enigma.catat_arey.data.network.ProductsDataResponse
import com.enigma.catat_arey.databinding.ActivityHomeBinding
import com.enigma.catat_arey.ui.setting.SettingActivity
import com.enigma.catat_arey.util.showCustomDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var productListAdapter: ProductListAdapter
    private val viewModel: HomeViewModel by viewModels()

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
        setupRecyclerView()

        viewModel.getCurrentUserData().observe(this) {
            when (it) {
                is HomeUiState.Error -> showToast("[Debug] Error: ${it.message}")
                HomeUiState.Loading -> {}
                is HomeUiState.Success -> {
                    showToast("[Debug] Logged in: ${it.data!!.username}")
                }
            }
        }

        lifecycleScope.launch {
            viewModel.allProducts.collect { result ->
                when (result) {
                    is HomeUiState.Error -> {
                        binding.loadingProductList.visibility = View.GONE
                        showToast("[Debug] $result")
                    }

                    HomeUiState.Loading -> binding.loadingProductList.visibility = View.VISIBLE
                    is HomeUiState.Success -> {
                        binding.loadingProductList.visibility = View.GONE
                        displayProductList(result.data!!)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.retrieveAllProducts()
    }

    private fun setupRecyclerView() {
        productListAdapter = ProductListAdapter { product ->
            showToast("[Debug] ${product.name}")
        }
        binding.rvProductList.layoutManager = LinearLayoutManager(this)
        binding.rvProductList.adapter = productListAdapter
    }

    private fun displayProductList(data: List<ProductsDataResponse>) {
        productListAdapter.submitList(data)
        productListAdapter.notifyDataSetChanged()
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
                    var goodInput = true

                    val edProductName =
                        dialogView.findViewById<EditText>(R.id.ed_product_name).text.toString()
                    val edProductCategory =
                        dialogView.findViewById<EditText>(R.id.ed_product_category).text.toString()
                    val edProductPrice =
                        dialogView.findViewById<EditText>(R.id.ed_product_price).text.toString()
                    val edProductStock =
                        dialogView.findViewById<EditText>(R.id.ed_product_stock).text.toString()
                    val edProductRestock =
                        dialogView.findViewById<EditText>(R.id.ed_product_restock_threshold).text.toString()

                    if (edProductName.isEmpty() || edProductCategory.isEmpty() || edProductPrice.isEmpty() || edProductStock.isEmpty() || edProductRestock.isEmpty()) {
                        showToast("Silahkan isi semua data")
                        goodInput = false
                    }

                    if (!properNumber(edProductPrice) || !properNumber(edProductStock) || !properNumber(
                            edProductRestock
                        )
                    ) {
                        showToast("Data angka harus positif")
                        goodInput = false
                    }

                    if (goodInput) {
                        viewModel.addNewProduct(
                            edProductName,
                            edProductCategory,
                            edProductPrice,
                            edProductStock,
                            edProductRestock
                        ).observe(this) {
                            when (it) {
                                is HomeUiState.Error -> showToast("[Debug] Error - $it")
                                HomeUiState.Loading -> {}
                                is HomeUiState.Success -> {
                                    showToast("[Debug] Ok - ${it.data!!.productId}")
                                    viewModel.retrieveAllProducts()
                                    dialog.dismiss()
                                }
                            }
                        }
                    }
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

    private fun properNumber(input: String): Boolean {
        return input.isNotBlank() &&
                input.contains(Regex("^\\d+$")) &&
                input.toIntOrNull()?.let { it > 0 } ?: false
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}