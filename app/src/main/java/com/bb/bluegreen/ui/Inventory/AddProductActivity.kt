package com.bb.bluegreen.ui.Inventory

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bb.bluegreen.databinding.ActivityAddProductBinding
import com.google.firebase.firestore.FirebaseFirestore

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance() // Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar el botón para seleccionar la imagen
        binding.btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*") // Abre la galería
        }

        // Configurar el botón para agregar producto
        binding.btnAddProduct.setOnClickListener {
            addProduct()
        }
    }

    // Lógica para seleccionar una imagen de la galería
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.ivProductImage.setImageURI(uri)
        }
    }

    private fun addProduct() {
        val name = binding.etProductName.text.toString().trim()
        val barcode = binding.etBarcode.text.toString().trim()
        val price = binding.etPrice.text.toString().toDoubleOrNull()
        val stock = binding.etStock.text.toString().toIntOrNull()  // El stock es Int

        // Validación de campos
        if (name.isBlank() || barcode.isBlank() || price == null || stock == null) {
            Toast.makeText(this, "Por favor, rellena todos los campos correctamente", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear un objeto de producto
        val newProduct = Product(
            id = firestore.collection("products").document().id, // ID automático de Firestore
            name = name,
            barcode = barcode,
            price = price,
            stock = stock,  // El stock es Int
            imageUrl = "https://example.com/placeholder.jpg"
        )

        // Llamar para agregar el producto a Firestore
        addProductToFirestore(newProduct)

        // Llamar a updateStock si necesitas actualizar el stock de un producto existente
        // Si el producto ya existe en Firestore, por ejemplo, actualiza el stock
        // Aquí se puede usar updateStock con el id del producto que ya existe:
        // updateStock(newProduct.id, newProduct.stock)
    }

    // Lógica para agregar el producto a Firestore
    private fun addProductToFirestore(product: Product) {
        val productRef = firestore.collection("products").document(product.id)

        productRef.set(product)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Producto agregado exitosamente", Toast.LENGTH_SHORT).show()
                    finish() // Cerrar la actividad y volver atrás
                } else {
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error al agregar el producto: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Método para actualizar el stock de un producto existente en Firestore
    private fun updateStock(productId: String, newStock: Int) {
        val productRef = firestore.collection("products").document(productId)

        // Actualiza solo el campo stock
        productRef.update("stock", newStock)
            .addOnSuccessListener {
                Toast.makeText(this, "Stock actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                val errorMessage = exception.message ?: "Error desconocido"
                Toast.makeText(this, "Error al actualizar el stock: $errorMessage", Toast.LENGTH_SHORT).show()
            }
    }
}
