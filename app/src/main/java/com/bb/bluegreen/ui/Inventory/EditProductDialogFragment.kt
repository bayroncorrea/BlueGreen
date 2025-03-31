package com.bb.bluegreen.ui.Inventory

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bb.bluegreen.databinding.DialogEditProductBinding

class EditProductDialogFragment(
    private val product: Product,
    private val onProductUpdated: (Product) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogEditProductBinding
    private lateinit var updatedProduct: Product

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogEditProductBinding.inflate(inflater, container, false)

        // Inicializar el producto con los valores actuales
        updatedProduct = product.copy()

        // Rellenar los campos del formulario con los valores actuales
        binding.productNameEditText.setText(product.name)
        binding.productBarcodeEditText.setText(product.barcode)
        binding.productPriceEditText.setText(product.price.toString())

        // Actualizar el producto cuando se hagan cambios
        binding.saveButton.setOnClickListener {
            val name = binding.productNameEditText.text.toString()
            val barcode = binding.productBarcodeEditText.text.toString()
            val priceText = binding.productPriceEditText.text.toString()

            // Verificar que todos los campos tengan valores válidos
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(barcode) || TextUtils.isEmpty(priceText)) {
                // Aquí podrías mostrar un mensaje de error si lo prefieres
                return@setOnClickListener
            }

            // Intentar convertir el precio a Double
            val price = try {
                priceText.toDouble()
            } catch (e: NumberFormatException) {
                // Aquí puedes manejar el error si no es un número válido
                return@setOnClickListener
            }

            updatedProduct.name = name
            updatedProduct.barcode = barcode
            updatedProduct.price = price

            // Pasar el producto actualizado de vuelta
            onProductUpdated(updatedProduct)

            dismiss()  // Cerrar el diálogo
        }

        return binding.root
    }
}
