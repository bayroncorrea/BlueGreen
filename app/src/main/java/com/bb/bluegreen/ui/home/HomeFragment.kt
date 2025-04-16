package com.bb.bluegreen.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bb.bluegreen.R
import com.bb.bluegreen.databinding.FragmentHomeBinding
import com.bb.bluegreen.ui.Inventory.AddProductActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var lowStockAdapter: LowStockAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

// Inicialización del RecyclerView
        lowStockAdapter = LowStockAdapter(emptyList())
        binding.recyclerLowStock.adapter = lowStockAdapter
        binding.recyclerLowStock.layoutManager =
            LinearLayoutManager(requireContext())
        binding.recyclerLowStock.setHasFixedSize(true)

        setupObservers()
        setupButtons()

        // Cargar inventario desde Firestore
        viewModel.loadInventoryFromFirebase()

        return binding.root
    }

    private fun setupObservers() {
        // Observa cambios en el total de productos en inventario
        viewModel.totalStock.observe(viewLifecycleOwner) { total ->
            binding.txtInventorySummary.text = when {
                total == 0 -> getString(R.string.no_products_in_stock)
                total == 1 -> getString(R.string.one_product_in_stock)
                else -> getString(R.string.multiple_products_in_stock, total)
            }
        }

        // Observa productos con bajo stock
        viewModel.lowStockProducts.observe(viewLifecycleOwner) { lowStockList ->
            if (lowStockList.isNotEmpty()) {
                lowStockAdapter.updateProducts(lowStockList)
                binding.recyclerLowStock.visibility = View.VISIBLE
                binding.txtEmptyLowStock.visibility = View.GONE
            } else {
                binding.recyclerLowStock.visibility = View.GONE
                binding.txtEmptyLowStock.visibility = View.VISIBLE
            }
        }

        // Observa alerta de bajo stock
        viewModel.lowStockAlert.observe(viewLifecycleOwner) { alertText ->
            binding.txtLowStockAlert.text = alertText
            binding.txtLowStockAlert.visibility =
                if (alertText.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupButtons() {
        binding.btnAddProduct.setOnClickListener {
            val activity = requireActivity()
            activity.startActivity(Intent(activity, AddProductActivity::class.java))
        }

        binding.btnViewInventory.setOnClickListener {
            // Navegar para ver inventario
        }

        binding.btnSettings.setOnClickListener {
            // Navegar a configuraciones
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}