package com.bb.bluegreen.ui.Inventory

data class Product(
    val id: String = "",
    var name: String = "",
    var barcode: String = "",
    var price: Double = 0.0,
    var stock: Int = 0,
    var imageUrl: String = ""
)
