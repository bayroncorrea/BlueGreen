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
import com.bb.bluegreen.loginActivity
import com.bb.bluegreen.ui.Inventory.AddProductActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

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

        // Botón de cerrar sesión
        binding.btnLogout.setOnClickListener {
            logout()
        }

        // Inicialización del RecyclerView
        lowStockAdapter = LowStockAdapter(emptyList(), showActions = false)


        binding.recyclerLowStock.adapter = lowStockAdapter
        binding.recyclerLowStock.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLowStock.setHasFixedSize(true)

        setupObservers()

        // Cargar inventario desde Firestore
        viewModel.loadInventoryFromFirebase()

        return binding.root
    }

    private fun setupObservers() {
        viewModel.totalStock.observe(viewLifecycleOwner) { total ->
            binding.txtInventorySummary.text = when {
                total == 0 -> getString(R.string.no_products_in_stock)
                total == 1 -> getString(R.string.one_product_in_stock)
                else -> getString(R.string.multiple_products_in_stock, total)
            }
        }

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

        viewModel.lowStockAlert.observe(viewLifecycleOwner) { alertText ->
            binding.txtLowStockAlert.text = alertText
            binding.txtLowStockAlert.visibility =
                if (alertText.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun logout() {
        // Cerrar sesión de Firebase
        FirebaseAuth.getInstance().signOut()

        // Cerrar sesión de Google
        GoogleSignIn.getClient(
            requireContext(),
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        ).signOut().addOnCompleteListener {
            // Redirigir al login
            val intent = Intent(requireContext(), loginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
