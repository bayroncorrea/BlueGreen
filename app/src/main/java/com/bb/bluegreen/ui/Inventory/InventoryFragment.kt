package com.bb.bluegreen.ui.Inventory

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bb.bluegreen.databinding.FragmentInventoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: ProductAdapter

    private var productList: MutableList<Product> = mutableListOf()
    private var filteredProductList: MutableList<Product> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()

        // Botón de ordenamiento
        binding.btnSort.setOnClickListener {
            showSortOptions()
        }

        // Configurar RecyclerView y Adaptador
        setupRecyclerView()

        // Configurar SearchView
        setupSearchView()

        // Obtener productos desde Firestore
        fetchProductsFromFirestore()

        // FAB para agregar productos
        binding.fabAdd.setOnClickListener {
            navigateToAddProductScreen()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ajuste de FAB según barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabAdd) { fab, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 16
                marginEnd = 16
            }
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(mutableListOf(), showActions = true)

        adapter.setOnEditClickListener { product ->
            val editDialog = EditProductDialogFragment(product) { updatedProduct ->
                updateProductInFirestore(updatedProduct)
            }
            editDialog.show(childFragmentManager, "EditProductDialog")
        }

        adapter.setOnDeleteClickListener { product ->
            showDeleteConfirmationDialog(product)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.apply {
            // Configurar el SearchView para que esté expandido por defecto
            isIconified = false
            setIconifiedByDefault(false)

            // Mostrar teclado cuando la búsqueda se toque
            setOnQueryTextFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    post {
                        val imm =
                            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                                    as android.view.inputmethod.InputMethodManager
                        imm.showSoftInput(
                            this,
                            android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
                        )
                    }
                }
            }

            // Expande el SearchView al tocar
            setOnClickListener {
                if (isIconified) {
                    isIconified = false
                    requestFocus()
                }
            }

            // Configurar la lógica de búsqueda
            setOnQueryTextListener(object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    val filteredList = productList.filter {
                        it.name.contains(newText ?: "", ignoreCase = true) ||
                                it.barcode.contains(newText ?: "", ignoreCase = true)
                    }

                    filteredProductList.clear()
                    filteredProductList.addAll(filteredList)
                    adapter.updateList(filteredProductList)
                    return true
                }
            })

            // Permitir cerrar el SearchView manualmente (cuando se toca la X)
            setOnCloseListener {
                // Cuando se cierra, eliminar el foco y colapsarlo
                clearFocus()
                isIconified = true
                false // Dejar que el SearchView haga su trabajo
            }
        }
    }

    private fun fetchProductsFromFirestore() {
        Log.d("Firestore", "Iniciando carga de productos...")
        val currentUser = FirebaseAuth.getInstance().currentUser
        val ownerId = currentUser?.uid
        Log.d("Firestore", "Usuario actual UID: $ownerId")

        firestore.collection("products")
            .whereEqualTo("ownerId", ownerId)
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                productList.clear()
                val newProductList = mutableListOf<Product>()

                for (document in snapshot) {
                    val product = document.toObject<Product>()
                    product.let { newProductList.add(it) }
                    Log.d("Firestore", "Producto cargado: ${product.name}")
                }

                productList.addAll(newProductList)
                filteredProductList.clear()
                filteredProductList.addAll(productList)

                adapter.updateList(filteredProductList)
                Log.d("Firestore", "Productos totales: ${productList.size}")
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error al cargar los productos", exception)
            }
    }

    private fun navigateToAddProductScreen() {
        val activity = requireActivity()
        activity.startActivity(Intent(activity, AddProductActivity::class.java))
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este producto?")
            .setPositiveButton("Aceptar") { dialog, _ ->
                deleteProductFromFirestore(product)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteProductFromFirestore(product: Product) {
        firestore.collection("products").document(product.id).delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firestore", "Producto eliminado")
                    adapter.removeProduct(product)
                } else {
                    Log.e("Firestore", "Error al eliminar producto", task.exception)
                }
            }
    }

    private fun updateProductInFirestore(updatedProduct: Product) {
        firestore.collection("products").document(updatedProduct.id).set(updatedProduct)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firestore", "Producto actualizado")
                } else {
                    Log.e("Firestore", "Error al actualizar producto", task.exception)
                }
            }
    }

    private fun showSortOptions() {
        val options = arrayOf("Nombre (A-Z)", "Stock (mayor a menor)", "Más reciente")
        AlertDialog.Builder(requireContext())
            .setTitle("Ordenar por:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sortByName()
                    1 -> sortByStock()
                    2 -> sortByRecent()
                }
            }
            .show()
    }

    private fun sortByName() {
        val sorted = filteredProductList.sortedBy { it.name.lowercase() }
        adapter.updateList(sorted)
    }

    private fun sortByStock() {
        val sorted = filteredProductList.sortedByDescending { it.stock }
        adapter.updateList(sorted)
    }

    private fun sortByRecent() {
        val sorted = filteredProductList.sortedByDescending { it.createdAt }
        adapter.updateList(sorted)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
