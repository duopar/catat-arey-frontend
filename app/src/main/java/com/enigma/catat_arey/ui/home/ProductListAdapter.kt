package com.enigma.catat_arey.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.enigma.catat_arey.data.network.ProductsDataResponse
import com.enigma.catat_arey.databinding.ItemProductBinding
import com.enigma.catat_arey.util.GeneralUtil.toFormatterCurrency

class ProductListAdapter(
    private val onClick: (ProductsDataResponse) -> Unit,
    private val onLongPress: (ProductsDataResponse) -> Unit
) : RecyclerView.Adapter<ProductListAdapter.ProductListViewHolder>() {

    private var products = ArrayList<ProductsDataResponse>()

    inner class ProductListViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductsDataResponse) {
            with(binding) {
                tvProductPrice.text = "Rp. " + product.price.toFormatterCurrency()
                tvProductName.text = product.name

                val stock = product.stockLevel
                var stockStr = stock.toString()

                if (stock > 100) {
                    stockStr = "100+"
                }

                tvProductQuantity.text = "$stockStr Pcs"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductListViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductListViewHolder(binding)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: ProductListViewHolder, position: Int) {
        holder.bind(products[position])
        holder.binding.root.setOnClickListener {
            onClick.invoke(products[position])
        }
        holder.binding.root.setOnLongClickListener {
            onLongPress.invoke(products[position])
            true
        }
    }

    fun submitList(list: List<ProductsDataResponse>?) {
        this.products.clear()
        if (list != null) {
            if (list.isNotEmpty()) {
                this.products.addAll(list)
            }
        }
    }
}