package com.enigma.catat_arey.ui.product_detail

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.enigma.catat_arey.data.network.InventoryLogResponse
import com.enigma.catat_arey.databinding.ItemProductHistoryBinding
import com.enigma.catat_arey.util.GeneralUtil

class ProductHistoryAdapter :
    RecyclerView.Adapter<ProductHistoryAdapter.ProductHistoryViewHolder>() {

    private var logs = ArrayList<InventoryLogResponse>()

    inner class ProductHistoryViewHolder(val binding: ItemProductHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(logs: InventoryLogResponse) {
            with(binding) {
                tvDate.text = GeneralUtil.epochLongToFormattedDate(logs.createdAt.secondsInEpoch)
                tvProductQuantity.text = "${logs.stockChange} PCS"

                when (logs.changeType) {
                    "stockOut" -> {
                        tvActionType.setTextColor(Color.parseColor("#f54242"))
                        tvActionType.text = "Stock Out"
                    }

                    "stockIn" -> {
                        tvActionType.setTextColor(Color.parseColor("#5af542"))
                        tvActionType.text = "Stock In"
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductHistoryViewHolder {
        val binding = ItemProductHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductHistoryViewHolder(binding)
    }

    override fun getItemCount(): Int = logs.size

    override fun onBindViewHolder(holder: ProductHistoryViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    fun submitList(list: List<InventoryLogResponse>?) {
        this.logs.clear()
        if (list != null) {
            if (list.isNotEmpty()) {
                this.logs.addAll(list.sortedWith(compareByDescending { it.createdAt.secondsInEpoch }))
            }
        }
    }
}