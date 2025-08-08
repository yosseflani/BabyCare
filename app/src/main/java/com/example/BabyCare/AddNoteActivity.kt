package com.example.babycare

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddNoteActivity : AppCompatActivity() {

    private var babyId: String? = null
    private var familyId: String? = null
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        babyId = intent.getStringExtra("babyId")
        familyId = intent.getStringExtra("familyId")
            ?: getSharedPreferences("BabyCare", MODE_PRIVATE).getString("familyId", null)

        if (babyId.isNullOrEmpty() || familyId.isNullOrEmpty()) {
            Toast.makeText(this, "שגיאה: לא נמצא מזהה משתמש או תינוק", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        db = FirebaseFirestore.getInstance()

        val etTitle = findViewById<EditText>(R.id.etNoteTitle)
        val etDesc = findViewById<EditText>(R.id.etNoteDesc)
        val etTime = findViewById<EditText>(R.id.etNoteTime)
        val btnSave = findViewById<Button>(R.id.btnSaveNote)


        etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    etTime.setText(String.format("%02d:%02d", hour, minute))
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val time = etTime.text.toString().trim()

            if (title.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "אנא מלא כותרת ושעה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            val noteMap = hashMapOf(
                "title" to title,
                "description" to desc,
                "datetime" to "$now $time",
                "time" to time,
                "babyId" to babyId,
                "createdAt" to now
            )

            db.collection("families").document(familyId!!)
                .collection("notes")
                .add(noteMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "ההערה נוספה בהצלחה!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "שגיאה בהוספת ההערה: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
