package com.bb.bluegreen.ui.Inventory

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bb.bluegreen.databinding.DialogEditProductBinding
import com.bumptech.glide.Glide

class EditProductDialogFragment(
    private val product: Product, private val onProductUpdated: (Product) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogEditProductBinding
    private lateinit var updatedProduct: Product
    private var selectedImageUri: Uri? = null

    companion object {
        private const val REQUEST_SELECT_IMAGE = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogEditProductBinding.inflate(inflater, container, false)
        updatedProduct = product.copy()

        // Rellenar campos con valores actuales del producto
        binding.etProductName.setText(product.name)
        binding.etBarcode.setText(product.barcode)
        binding.etPrice.setText(product.price.toString())
        binding.etStock.setText(product.stock.toString())

        // Cargar imagen si existe
        if (product.imageUrl.isNotEmpty()) {
            Glide.with(this).load(product.imageUrl).into(binding.ivProductImage)
        }

        // Botón para seleccionar nueva imagen
        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_SELECT_IMAGE)
        }

        // Botón para guardar cambios
        binding.btnUpdateProduct.setOnClickListener {
            val name = binding.etProductName.text.toString().trim()
            val barcode = binding.etBarcode.text.toString().trim()
            val priceText = binding.etPrice.text.toString().trim()
            val stockText = binding.etStock.text.toString().trim()

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(barcode) || TextUtils.isEmpty(priceText) || TextUtils.isEmpty(
                    stockText
                )
            ) {
                // Puedes mostrar un mensaje de error si deseas
                return@setOnClickListener
            }

            val price = try {
                priceText.toDouble()
            } catch (e: NumberFormatException) {
                return@setOnClickListener
            }

            val stock = try {
                stockText.toInt()
            } catch (e: NumberFormatException) {
                return@setOnClickListener
            }

            // Actualizar producto con los nuevos valores
            updatedProduct.name = name
            updatedProduct.barcode = barcode
            updatedProduct.price = price
            updatedProduct.stock = stock
            updatedProduct.ownerId = product.ownerId
            updatedProduct.createdAt = product.createdAt



            // Si se seleccionó una nueva imagen, actualizar imageUrl (puedes subirla a Firebase después)
            selectedImageUri?.let {
                updatedProduct.imageUrl =
                    it.toString()  // ¡OJO! Aquí deberías subir la imagen y obtener su URL real
            }

            onProductUpdated(updatedProduct)
            Toast.makeText(requireContext(), "Producto actualizado con éxito", Toast.LENGTH_SHORT)
                .show()
            dismiss()
        }

        return binding.root
    }

    // Recibir imagen seleccionada desde la galería
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let {
                Glide.with(this).load(it).into(binding.ivProductImage)
            }
        }
    }
}
