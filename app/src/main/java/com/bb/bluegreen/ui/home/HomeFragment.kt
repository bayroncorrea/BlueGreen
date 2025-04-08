package com.bb.bluegreen.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bb.bluegreen.R
import com.bb.bluegreen.databinding.FragmentHomeBinding
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var lowStockAdapter: LowStockAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupObservers()
        setupButtons()

        // Cargar el inventario desde Firebase al iniciar el fragmento
        viewModel.loadInventoryFromFirebase() // Llamada al ViewModel
        FirebaseFirestore.setLoggingEnabled(true)

        return binding.root
    }

    private fun setupObservers() {
        // Observa el total de stock
        viewModel.totalStock.observe(viewLifecycleOwner) { stock ->
            binding.txtInventorySummary.text = getString(R.string.inventory_summary, stock)
        }

        // Observa alertas de stock bajo
        viewModel.lowStockAlert.observe(viewLifecycleOwner) { alertMessage ->
            if (alertMessage.isNotEmpty()) {
                binding.txtLowStockAlert.text = alertMessage
                binding.txtLowStockAlert.visibility = View.VISIBLE
            } else {
                binding.txtLowStockAlert.visibility = View.GONE
            }
        }

        // Observa productos con stock bajo
        viewModel.lowStockProducts.observe(viewLifecycleOwner) { lowStockList ->
            if (lowStockList.isNotEmpty()) {
                binding.recyclerLowStock.visibility = View.VISIBLE
                lowStockAdapter = LowStockAdapter(lowStockList)
                binding.recyclerLowStock.adapter = lowStockAdapter
            } else {
                binding.recyclerLowStock.visibility = View.GONE
            }
        }
    }

    private fun setupButtons() {
        binding.btnAddProduct.setOnClickListener {
            // Navegar al fragmento de agregar producto
            // Puedes implementar la navegación o la acción que desees aquí
        }

        binding.btnViewInventory.setOnClickListener {
            // Navegar a la vista del inventario
            findNavController().navigate(R.id.mobile_navigation)
        }

        binding.btnSettings.setOnClickListener {
            // Navegar a configuración
            // Puedes implementar la acción de navegación aquí
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
