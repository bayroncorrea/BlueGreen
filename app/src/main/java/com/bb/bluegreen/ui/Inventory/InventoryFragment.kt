package com.bb.bluegreen.ui.Inventory

import android.app.AlertDialog
import android.content.Intent
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

    private var productList: MutableList<Product> = mutableListOf()
    private var filteredProductList: MutableList<Product> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().reference.child("products")

        // Configurar RecyclerView y Adaptador
        setupRecyclerView()

        // Configurar SearchView
        setupSearchView()

        // Obtener productos desde Firebase
        fetchProductsFromFirebase()

        // Navegar a la pantalla para agregar productos
        binding.fabAdd.setOnClickListener {
            navigateToAddProductScreen()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(mutableListOf())

        // Listener para editar un producto
        adapter.setOnEditClickListener { product ->
            val editDialog = EditProductDialogFragment(product) { updatedProduct ->
                updateProductInFirebase(updatedProduct)
            }
            editDialog.show(childFragmentManager, "EditProductDialog")
        }

        // Listener para eliminar un producto
        adapter.setOnDeleteClickListener { product ->
            showDeleteConfirmationDialog(product)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filtrar productos por nombre o código de barras
                val filteredList = productList.filter {
                    it.name.contains(newText ?: "", ignoreCase = true) ||
                            it.barcode.contains(newText ?: "", ignoreCase = true)
                }

                // Actualizar la lista filtrada y notificar al adaptador
                filteredProductList.clear()
                filteredProductList.addAll(filteredList)
                adapter.updateList(filteredProductList)
                return true
            }
        })
    }

    private fun fetchProductsFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                val newProductList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let {
                        newProductList.add(it)
                        Log.d("Firebase", "Producto cargado: ${it.name}")
                    }
                }
                // Actualizar las listas con los productos cargados
                productList.addAll(newProductList)
                filteredProductList.clear()
                filteredProductList.addAll(productList)

                // Actualizar el adaptador
                adapter.updateList(filteredProductList)
                Log.d("Firebase", "Número de productos cargados: ${productList.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al cargar los productos", error.toException())
            }
        })
    }

    private fun navigateToAddProductScreen() {
        val activity = requireActivity()
        activity.startActivity(
            Intent(activity, AddProductActivity::class.java)
        )
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar este producto?")

        builder.setPositiveButton("Aceptar") { dialog, _ ->
            deleteProductFromFirebase(product)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun deleteProductFromFirebase(product: Product) {
        database.child(product.id).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firebase", "Producto eliminado exitosamente")
                    adapter.removeProduct(product)
                } else {
                    Log.e("Firebase", "Error al eliminar el producto", task.exception)
                }
            }
    }

    private fun updateProductInFirebase(updatedProduct: Product) {
        database.child(updatedProduct.id).setValue(updatedProduct)
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
