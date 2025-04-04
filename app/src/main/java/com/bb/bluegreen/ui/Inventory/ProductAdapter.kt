package com.bb.bluegreen.ui.Inventory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bb.bluegreen.R
import com.bumptech.glide.Glide

class ProductAdapter(
    private var productList: MutableList<Product> = mutableListOf()
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var onEditClickListener: ((Product) -> Unit)? = null
    private var onDeleteClickListener: ((Product) -> Unit)? = null

    // Set listener for edit button
    fun setOnEditClickListener(listener: (Product) -> Unit) {
        onEditClickListener = listener
    }

    // Set listener for delete button
    fun setOnDeleteClickListener(listener: (Product) -> Unit) {
        onDeleteClickListener = listener
    }

    // Add method to remove product from the list
    fun removeProduct(product: Product) {
        val position = productList.indexOf(product)
        if (position != -1) {
            productList.removeAt(position)  // Remove the product from the list
            notifyItemRemoved(position)     // Notify the adapter that an item was removed
        }
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.productName)
        val barcode: TextView = itemView.findViewById(R.id.productBarcode)
        val price: TextView = itemView.findViewById(R.id.productPrice)
        val stock: TextView = itemView.findViewById(R.id.productstock)
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val editButton: ImageView = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageView = itemView.findViewById(R.id.btnDelete)  // New delete button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.name.text = product.name
        holder.barcode.text = "Código: ${product.barcode}"
        holder.price.text = "Precio: $${product.price}"
        holder.stock.text = "Stock: ${product.stock}"

        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(holder.productImage)

        // Edit button listener
        holder.editButton.setOnClickListener {
            onEditClickListener?.invoke(product)
        }

        // Delete button listener
        holder.deleteButton.setOnClickListener {
            onDeleteClickListener?.invoke(product)
        }
    }

    override fun getItemCount(): Int = productList.size

    // Método para actualizar la lista de productos sin DiffUtil
    fun updateList(newList: List<Product>) {
        productList.clear()
        productList.addAll(newList)
        notifyDataSetChanged() // Notificar que la lista ha cambiado
    }
}
