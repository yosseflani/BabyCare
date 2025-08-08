package com.example.babycare

import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddScheduleActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences
    private var babyId: String? = null
    private var familyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
        familyId = sharedPref.getString("familyId", null)
        babyId = intent.getStringExtra("babyId")

        if (familyId.isNullOrEmpty() || babyId.isNullOrEmpty()) {
            Toast.makeText(this, "שגיאה: אין תינוק או משפחה", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etFrequency = findViewById<EditText>(R.id.etFrequency)
        val etTime = findViewById<EditText>(R.id.etTime)
        val btnSave = findViewById<Button>(R.id.btnSaveSchedule)

        etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                etTime.setText(String.format("%02d:%02d", hour, minute))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val frequency = etFrequency.text.toString().toIntOrNull()
            val time = etTime.text.toString().trim()

            var valid = true
            if (title.isEmpty()) {
                etTitle.error = "יש להזין שם משימה"
                valid = false
            }
            if (frequency == null || frequency <= 0) {
                etFrequency.error = "יש להזין ערך מספרי חיובי"
                valid = false
            }
            if (time.isEmpty()) {
                etTime.error = "יש לבחור שעה"
                valid = false
            }
            if (!valid) return@setOnClickListener

            val scheduleItem = hashMapOf(
                "title" to title,
                "description" to description,
                "frequencyHours" to frequency,
                "time" to time,
                "babyId" to babyId!!,
                "isDone" to false
            )

            db.collection("families").document(familyId!!)
                .collection("schedule")
                .add(scheduleItem)
                .addOnSuccessListener {
                    Toast.makeText(this, "המשימה נוספה בהצלחה", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "שגיאה: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
