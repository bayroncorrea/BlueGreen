package com.bb.bluegreen.ui.Perfil

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bb.bluegreen.databinding.FragmentPerfilBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.InputStream

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var imageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK && it.data != null) {
            imageUri = it.data!!.data
            val inputStream: InputStream? = imageUri?.let { uri ->
                requireContext().contentResolver.openInputStream(uri)
            }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.imageProfile.setImageBitmap(bitmap)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarDatosPerfil()

        binding.btnSelectProfileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }

        guardarDatosInicialesDeUsuario()

        binding.btnUpdateProfile.setOnClickListener {
            val nombre = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Nombre y correo no pueden estar vacíos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            val userMap = hashMapOf(
                "nombre" to nombre,
                "email" to email
            )

            if (imageUri != null) {
                val ref = storage.reference.child("usuarios/$userId/perfil.jpg")
                ref.putFile(imageUri!!)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { uri ->
                            userMap["imagenUrl"] = uri.toString()
                            guardarDatosFirestore(userId, userMap)
                        }
                    }
                    .addOnFailureListener {
                        mostrarError("Error al subir la imagen")
                        binding.progressBar.visibility = View.GONE
                    }
            } else {
                guardarDatosFirestore(userId, userMap)
            }
        }
    }

    private fun guardarDatosInicialesDeUsuario() {
        val user = auth.currentUser ?: return

        val userData = hashMapOf(
            "nombre" to (user.displayName ?: ""),
            "email" to (user.email ?: "")
        )

        val userDocRef = db.collection("usuarios").document(user.uid)

        userDocRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                userDocRef.set(userData)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Perfil inicial guardado")
                    }
                    .addOnFailureListener { e ->
                        Log.w("Firestore", "Error al guardar perfil inicial", e)
                    }
            }
        }
    }

    private fun cargarDatosPerfil() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    binding.etName.setText(document.getString("nombre") ?: "")
                    binding.etEmail.setText(document.getString("email") ?: "")

                    val imagenUrl = document.getString("imagenUrl")
                    if (!imagenUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imagenUrl)
                            .centerCrop()
                            .into(binding.imageProfile)
                    }
                }
            }
            .addOnFailureListener {
                mostrarError("Error al cargar datos del perfil")
            }
    }

    private fun guardarDatosFirestore(userId: String, userMap: HashMap<String, String>) {
        db.collection("usuarios").document(userId).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                mostrarError("Error al guardar los datos")
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
