package com.example.babycare

import android.app.TimePickerDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddChecklistItemActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_checklist_item)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("BabyCare", MODE_PRIVATE)

        val familyId = sharedPref.getString("familyId", null)
        val babyId = intent.getStringExtra("babyId")

        if (familyId.isNullOrEmpty() || babyId.isNullOrEmpty()) {
            Toast.makeText(this, "שגיאה: לא נמצא מזהה משפחה או תינוק", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val etTitle = findViewById<EditText>(R.id.etChecklistTitle)
        val etDesc = findViewById<EditText>(R.id.etChecklistDesc)
        val etTime = findViewById<EditText>(R.id.etChecklistTime)
        val btnSave = findViewById<Button>(R.id.btnSaveChecklist)

        etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                val timeStr = String.format("%02d:%02d", hour, minute)
                etTime.setText(timeStr)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val time = etTime.text.toString().trim()

            if (title.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "אנא מלא כותרת ושעה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val itemMap = hashMapOf(
                "title" to title,
                "description" to desc,
                "time" to time,
                "date" to today,
                "babyId" to babyId,
                "isCompleted" to false
            )

            db.collection("families").document(familyId)
                .collection("checklist")
                .add(itemMap)
                .addOnSuccessListener { docRef ->

                    val checklistItem = ChecklistItem(
                        id = docRef.id,
                        babyId = babyId,
                        date = today,
                        title = title,
                        description = desc,
                        time = time,
                        isCompleted = false
                    )

                    ChecklistAlarmScheduler.scheduleChecklistReminder(this, checklistItem)

                    Toast.makeText(this, "המשימה נוספה!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "שגיאה בהוספה, נסה שוב", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
