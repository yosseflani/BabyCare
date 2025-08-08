package com.example.babycare

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class BabyDetailsActivity : AppCompatActivity() {

    private var babyId: String? = null
    private var familyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_baby_details)

        babyId = intent.getStringExtra("babyId")
        familyId = getSharedPreferences("BabyCare", MODE_PRIVATE).getString("familyId", null)

        if (babyId.isNullOrEmpty() || familyId.isNullOrEmpty()) {
            Toast.makeText(this, "שגיאה: חסרים פרטי גישה", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        title = "פרטי תינוק"
        initializeViews()
    }

    private fun initializeViews() {
        val tvName = findViewById<TextView>(R.id.tvBabyName)
        val tvBirthDate = findViewById<TextView>(R.id.tvBirthDate)
        val tvWeight = findViewById<TextView>(R.id.tvWeight)
        val tvHeight = findViewById<TextView>(R.id.tvHeight)
        val tvAllergies = findViewById<TextView>(R.id.tvAllergies)
        val tvMedications = findViewById<TextView>(R.id.tvMedications)
        val imageView = findViewById<ImageView>(R.id.imageViewBabyDetails)

        setupButtonListeners()
        loadBabyDetails(tvName, tvBirthDate, tvWeight, tvHeight, tvAllergies, tvMedications, imageView)
    }

    private fun setupButtonListeners() {
        findViewById<Button>(R.id.btnAddTask).setOnClickListener {
            startActivity(Intent(this, AddScheduleActivity::class.java).apply {
                putExtra("babyId", babyId)
            })
        }

        findViewById<Button>(R.id.btnChecklist).setOnClickListener {
            startActivity(Intent(this, AddChecklistItemActivity::class.java).apply {
                putExtra("babyId", babyId)
                putExtra("familyId", familyId)
            })
        }

        findViewById<Button>(R.id.btnNotes).setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java).apply {
                putExtra("babyId", babyId)
                putExtra("familyId", familyId)
            })
        }

        findViewById<Button>(R.id.btnEditBaby).setOnClickListener {
            startActivity(Intent(this, EditBabyActivity::class.java).apply {
                putExtra("babyId", babyId)
            })
        }
    }

    private fun loadBabyDetails(
        tvName: TextView,
        tvBirthDate: TextView,
        tvWeight: TextView,
        tvHeight: TextView,
        tvAllergies: TextView,
        tvMedications: TextView,
        imageView: ImageView
    ) {
        val db = FirebaseFirestore.getInstance()
        val documentRef = db.collection("families").document(familyId!!)
            .collection("babies").document(babyId!!)

        documentRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "התינוק לא נמצא במערכת", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val baby = doc.toObject(Baby::class.java)
                if (baby != null) {
                    updateUI(baby, tvName, tvBirthDate, tvWeight, tvHeight, tvAllergies, tvMedications, imageView)
                } else {
                    Toast.makeText(this, "שגיאה בטעינת פרטי התינוק", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "שגיאה בגישה לנתונים", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun updateUI(
        baby: Baby,
        tvName: TextView,
        tvBirthDate: TextView,
        tvWeight: TextView,
        tvHeight: TextView,
        tvAllergies: TextView,
        tvMedications: TextView,
        imageView: ImageView
    ) {
        tvName.text = baby.name
        tvBirthDate.text = "📅 תאריך לידה: ${baby.birthDate ?: "-"}"
        tvWeight.text = "⚖️ משקל: ${baby.weight ?: "-"} ק\"ג"
        tvHeight.text = "📏 גובה: ${baby.height ?: "-"} ס\"מ"
        tvAllergies.text = "🌿 אלרגיות: ${if (baby.allergies.isNullOrBlank()) "אין" else baby.allergies}"
        tvMedications.text = "💊 תרופות: ${if (baby.medications.isNullOrBlank()) "אין" else baby.medications}"

        if (!baby.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(baby.imageUrl)
                .placeholder(R.drawable.ic_baby_placeholder)
                .error(R.drawable.ic_baby_placeholder)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_baby_placeholder)
        }
    }
}
