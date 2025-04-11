package com.bb.bluegreen.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bb.bluegreen.ui.Inventory.Product
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var lowStockThreshold = 5 // Umbral configurable para bajo stock

    private val _totalStock = MutableLiveData(0)
    val totalStock: LiveData<Int> get() = _totalStock

    private val _lowStockAlert = MutableLiveData("")
    val lowStockAlert: LiveData<String> get() = _lowStockAlert

    private val _lowStockProducts = MutableLiveData<List<Product>>(emptyList())
    val lowStockProducts: LiveData<List<Product>> get() = _lowStockProducts

    private var inventoryList: List<Product> = listOf()

    fun loadInventoryFromFirebase() {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("FirestoreDebug", "Documentos recibidos: ${querySnapshot.documents.size}")

                val products = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val stock = doc.getDouble("stock")?.toInt() ?: 0
                        Log.d("ProductDebug", "Producto: ${doc.getString("name")} - Stock: $stock")

                        Product(
                            id = doc.id,
                            name = doc.getString("name") ?: "Sin nombre",
                            barcode = doc.getString("barcode") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            stock = stock,
                            imageUrl = doc.getString("imageUrl") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("MappingError", "Error con documento ${doc.id}", e)
                        null
                    }
                }

                Log.d("InventoryDebug", "Productos mapeados: ${products.size}")
                updateInventory(products)
                calculateTotalStock(products)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error al cargar productos", e)
                _totalStock.value = 0 // Resetear a 0 en caso de error
            }
    }

    private fun calculateTotalStock(products: List<Product>) {
        val total = products.sumOf { it.stock }
        Log.d("StockDebug", "Productos encontrados: ${products.size}")
        Log.d("StockDebug", "Stock total calculado: $total")
        _totalStock.value = total
        checkLowStock()
    }

    private fun updateInventory(products: List<Product>) {
        inventoryList = products
        checkLowStock()
    }

    private fun checkLowStock() {
        val lowStockList = inventoryList.filter { it.stock < lowStockThreshold }
        _lowStockProducts.value = lowStockList

        Log.d("LowStockDebug", "Productos con bajo stock: ${lowStockList.size}")

        _lowStockAlert.value = when {
            lowStockList.any { it.stock == 0 } -> "¡Alerta! Tienes productos agotados"
            lowStockList.isNotEmpty() -> "¡Atención! Algunos productos tienen stock bajo"
            else -> ""
        }
    }

    // Para actualizar el umbral de bajo stock si es necesario
    fun setLowStockThreshold(threshold: Int) {
        lowStockThreshold = threshold
        checkLowStock()
    }
}