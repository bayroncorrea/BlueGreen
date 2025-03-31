package com.bb.bluegreen.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bb.bluegreen.R
import com.bb.bluegreen.databinding.ItemInventoryBinding
import com.bb.bluegreen.ui.Inventory.Product

class LowStockAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<LowStockAdapter.LowStockViewHolder>() {

    class LowStockViewHolder(private val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.productName.text = product.name
            binding.productBarcode.text =
                binding.root.context.getString(R.string.barcode_text, product.barcode)
            binding.productPrice.text =
                binding.root.context.getString(R.string.price_text, product.price.toString())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LowStockViewHolder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LowStockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LowStockViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size
}
