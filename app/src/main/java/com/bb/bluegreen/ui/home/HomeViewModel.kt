package com.bb.bluegreen.ui.Home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bb.bluegreen.ui.Inventory.Product

class HomeViewModel : ViewModel() {

    private val _totalStock = MutableLiveData(0)
    val totalStock: LiveData<Int> get() = _totalStock

    private val _lowStockAlert = MutableLiveData("")
    val lowStockAlert: LiveData<String> get() = _lowStockAlert

    private val _lowStockProducts = MutableLiveData<List<Product>>()
    val lowStockProducts: LiveData<List<Product>> get() = _lowStockProducts

    private var inventoryList: List<Product> = listOf()  // Se inicializa la lista de productos

    fun updateStock(stock: Int) {
        _totalStock.value = stock
        checkLowStock() // Llamamos a la función para verificar el stock bajo
    }

    fun updateInventory(products: List<Product>) {
        inventoryList = products
        checkLowStock() // Verificamos el stock bajo después de actualizar la lista
    }

    private fun checkLowStock() {
        val lowStockList = inventoryList.filter { it.stock < 5 }  // Filtra productos con stock < 5
        _lowStockProducts.value = lowStockList

        _lowStockAlert.value = if (lowStockList.isNotEmpty()) {
            "¡Alerta! Algunos productos tienen stock bajo."
        } else {
            ""
        }
    }
}
