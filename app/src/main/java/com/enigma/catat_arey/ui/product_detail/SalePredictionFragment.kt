package com.enigma.catat_arey.ui.product_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.enigma.catat_arey.databinding.FragmentSalePredictionBinding
import com.enigma.catat_arey.util.ErrorPopupDialog
import com.enigma.catat_arey.util.GeneralUtil.createForecastList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SalePredictionFragment : Fragment() {
    private var _binding: FragmentSalePredictionBinding? = null
    private val binding get() = _binding!!

    private val productViewModel: ProductDetailViewModel by activityViewModels<ProductDetailViewModel>()
    private lateinit var productForecastAdapter: ProductForecastAdapter
    private lateinit var productId: String

    private var onStop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productId = arguments?.getString(ProductDetailActivity.EXTRA_PRODUCT_ID)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSalePredictionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()

        if (onStop) {
            displayForecast()
            onStop = false
        }
    }

    override fun onStop() {
        super.onStop()

        onStop = true
    }

    private fun setupRecyclerView() {
        productForecastAdapter = ProductForecastAdapter()
        binding.rvForecast.layoutManager = LinearLayoutManager(requireActivity())
        binding.rvForecast.adapter = productForecastAdapter

        displayForecast()
    }

    private fun displayForecast() {
        viewLifecycleOwner.lifecycleScope.launch {
            productViewModel.productForecast.collect {
                when (it) {
                    is ProductDetailUiState.Error -> {
                        binding.wholeLoadingIndicator.visibility = View.INVISIBLE

                        val msg = it.message

                        if (msg.lowercase().contains("network error")) {
                            showNetworkError {
                                displayForecast()
                            }
                        } else if (msg.lowercase().contains("no inventory logs found")) {
                            binding.tvRvMessage.text = "Data Belum Cukup untuk Fitur Prediksi."
                        } else {
                            showToast("Error: ${it.message}")
                        }
                    }

                    ProductDetailUiState.Loading -> {
                        binding.wholeLoadingIndicator.visibility = View.VISIBLE
                    }

                    is ProductDetailUiState.Success -> {
                        binding.wholeLoadingIndicator.visibility = View.INVISIBLE

                        if (it.data != null) {
                            val data = it.data[0]
                            val forecasts = createForecastList(data)

                            if (forecasts.isEmpty()) {
                                binding.tvRvMessage.visibility = View.VISIBLE
                                binding.tvRvMessage.text = "Data Belum Cukup untuk Fitur Prediksi."
                            } else {
                                binding.tvRvMessage.visibility = View.GONE
                            }

                            productForecastAdapter.submitList(forecasts)
                            productForecastAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showNetworkError(onButtonClick: () -> Unit) {
        ErrorPopupDialog.hideError()
        ErrorPopupDialog.showError(
            context = requireActivity(),
            title = "Kesalahan Jaringan",
            message = "Silahkan coba lagi atau hubungi administrator jika berkelanjutan.",
            buttonText = "Coba Lagi",
            onButtonClick = onButtonClick
        )
    }
}