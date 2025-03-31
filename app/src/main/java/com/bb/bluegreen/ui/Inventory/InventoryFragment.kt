package com.bb.bluegreen.ui.Inventory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bb.bluegreen.databinding.FragmentInventoryBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().reference

        // Configurar RecyclerView y adaptador
        setupRecyclerView()

        // Escuchar cambios en la base de datos
        fetchProductsFromFirebase()

        binding.fabAdd.setOnClickListener {
            addProduct()  // Verifica que este método se está llamando
        }

        return binding.root
    }

    private fun fetchProductsFromFirebase() {
        database.child("products").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let {
                        productList.add(it)
                        Log.d("Firebase", "Producto cargado: ${it.name}")
                    }
                }
                Log.d("Firebase", "Número de productos cargados: ${productList.size}")
                adapter.updateList(productList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al cargar los productos", error.toException())
            }
        })
    }

    private fun addProduct() {
        val newProduct = Product(
            id = database.child("products").push().key ?: return,
            name = "Producto ${adapter.itemCount + 1}",
            barcode = "000000${adapter.itemCount + 1}",
            price = (10..100).random().toDouble(),
            stock = (1..50).random(),
            imageUrl = "https://www.example.com/product_image.jpg"
        )
        addProductToFirebase(newProduct)
    }

    private fun addProductToFirebase(product: Product) {
        database.child("products").child(product.id).setValue(product)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firebase", "Producto agregado exitosamente")
                } else {
                    Log.e("Firebase", "Error al agregar el producto", task.exception)
                }
            }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(mutableListOf())
        adapter.setOnEditClickListener { product ->
            val editDialog = EditProductDialogFragment(product) { updatedProduct ->
                updateProductInFirebase(updatedProduct)
            }
            editDialog.show(childFragmentManager, "EditProductDialog")
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun updateProductInFirebase(updatedProduct: Product) {
        database.child("products").child(updatedProduct.id).setValue(updatedProduct)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firebase", "Producto actualizado exitosamente")
                } else {
                    Log.e("Firebase", "Error al actualizar el producto", task.exception)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
