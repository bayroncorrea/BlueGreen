package com.bb.bluegreen.ui.Inventory

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bb.bluegreen.R
import com.bb.bluegreen.databinding.ActivityAddProductBinding
import com.bb.bluegreen.databinding.DialogProgressBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject
import okhttp3.Callback
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    private lateinit var progressDialog: AlertDialog

    // Contracts para la selección de imágenes
    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                if (isUriAccessible(it)) {
                    selectedImageUri = it
                    binding.ivProductImage.setImageURI(it)
                } else {
                    Toast.makeText(
                        this,
                        "La imagen seleccionada no es accesible",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                if (isUriAccessible(it)) {
                    selectedImageUri = it
                    binding.ivProductImage.setImageURI(it)
                } else {
                    Toast.makeText(
                        this,
                        "La imagen seleccionada no es accesible",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                selectedImageUri = cameraImageUri
                binding.ivProductImage.setImageURI(cameraImageUri)
            } else {
                currentPhotoPath?.let { path ->
                    File(path).takeIf { it.exists() }?.delete()
                }
            }
        }

    // Contracts para permisos
    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                showPermissionExplanation(
                    "Para tomar fotos, la aplicación necesita acceso a la cámara",
                    Manifest.permission.CAMERA
                )
            }
        }

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                selectImageLauncher.launch("image/*")
            } else {
                showPermissionExplanation(
                    "Para seleccionar imágenes, la aplicación necesita acceso al almacenamiento",
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Firebase Storage
        FirebaseStorage.getInstance().maxOperationRetryTimeMillis = 30000

        initProgressDialog()
        setupButtons()
    }

    private fun initProgressDialog() {
        val progressBinding = DialogProgressBinding.inflate(LayoutInflater.from(this))
        progressDialog = AlertDialog.Builder(this)
            .setView(progressBinding.root)
            .setCancelable(false)
            .create()

        progressBinding.tvMessage.text = getString(R.string.uploading_image)
    }

    private fun setupButtons() {
        binding.btnSelectImage.setOnClickListener {
            openImageOptions()
        }

        binding.btnAddProduct.setOnClickListener {
            addProduct()
        }
    }

    private fun openImageOptions() {
        val options = arrayOf("Seleccionar desde galería", "Tomar foto")
        AlertDialog.Builder(this)
            .setTitle("Agregar imagen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> checkCameraPermission()
                }
            }
            .show()
    }

    private fun openGallery() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (hasStoragePermission()) {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } else {
                    requestStoragePermission()
                }
            }

            else -> {
                if (hasStoragePermission()) {
                    selectImageLauncher.launch("image/*")
                } else {
                    requestStoragePermission()
                }
            }
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showPermissionExplanation(
                "Para seleccionar imágenes, necesitamos acceso a tus archivos multimedia",
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.CAMERA
                )
            ) {
                showPermissionExplanation(
                    "Para tomar fotos, necesitamos acceso a la cámara",
                    Manifest.permission.CAMERA
                )
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionExplanation(message: String, permission: String) {
        AlertDialog.Builder(this)
            .setTitle("Permiso requerido")
            .setMessage(message)
            .setPositiveButton("Aceptar") { _, _ ->
                when (permission) {
                    Manifest.permission.CAMERA -> requestCameraPermissionLauncher.launch(permission)
                    Manifest.permission.READ_EXTERNAL_STORAGE -> requestStoragePermissionLauncher.launch(
                        permission
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openCamera() {
        val photoFile = try {
            createImageFile()
        } catch (ex: Exception) {
            Log.e("CAMERA_ERROR", "Error al crear archivo", ex)
            Toast.makeText(this, "Error al acceder a la cámara", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            val photoURI = FileProvider.getUriForFile(
                this, "${packageName}.fileprovider", it
            )
            cameraImageUri = photoURI
            currentPhotoPath = it.absolutePath
            cameraLauncher.launch(photoURI)
        }
    }

    @Throws(Exception::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.mkdirs()

        return File.createTempFile(
            "JPEG_${timeStamp}_", ".jpg", storageDir
        ).apply {
            currentPhotoPath = absolutePath
            scanFile(this)
        }
    }

    private fun scanFile(file: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = Uri.fromFile(file)
        sendBroadcast(mediaScanIntent)
    }

    private fun addProduct() {
        val name = binding.etProductName.text.toString().trim()
        val barcode = binding.etBarcode.text.toString().trim()
        val price = binding.etPrice.text.toString().toDoubleOrNull()
        val stock = binding.etStock.text.toString().toIntOrNull()

        if (name.isBlank() || barcode.isBlank() || price == null || stock == null) {
            Toast.makeText(
                this,
                "Por favor, complete todos los campos correctamente",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val productId = firestore.collection("products").document().id

        selectedImageUri?.let { uri ->
            if (!isUriAccessible(uri)) {
                Toast.makeText(this, "La imagen seleccionada no es accesible", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            progressDialog.show()
            uploadImageToCloudinary(uri, { imageUrl ->
                saveProductToFirestore(productId, name, barcode, price, stock, imageUrl)
            }, { error ->
                progressDialog.dismiss()
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            })

        } ?: run {
            saveProductToFirestore(
                productId,
                name,
                barcode,
                price,
                stock,
                "https://example.com/default_product.png"
            )
        }
    }

    private fun uploadImageToCloudinary(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val file = uriToFile(uri) ?: return onError("No se pudo acceder a la imagen")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaType()))
            .addFormDataPart("upload_preset", "BlueGreen")// 🔒 Configura esto en Cloudinary
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/dq2rzordz/image/upload") // 👈 Reemplaza con tu cloud name
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    onError("Error al subir la imagen: ${e.localizedMessage}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")
                if (response.isSuccessful) {
                    val imageUrl = json.getString("secure_url")
                    runOnUiThread {
                        onSuccess(imageUrl)
                    }
                } else {
                    runOnUiThread {
                        onError("Error en respuesta de Cloudinary: ${json.toString()}")
                    }
                }
            }
        })
    }

    private fun isUriAccessible(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { it.close() }
            true
        } catch (e: Exception) {
            Log.e("URI_ACCESS", "Error al verificar URI", e)
            false
        }
    }

    private fun saveProductToFirestore(
        productId: String,
        name: String,
        barcode: String,
        price: Double,
        stock: Int,
        imageUrl: String = ""
    ) {
        val product = hashMapOf(
            "id" to productId,
            "name" to name,
            "barcode" to barcode,
            "price" to price,
            "stock" to stock,
            "imageUrl" to imageUrl
        )

        firestore.collection("products").document(productId)
            .set(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto agregado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_ERROR", "Error al guardar producto: ${e.message}")
                Toast.makeText(
                    this,
                    "Error al guardar el producto: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentPhotoPath?.let { path ->
            File(path).takeIf { it.exists() }?.delete()
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

}