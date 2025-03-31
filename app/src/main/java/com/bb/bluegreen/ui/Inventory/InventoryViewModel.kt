package com.bb.bluegreen.ui.Inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class InventoryViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> get() = _products
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        fetchProductsFromFirebase() // Escucha cambios en tiempo real
    }

    fun addProduct(product: Product) {
        db.collection("products").document(product.id)
            .set(product)
            .addOnSuccessListener {
                // Agregar solo el nuevo producto sin recargar todo
                val currentList = _products.value?.toMutableList() ?: mutableListOf()
                currentList.add(product)
                _products.value = currentList
            }
            .addOnFailureListener { e ->
                println("Error al agregar producto: ${e.message}")
            }
    }

    fun updateProduct(product: Product) {
        db.collection("products").document(product.id)
            .set(product)  // Reemplazará todo el documento con los nuevos datos
            .addOnSuccessListener {
                println("Producto actualizado correctamente")
            }
            .addOnFailureListener { e ->
                println("Error al actualizar producto: ${e.message}")
            }
    }

    private fun fetchProductsFromFirebase() {
        _isLoading.value = true
        db.collection("products").addSnapshotListener { result, error ->
            _isLoading.value = false // Ocultar el loading al terminar

            if (error != null) {
                println("Error al obtener productos: ${error.message}")
                return@addSnapshotListener
            }

            if (result != null) {
                val productList = result.map { doc -> doc.toObject(Product::class.java) }
                _products.value = productList
            }
        }
    }

    fun searchProducts(query: String): List<Product> {
        return _products.value?.filter {
            it.name.contains(query, ignoreCase = true) || it.barcode.contains(query, ignoreCase = true)
        } ?: emptyList()
    }
}
