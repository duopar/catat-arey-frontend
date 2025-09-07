package com.enigma.catat_arey.ui.product_detail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ProductDetailSectionsAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    private lateinit var productId: String

    override fun getItemCount(): Int = 2;

    override fun createFragment(position: Int): Fragment {
        lateinit var fragment: Fragment
        when (position) {
            0 -> fragment = SalePredictionFragment()
            1 -> fragment = SaleHistoryFragment()
        }
        fragment.arguments = Bundle().apply {
            putString(ProductDetailActivity.EXTRA_PRODUCT_ID, productId)
        }
        return fragment
    }

    fun setProductId(productId: String? = null) {
        if (!productId.isNullOrBlank()) {
            this.productId = productId
        }
    }
}