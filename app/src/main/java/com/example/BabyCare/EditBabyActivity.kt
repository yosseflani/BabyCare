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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class EditBabyActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences
    private var selectedImageUri: Uri? = null
    private lateinit var imageViewSelected: ImageView
    private lateinit var progressDialog: ProgressDialog
    private var babyId: String? = null
    private var familyId: String? = null
    private var imageUrl: String = ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            imageViewSelected.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_baby)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
        progressDialog = ProgressDialog(this).apply {
            setMessage("מעלה תמונה...")
            setCancelable(false)
        }

        babyId = intent.getStringExtra("babyId")
        familyId = sharedPref.getString("familyId", null)

        if (babyId.isNullOrEmpty() || familyId.isNullOrEmpty()) {
            Toast.makeText(this, "שגיאה: אין מזהה תינוק או משפחה", Toast.LENGTH_SHORT).show()
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
        val btnDelete = findViewById<Button>(R.id.btnDeleteBaby)
        imageViewSelected = findViewById(R.id.imageViewSelected)

        db.collection("families").document(familyId!!)
            .collection("babies").document(babyId!!)
            .get()
            .addOnSuccessListener { doc ->
                val baby = doc.toObject(Baby::class.java)
                if (baby != null) {
                    etName.setText(baby.name)
                    etBirthDate.setText(baby.birthDate)
                    etWeight.setText(baby.weight.toString())
                    etHeight.setText(baby.height.toString())
                    etAllergies.setText(baby.allergies)
                    etMedications.setText(baby.medications)
                    imageUrl = baby.imageUrl
                    if (imageUrl.isNotEmpty()) {
                        Glide.with(this).load(imageUrl).into(imageViewSelected)
                    }
                }
            }

        etBirthDate.setOnClickListener { showDatePickerDialog(etBirthDate) }

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

            if (name.isEmpty() || birthDate.isEmpty() || weight == null || height == null || weight <= 0 || height <= 0) {
                Toast.makeText(this, "יש למלא את כל השדות החיוניים בצורה תקינה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = calculateAge(birthDate)

            fun updateBaby(imageUrl: String) {
                val updatedBaby = mapOf(
                    "name" to name,
                    "birthDate" to birthDate,
                    "age" to age,
                    "weight" to weight,
                    "height" to height,
                    "allergies" to allergies,
                    "medications" to medications,
                    "imageUrl" to imageUrl
                )

                db.collection("families").document(familyId!!)
                    .collection("babies").document(babyId!!)
                    .update(updatedBaby)
                    .addOnSuccessListener {
                        Toast.makeText(this, "הפרטים עודכנו בהצלחה", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "שגיאה בעדכון: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            if (selectedImageUri != null) {
                progressDialog.show()
                val storageRef = FirebaseStorage.getInstance().reference.child("baby_images/${UUID.randomUUID()}")
                storageRef.putFile(selectedImageUri!!)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) throw task.exception!!
                        storageRef.downloadUrl
                    }.addOnSuccessListener { uri ->
                        progressDialog.dismiss()
                        updateBaby(uri.toString())
                    }.addOnFailureListener {
                        progressDialog.dismiss()
                        updateBaby(imageUrl)
                    }
            } else {
                updateBaby(imageUrl)
            }
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("אישור מחיקה")
                .setMessage("האם אתה בטוח שברצונך למחוק את התינוק?")
                .setPositiveButton("כן, מחק") { _, _ ->
                    deleteBaby()
                }
                .setNegativeButton("בטל", null)
                .show()
        }
    }

    private fun deleteBaby() {
        db.collection("families").document(familyId!!)
            .collection("babies").document(babyId!!)
            .delete()
            .addOnSuccessListener {
                if (imageUrl.isNotEmpty()) {
                    try {
                        FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl).delete()
                    } catch (_: Exception) {}
                }
                Toast.makeText(this, "התינוק נמחק", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "שגיאה במחיקה: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestGalleryPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 1001)
            } else {
                pickImageLauncher.launch("image/*")
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1001)
            } else {
                pickImageLauncher.launch("image/*")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*")
        } else if (requestCode == 1001) {
            Toast.makeText(this, "לא ניתנה הרשאה לגשת לגלריה", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePickerDialog(et: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            et.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
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
