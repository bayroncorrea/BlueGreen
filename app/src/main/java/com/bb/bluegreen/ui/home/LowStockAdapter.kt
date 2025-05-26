package com.bb.bluegreen.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bb.bluegreen.R
import com.bb.bluegreen.databinding.ItemInventoryBinding
import com.bb.bluegreen.ui.Inventory.Product
import com.bumptech.glide.Glide
import java.text.DecimalFormat

class LowStockAdapter(private var productList: List<Product>,
                      private val showActions: Boolean = true) :
    RecyclerView.Adapter<LowStockAdapter.LowStockViewHolder>() {

    class LowStockViewHolder(private val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product, showActions: Boolean) {
            binding.productName.text = product.name
            binding.productBarcode.text =
                binding.root.context.getString(R.string.barcode_text, product.barcode)

            val decimalFormat = DecimalFormat("#.##")
            binding.productPrice.text = binding.root.context.getString(
                R.string.price_text,
                decimalFormat.format(product.price)
            )

            if (product.imageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(product.imageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error)
                    .into(binding.productImage)
            } else {
                binding.productImage.setImageResource(R.drawable.placeholder)
            }
            binding.editButton.visibility = if (showActions) View.VISIBLE else View.GONE
            binding.btnDelete.visibility = if (showActions) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LowStockViewHolder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LowStockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LowStockViewHolder, position: Int) {
        holder.bind(productList[position], showActions)
    }

    override fun getItemCount(): Int = productList.size

    fun updateProducts(newList: List<Product>) {
        productList = newList
        notifyDataSetChanged()
    }
}