package com.bb.bluegreen.ui.Inventory

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bb.bluegreen.databinding.ActivityAddProductBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var database: DatabaseReference
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar referencia a Firebase
        database = FirebaseDatabase.getInstance().reference.child("products")

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
            selectedImageUri = uri // Guarda el URI de la imagen seleccionada
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

        // Si no se selecciona una imagen, usa una imagen por defecto
        val imageUri = selectedImageUri?.toString() ?: "https://example.com/placeholder.jpg"

        // Crear un objeto de producto
        val newProduct = Product(
            id = database.push().key ?: return, // Verifica que key no sea null
            name = name,
            barcode = barcode,
            price = price,
            stock = stock,  // El stock es Int
            imageUrl = imageUri
        )

        addProductToFirebase(newProduct)
    }

    // Lógica para agregar el producto a Firebase
    private fun addProductToFirebase(product: Product) {
        database.child(product.id).setValue(product)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Producto agregado exitosamente", Toast.LENGTH_SHORT).show()
                    finish() // Cerrar la actividad y volver atrás
                } else {
                    // Mejorar el manejo de errores
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error al agregar el producto: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
