package com.example.babycare

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class AddBabyActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences
    private var selectedImageUri: Uri? = null
    private lateinit var imageViewSelected: ImageView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var babyId: String

    companion object {
        private const val TAG = "AddBabyActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            imageViewSelected.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_baby)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
        progressDialog = ProgressDialog(this).apply {
            setMessage("מעלה תמונה...")
            setCancelable(false)
        }

        val familyId = sharedPref.getString("familyId", null)
        if (familyId.isNullOrEmpty()) {
            Toast.makeText(this, "שגיאה: לא מחובר למשפחה", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etName = findViewById<EditText>(R.id.etName)
        val etBirthDate = findViewById<EditText>(R.id.etBirthDate)
        val etWeight = findViewById<EditText>(R.id.etWeight)
        val etHeight = findViewById<EditText>(R.id.etHeight)
        val etAllergies = findViewById<EditText>(R.id.etAllergies)
        val etMedications = findViewById<EditText>(R.id.etMedications)
        val btnSave = findViewById<Button>(R.id.btnSaveBaby)
        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)
        imageViewSelected = findViewById(R.id.imageViewSelected)

        etBirthDate.setOnClickListener {
            showDatePickerDialog(etBirthDate)
        }

        btnSelectImage.setOnClickListener {
            requestGalleryPermissionAndPickImage()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val birthDate = etBirthDate.text.toString()
            val weight = etWeight.text.toString().toDoubleOrNull()
            val height = etHeight.text.toString().toDoubleOrNull()
            val allergies = etAllergies.text.toString()
            val medications = etMedications.text.toString()

            var valid = true
            if (name.isEmpty()) {
                etName.error = "יש להזין שם"
                valid = false
            }
            if (birthDate.isEmpty()) {
                etBirthDate.error = "יש להזין תאריך לידה"
                valid = false
            }
            if (weight == null || weight <= 0) {
                etWeight.error = "יש להזין משקל תקין"
                valid = false
            }
            if (height == null || height <= 0) {
                etHeight.error = "יש להזין גובה תקין"
                valid = false
            }
            if (!valid) return@setOnClickListener

            val age = calculateAge(birthDate)
            babyId = UUID.randomUUID().toString()

            if (selectedImageUri != null) {
                uploadImageAndSaveBaby(familyId, name, birthDate, age, weight!!, height!!, allergies, medications)
            } else {
                saveBaby(familyId, name, birthDate, age, weight!!, height!!, allergies, medications, "")
            }
        }
    }

    private fun uploadImageAndSaveBaby(
        familyId: String,
        name: String,
        birthDate: String,
        age: Int,
        weight: Double,
        height: Double,
        allergies: String,
        medications: String
    ) {
        progressDialog.show()

        val storage = FirebaseStorage.getInstance()
        val imageFileName = "baby_${System.currentTimeMillis()}_${babyId}.jpg"
        val storageRef = storage.reference.child("baby_images/$imageFileName")

        try {
            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
            if (inputStream != null) {
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()

                val uploadTask = storageRef.putStream(inputStream, metadata)

                uploadTask.addOnSuccessListener {
                    Log.d(TAG, "Image uploaded successfully")
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        Log.d(TAG, "Download URL: $uri")
                        progressDialog.dismiss()
                        saveBaby(familyId, name, birthDate, age, weight, height, allergies, medications, uri.toString())
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get download URL: ${e.message}")
                        progressDialog.dismiss()
                        saveBaby(familyId, name, birthDate, age, weight, height, allergies, medications, "")
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Image upload failed: ${e.message}")
                    progressDialog.dismiss()
                    saveBaby(familyId, name, birthDate, age, weight, height, allergies, medications, "")
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "לא ניתן לפתוח את התמונה שנבחרה", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during upload: ${e.message}")
            progressDialog.dismiss()
            Toast.makeText(this, "שגיאה בקריאת התמונה: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveBaby(
        familyId: String,
        name: String,
        birthDate: String,
        age: Int,
        weight: Double,
        height: Double,
        allergies: String,
        medications: String,
        imageUrl: String
    ) {
        val baby = hashMapOf(
            "id" to babyId,
            "name" to name,
            "birthDate" to birthDate,
            "age" to age,
            "weight" to weight,
            "height" to height,
            "allergies" to allergies,
            "medications" to medications,
            "imageUrl" to imageUrl,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("families").document(familyId)
            .collection("babies").document(babyId)
            .set(baby)
            .addOnSuccessListener {
                Toast.makeText(this, "התינוק התווסף בהצלחה", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "שגיאה בהוספה: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun requestGalleryPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
            } else {
                pickImageLauncher.launch("image/*")
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            } else {
                pickImageLauncher.launch("image/*")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "לא ניתנה הרשאה לגשת לגלריה", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePickerDialog(et: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                et.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun calculateAge(birthDate: String): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birth = sdf.parse(birthDate) ?: return 0
            val dob = Calendar.getInstance().apply { time = birth }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--
            age
        } catch (e: Exception) {
            0
        }
    }
}
