package com.bb.bluegreen.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bb.bluegreen.R
import com.bb.bluegreen.databinding.ItemInventoryBinding
import com.bb.bluegreen.ui.Inventory.Product
import java.text.DecimalFormat

class LowStockAdapter(private var products: List<Product>) :
    RecyclerView.Adapter<LowStockAdapter.LowStockViewHolder>() {

    class LowStockViewHolder(private val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.productName.text = product.name
            binding.productBarcode.text = binding.root.context.getString(R.string.barcode_text, product.barcode)

            // Usar DecimalFormat para dar formato al precio
            val decimalFormat = DecimalFormat("#.##")
            binding.productPrice.text = binding.root.context.getString(R.string.price_text, decimalFormat.format(product.price))
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

    // Actualizar la lista de productos con bajo stock
    fun updateProducts(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged() // Notificar el cambio para actualizar el RecyclerView
    }
}