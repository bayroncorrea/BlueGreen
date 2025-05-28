package com.bb.bluegreen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class loginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private val GOOGLE_SIGN_IN_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()

        val googleSignInBtn = findViewById<Button>(R.id.btnGoogleSignIn)
        googleSignInBtn.setOnClickListener {
            startGoogleSignIn()
        }

        if (firebaseAuth.currentUser != null) {
            Log.d("AuthDebug", "Usuario ya autenticado: ${firebaseAuth.currentUser?.uid}")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
    }

    private fun startGoogleSignIn() {
        val googleSignInIntent = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            this,
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        ).signInIntent

        startActivityForResult(googleSignInIntent, GOOGLE_SIGN_IN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_CODE) {
            val task =
                com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(
                    data
                )
            if (task.isSuccessful) {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener { authResult ->
                        if (authResult.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            Log.d("AuthDebug", "Login exitoso: UID=${user?.uid}")

                            val uid = user?.uid
                            if (uid == null) {
                                Log.e("AuthDebug", "UID es null, no se puede guardar el usuario")
                                return@addOnCompleteListener
                            }

                            val name = user.displayName ?: ""
                            val email = user.email ?: ""
                            val photoUrl = user.photoUrl?.toString() ?: ""

                            Log.d(
                                "FirestoreDebug",
                                "Datos del usuario: Nombre=$name, Email=$email, Foto=$photoUrl"
                            )
                            Log.d(
                                "FirestoreDebug",
                                "Intentando guardar usuario en Firestore con UID: $uid"
                            )

                            val userData = hashMapOf(
                                "nombre" to name,
                                "email" to email,
                                "foto" to photoUrl
                            )

                            FirebaseFirestore.getInstance()
                                .collection("usuarios")
                                .document(uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    Log.d(
                                        "FirestoreDebug",
                                        "Usuario guardado en Firestore: UID=$uid"
                                    )
                                    Toast.makeText(this, "Bienvenido $email", Toast.LENGTH_SHORT)
                                        .show()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(
                                        "FirestoreError",
                                        "Error guardando usuario en Firestore",
                                        e
                                    )
                                    Toast.makeText(
                                        this,
                                        "Error al guardar usuario",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Log.e(
                                "AuthDebug",
                                "Fallo login Firebase: ${authResult.exception?.message}",
                                authResult.exception
                            )
                            Toast.makeText(
                                this,
                                "Error al iniciar sesión con Firebase",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Log.e(
                    "AuthDebug",
                    "Error al obtener cuenta de Google: ${task.exception?.message}",
                    task.exception
                )
            }
        }
    }
}
