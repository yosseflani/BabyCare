package com.example.babycare

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DailyChecklistActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences
    private lateinit var adapter: ChecklistAdapter
    private lateinit var items: ArrayList<ChecklistItem>
    private lateinit var today: String
    private var babyId: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_checklist)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("BabyCare", MODE_PRIVATE)

        val familyId = sharedPref.getString("familyId", null)
        babyId = intent.getStringExtra("babyId")

        if (familyId == null || babyId.isNullOrEmpty()) {
            Toast.makeText(this, "שגיאה: לא נבחר תינוק או משפחה", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        findViewById<TextView>(R.id.textViewDate).text = "צ'קליסט ל-$today"

        items = arrayListOf()
        adapter = ChecklistAdapter(items, familyId)

        findViewById<RecyclerView>(R.id.recyclerViewChecklist).apply {
            layoutManager = LinearLayoutManager(this@DailyChecklistActivity)
            adapter = this@DailyChecklistActivity.adapter
        }

        loadChecklistItems(familyId, babyId!!, today)
    }

    private fun loadChecklistItems(familyId: String, babyId: String, date: String) {
        db.collection("families").document(familyId)
            .collection("checklist")
            .whereEqualTo("babyId", babyId)
            .whereEqualTo("date", date)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "שגיאה בטעינת המשימות", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                items.clear()
                value?.forEach { doc ->
                    val item = doc.toObject(ChecklistItem::class.java).copy(id = doc.id)
                    items.add(item)
                }

                items.sortWith(compareBy {
                    try {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).parse(it.time)
                    } catch (e: Exception) {
                        null
                    }
                })

                adapter.notifyDataSetChanged()
            }
    }
}
