package com.enigma.catat_arey.ui.product_detail

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.enigma.catat_arey.R
import com.enigma.catat_arey.data.network.ProductDataResponse
import com.enigma.catat_arey.databinding.ActivityProductDetailBinding
import com.enigma.catat_arey.ui.home.HomeUiState
import com.enigma.catat_arey.ui.startup.MainActivity
import com.enigma.catat_arey.util.AreyUserRole
import com.enigma.catat_arey.util.ErrorPopupDialog
import com.enigma.catat_arey.util.GeneralUtil
import com.enigma.catat_arey.util.GeneralUtil.isProperPositiveNumber
import com.enigma.catat_arey.util.LoadingDialog
import com.enigma.catat_arey.util.showCustomDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    private val viewModel: ProductDetailViewModel by viewModels()

    private lateinit var productId: String
    private lateinit var currentProduct: ProductDataResponse

    private lateinit var userRole: AreyUserRole
    private lateinit var tabLayoutMediator: TabLayoutMediator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setTheme(R.style.Theme_CatatArey)
        actionBar?.hide()

        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val _pid = intent.getStringExtra(EXTRA_PRODUCT_ID)

        if (_pid.isNullOrEmpty()) {
            showToast("Null product id, shouldn't happen")
            finish()
        } else {
            productId = _pid
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUserData()
        setupUI()
        setupListener()
        setupFragment()
    }

    override fun onResume() {
        super.onResume()

        runBlocking {
            if (viewModel.shouldRefreshApp()) {
                gracefulExit()
            }
        }

        viewModel.getForecast(productId)
        viewModel.getInventoryLogs(productId)
    }

    private fun setupFragment() {
        val adapter = ProductDetailSectionsAdapter(this)
        binding.vpRoot.adapter = adapter

        adapter.setProductId(productId)

        tabLayoutMediator =
            TabLayoutMediator(binding.tabsProductDetail, binding.vpRoot) { tab, pos ->
                tab.text = resources.getString(TAB_TITLES[pos])
                TooltipCompat.setTooltipText(tab.view, null)
            }

        tabLayoutMediator.attach()
    }

    private fun setupUserData() {
        viewModel.getCurrentUserData().observe(this) {
            when (it) {
                is HomeUiState.Error -> {
                }

                HomeUiState.Loading -> {}
                is HomeUiState.Success -> {
                    val user = it.data!!

                    userRole = GeneralUtil.getUserRole(user.role)
                }
            }
        }
    }

    private fun setupUI() {
        viewModel.getProductData(productId).observe(this) {
            when (it) {
                is ProductDetailUiState.Error -> {
                    // binding.wholeLoadingIndicator.visibility = View.INVISIBLE
                    // showToast(it.message)
                }

                ProductDetailUiState.Loading -> {
                    binding.topAppBar.title = "-"
                    binding.tvSumTotal.text = "-"
                    binding.tvSumIn.text = "-"
                    binding.tvSumOut.text = "-"
                }

                is ProductDetailUiState.Success -> {
                    val product = it.data!!
                    currentProduct = product

                    binding.topAppBar.title = currentProduct.name
                    binding.tvSumTotal.text = currentProduct.stockLevel.toString()
                    binding.tvSumIn.text = currentProduct.stockInToday.toString()
                    binding.tvSumOut.text = currentProduct.stockOutToday.toString()
                }
            }
        }

        viewModel.getForecast(productId)
        viewModel.getInventoryLogs(productId)
    }

    private fun setupListener() {
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.delete_setting -> {
                    if (userRole != AreyUserRole.Owner) {
                        showUnauthorizedAccessError()
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(currentProduct.name)
                            .setMessage("Delete produk?")
                            .setNegativeButton("Cancel") { dialog, which ->
                                dialog.dismiss()
                            }
                            .setPositiveButton("Yes") { dialog, which ->
                                viewModel.deleteProduct(productId).observe(this) {
                                    when (it) {
                                        is ProductDetailUiState.Error -> {
                                            setGlobalLoading(false)

                                            val msg = it.message

                                            if (msg.lowercase().contains("network error")) {
                                                showNetworkError {}
                                            } else {
                                                showToast("Error: ${it.message}")
                                            }
                                        }

                                        ProductDetailUiState.Loading -> {
                                            setGlobalLoading(true)
                                        }

                                        is ProductDetailUiState.Success -> {
                                            setGlobalLoading(false)
                                            showToast("Produk berhasil dihapus.")
                                            finish()
                                        }
                                    }
                                }
                            }
                            .show()
                    }
                    true
                }

                R.id.edit_setting -> {
                    if (userRole != AreyUserRole.Owner) {
                        showUnauthorizedAccessError()
                    } else {
                        showCustomDialog(
                            context = this,
                            title = "Ubah Produk",
                            layoutId = R.layout.dialog_edit_product_detail_layout,
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
                                    viewModel.updateProduct(
                                        productId,
                                        edProductName,
                                        edProductCategory,
                                        edProductPrice,
                                        edProductStock,
                                        edProductRestock
                                    ).observe(this) {
                                        when (it) {
                                            is ProductDetailUiState.Error -> {
                                                val msg = it.message

                                                if (msg.lowercase().contains("network error")) {
                                                    showNetworkError({})
                                                } else {
                                                    showToast("[Debug] Error - ${it.message}")
                                                }

                                            }

                                            ProductDetailUiState.Loading -> {}
                                            is ProductDetailUiState.Success -> {
                                                showToast("Produk berhasil diubah.")
                                                setupUI()
                                                dialog.dismiss()
                                            }
                                        }
                                    }
                                }
                            },
                            onDialogViewCreated = { dialogView ->
                                dialogView.findViewById<EditText>(R.id.ed_product_name)
                                    ?.setText(currentProduct.name)
                                dialogView.findViewById<EditText>(R.id.ed_product_category)
                                    ?.setText(currentProduct.category)
                                dialogView.findViewById<EditText>(R.id.ed_product_price)
                                    ?.setText(currentProduct.price.toString())
                                dialogView.findViewById<EditText>(R.id.ed_product_stock)
                                    ?.setText(currentProduct.stockLevel.toString())
                                dialogView.findViewById<EditText>(R.id.ed_product_restock_threshold)
                                    ?.setText(currentProduct.restockThreshold.toString())

                                dialogView.findViewById<EditText>(R.id.ed_product_name).isEnabled =
                                    false
                                dialogView.findViewById<EditText>(R.id.ed_product_name).isClickable =
                                    false
                            }
                        )
                    }
                    true
                }

                else -> true
            }
        }

        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.clEditProduct.setOnClickListener {
            showCustomDialog(
                context = this,
                title = "Catat Entri Produk",
                layoutId = R.layout.dialog_edit_product_layout,
                onPositiveAction = { dialogView, dialog ->
                    val edProductOut = dialogView.findViewById<EditText>(R.id.ed_product_out)
                    val edProductIn = dialogView.findViewById<EditText>(R.id.ed_product_in)

                    val isEdProductInValid = edProductIn.validateStockInput(isInputPositive = true)
                    val isEdProductOutValid = edProductOut.validateStockInput(
                        isInputPositive = true,
                        maxStock = currentProduct.stockLevel
                    )

                    if (isEdProductInValid && isEdProductOutValid) {
                        viewModel.entryProductLog(
                            productId,
                            edProductIn.text.toString(),
                            edProductOut.text.toString()
                        ).observe(this) {
                            when (it) {
                                is ProductDetailUiState.Error -> {
                                    setGlobalLoading(false)
                                    val msg = it.message

                                    if (msg.lowercase().contains("network error")) {
                                        showNetworkError {}
                                    } else {
                                        showToast("Error: ${it.message}")
                                    }
                                }

                                ProductDetailUiState.Loading -> {
                                    setGlobalLoading(true)
                                }

                                is ProductDetailUiState.Success -> {
                                    setGlobalLoading(false)
                                    showToast("Entri produk berhasil.")
                                    setupUI()
                                    dialog.dismiss()
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    private fun EditText.validateStockInput(
        isInputPositive: Boolean = false,
        maxStock: Int? = null
    ): Boolean {
        error = when {
            text.isNullOrEmpty() -> {
                "Tidak boleh kosong"
            }

            isInputPositive && !isProperPositiveNumber(text.toString()) -> {
                "Harus angka positif"
            }

            maxStock != null && text.toString().toInt() > maxStock -> {
                "Produk keluar tidak boleh melewati batas stok"
            }

            else -> null
        }
        return error == null
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        ErrorPopupDialog.hideError()
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

    companion object {
        @StringRes
        private val TAB_TITLES = intArrayOf(
            R.string.sales_prediction,
            R.string.sales_history,
        )

        val EXTRA_PRODUCT_ID = "productId"
    }
}