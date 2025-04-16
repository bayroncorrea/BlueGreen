package com.bb.bluegreen.ui.Inventory

import com.google.firebase.firestore.PropertyName

data class Product(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("barcode") @set:PropertyName("barcode")
    var barcode: String = "",

    @get:PropertyName("price") @set:PropertyName("price")
    var price: Double = 0.0,

    @get:PropertyName("stock") @set:PropertyName("stock")
    var stock: Int = 0,

    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    val createdAt: Long = System.currentTimeMillis()
) {
    // Constructor sin parámetros requerido por Firestore
    constructor() : this("", "", "", 0.0, 0, "", 0)
}