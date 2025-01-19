package com.enigma.catat_arey.ui.product_detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.enigma.catat_arey.databinding.ItemProductDaysPredictBinding

class ProductForecastAdapter :
    RecyclerView.Adapter<ProductForecastAdapter.ProductForecastViewHolder>() {

    private var forecasts = ArrayList<ProductForecast>()

    inner class ProductForecastViewHolder(val binding: ItemProductDaysPredictBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(day: String, date: String, predictedValue: Int, isRestock: Boolean) {
            with(binding) {
                tvDay.text = day
                tvStockOutInt.text = predictedValue.toString()
                tvDate.text = date
                if (!isRestock) restockFlagLayout.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductForecastViewHolder {
        val binding = ItemProductDaysPredictBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductForecastViewHolder(binding)
    }

    override fun getItemCount(): Int = forecasts.size

    override fun onBindViewHolder(holder: ProductForecastViewHolder, position: Int) {
        val current = forecasts[position]
        holder.bind(current.day, current.date, current.predictedSale, current.isRestock)
    }

    fun submitList(list: List<ProductForecast>?) {
        this.forecasts.clear()
        if (list != null) {
            if (list.isNotEmpty()) {
                this.forecasts.addAll(list)
            }
        }
    }
}

data class ProductForecast(
    val day: String,
    val date: String,
    val predictedSale: Int,
    val isRestock: Boolean,
    val epoch: Long
)