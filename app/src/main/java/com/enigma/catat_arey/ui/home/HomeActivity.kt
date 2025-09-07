package com.enigma.catat_arey.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.enigma.catat_arey.ui.product_detail.ProductDetailActivity
import com.enigma.catat_arey.ui.product_detail.ProductDetailActivity.Companion.EXTRA_PRODUCT_ID
import com.enigma.catat_arey.ui.setting.SettingActivity
import com.enigma.catat_arey.ui.startup.MainActivity
import com.enigma.catat_arey.util.AreyUserRole
import com.enigma.catat_arey.util.ErrorPopupDialog
import com.enigma.catat_arey.util.GeneralUtil
import com.enigma.catat_arey.util.GeneralUtil.isProperPositiveNumber
import com.enigma.catat_arey.util.showCustomDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var productListAdapter: ProductListAdapter
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var userRole: AreyUserRole

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
        setupUserData()
        setupRecyclerView()

        lifecycleScope.launch {
            viewModel.allProducts.collect { result ->
                when (result) {
                    is HomeUiState.Error -> {
                        binding.loadingProductList.visibility = View.GONE
                        if (result.message.contains("No products found")) {
                            binding.tvProductListError.text = "Produk tidak ditemukan."
                            animateCrossFade(binding.tvProductListError, binding.rvProductList)
                        } else {
                            animateCrossFade(binding.tvProductListError, binding.rvProductList)
                            binding.rvProductList.visibility = View.INVISIBLE
                            binding.tvProductListError.text = ""
                            if (result.message.lowercase().contains("network error")) {
                                showNetworkError({
                                    viewModel.retrieveAllProducts(null)
                                    setupUserData()
                                })
                            } else {
                                showToast("[Debug] Error: ${result.message}")
                            }
                        }
                    }

                    HomeUiState.Loading -> binding.loadingProductList.visibility = View.VISIBLE

                    is HomeUiState.Success -> {
                        animateCrossFade(binding.rvProductList, binding.tvProductListError)
                        binding.loadingProductList.visibility = View.GONE
                        displayProductList(result.data!!)
                    }
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

        viewModel.retrieveAllProducts(null)

        viewModel.getInventoryLogs().observe(this) {
            when (it) {
                is HomeUiState.Error -> {
                }

                HomeUiState.Loading -> {
                    binding.tvSumIn.text = "-"
                    binding.tvSumOut.text = "-"
                }

                is HomeUiState.Success -> {
                    val data = it.data!!
                    val sum = GeneralUtil.getDailySum(data)

                    binding.tvSumIn.text = sum.stockIn.toString()
                    binding.tvSumOut.text = sum.stockOut.toString()
                }
            }
        }
    }

    private fun setupUserData() {
        viewModel.getCurrentUserData().observe(this) {
            when (it) {
                is HomeUiState.Error -> {
                }

                HomeUiState.Loading -> {}
                is HomeUiState.Success -> {
                    val user = it.data!!

                    // showToast("[Debug] Logged in: ${user.username}")
                    userRole = GeneralUtil.getUserRole(user.role)

                    if (userRole != AreyUserRole.Owner) {
                        binding.clAddProduct.background.setTint(resources.getColor(R.color.greyedOut))
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        productListAdapter = ProductListAdapter(
            onClick = { product ->
                Intent(this, ProductDetailActivity::class.java).run {
                    putExtra(EXTRA_PRODUCT_ID, product.productId)
                    startActivity(this)
                }
            },
            onLongPress = { product ->

            }
        )
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

        binding.edSearchProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.retrieveAllProducts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })

        binding.clAddProduct.setOnClickListener {
            if (userRole != AreyUserRole.Owner) {
                showUnauthorizedAccessError()
            } else {
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

                        if (!isProperPositiveNumber(edProductPrice) || !isProperPositiveNumber(
                                edProductStock
                            ) || !isProperPositiveNumber(
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
                                    is HomeUiState.Error -> {
                                        val msg = it.message

                                        if (msg.lowercase().contains("network error")) {
                                            showNetworkError({})
                                        } else {
                                            showToast("[Debug] Error - ${it.message}")
                                        }

                                    }

                                    HomeUiState.Loading -> {}
                                    is HomeUiState.Success -> {
                                        showToast("Produk berhasil ditambah.")
                                        viewModel.retrieveAllProducts(null)
                                        dialog.dismiss()
                                    }
                                }
                            }
                        }
                    }
                )
            }
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

    private fun animateCrossFade(viewIn: View, viewOut: View) {
        viewIn.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration((250).toLong())
                .setListener(null)
        }

        viewOut.animate()
            .alpha(0f)
            .setDuration((250).toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    viewOut.visibility = View.INVISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    viewOut.visibility = View.INVISIBLE
                }
            })
    }

    private fun showUnauthorizedAccessError() {
        ErrorPopupDialog.showError(
            context = this,
            title = "Aksi Tidak Diizinkan",
            message = "Hanya Owner yang dapat melakukan aksi ini.",
            buttonText = "Tutup"
        )
    }

    private fun showNetworkError(onButtonClick: () -> Unit) {
        ErrorPopupDialog.showError(
            context = this,
            title = "Kesalahan Jaringan",
            message = "Silahkan coba lagi atau hubungi admin aplikasi jika berkelanjutan.",
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}