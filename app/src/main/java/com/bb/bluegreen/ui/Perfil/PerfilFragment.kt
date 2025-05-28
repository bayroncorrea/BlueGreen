package com.bb.bluegreen.ui.Perfil

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bb.bluegreen.databinding.FragmentPerfilBinding
import com.bb.bluegreen.loginActivity
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)

        binding.btnSalir.setOnClickListener {
            logout()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarDatosPerfil()

        guardarDatosInicialesDeUsuario()

    }

    private fun guardarDatosInicialesDeUsuario() {
        val user = auth.currentUser ?: return

        val locale = resources.configuration.locales[0]
        val countryCode = locale.country
        val country = java.util.Locale("", countryCode).displayCountry

        val userData = hashMapOf(
            "nombre" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "pais" to country
        )

        val userDocRef = db.collection("usuarios").document(user.uid)

        userDocRef.set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", "Perfil actualizado con país: $country")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error al guardar perfil", e)
            }
    }


    private fun cargarDatosPerfil() {
        val userId = auth.currentUser?.uid ?: return
        val user = auth.currentUser

        db.collection("usuarios").document(userId).get().addOnSuccessListener { document ->
            if (document != null) {
                binding.etName.text = document.getString("nombre") ?: ""
                binding.etEmail.text = document.getString("email") ?: ""
                binding.etUbicacion.text = document.getString("pais") ?: "Ubicación desconocida"

                val imagenUrl = document.getString("imagenUrl")

                if (!imagenUrl.isNullOrEmpty()) {
                    Glide.with(this).load(imagenUrl).circleCrop().into(binding.imageProfile)
                } else {
                    val photoUrl = user?.photoUrl
                    if (photoUrl != null) {
                        Glide.with(this).load(photoUrl).circleCrop().into(binding.imageProfile)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        GoogleSignIn.getClient(
            requireContext(),
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        ).signOut().addOnCompleteListener {
            val intent = Intent(requireContext(), loginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
