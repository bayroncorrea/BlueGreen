package com.bb.bluegreen.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bb.bluegreen.ui.Inventory.Product
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // _totalStock should store the total stock of all products in the inventory
    private val _totalStock = MutableLiveData(0)  // Default to 0 as Int
    val totalStock: MutableLiveData<Int> get() = _totalStock

    private val _lowStockAlert = MutableLiveData("")
    val lowStockAlert: LiveData<String> get() = _lowStockAlert

    private val _lowStockProducts = MutableLiveData<List<Product>>(emptyList())
    val lowStockProducts: LiveData<List<Product>> get() = _lowStockProducts

    private var inventoryList: List<Product> = listOf()

    // Function to load products from Firebase and update total stock
    fun loadInventoryFromFirebase() {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Debug: ver datos crudos
                querySnapshot.documents.forEach { doc ->
                    Log.d("FirestoreData", "Document ID: ${doc.id}")
                    Log.d("FirestoreData", "Data: ${doc.data}")
                    Log.d("FirestoreData", "Stock raw value: ${doc.get("stock")}")
                }

                // Mapeo manual para mejor control
                val products = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        Product(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            barcode = doc.getString("barcode") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            stock = doc.getLong("stock")?.toInt() ?: 0, // Conversión explícita
                            imageUrl = doc.getString("imageUrl") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("ProductMapping", "Error mapping document ${doc.id}", e)
                        null
                    }
                }

                // Debug: ver productos mapeados
                products.forEach { product ->
                    Log.d("ProductDebug", "Mapped product: ${product.name} - Stock: ${product.stock}")
                }

                updateInventory(products)
                val totalStockValue = products.sumOf { it.stock }
                Log.d("TotalStock", "Calculated total: $totalStockValue")
                updateStock(totalStockValue)
            }
            .addOnFailureListener { exception ->
                Log.e("HomeViewModel", "Error fetching products", exception)
            }
    }

    // Function to update the total stock value
    private fun updateStock(stock: Int) {
        _totalStock.value = stock
        checkLowStock() // Check if any products are low on stock after updating the total
    }

    // Function to update the inventory list
    private fun updateInventory(products: List<Product>) {
        inventoryList = products
        checkLowStock() // Check for low stock after updating the inventory list
    }

    // Function to check for low stock products
    private fun checkLowStock() {
        val lowStockList = inventoryList.filter { it.stock < 5 } // Filter products with stock < 5
        _lowStockProducts.value = lowStockList

        _lowStockAlert.value = if (lowStockList.isNotEmpty()) {
            "¡Alerta! Algunos productos tienen stock bajo."
        } else {
            ""
        }
    }
}
