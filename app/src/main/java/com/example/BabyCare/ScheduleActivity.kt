package com.example.babycare

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ScheduleActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var scheduleList: ArrayList<ScheduleItem>
    private lateinit var registration: ListenerRegistration
    private lateinit var emptyStateText: TextView
    private lateinit var btnAddSchedule: Button
    private lateinit var recyclerView: RecyclerView
    private var babyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
        val familyId = sharedPref.getString("familyId", null)
        babyId = intent.getStringExtra("babyId")

        if (familyId.isNullOrEmpty() || babyId.isNullOrEmpty()) {
            Toast.makeText(this, "שגיאה: לא מחובר למשפחה או תינוק", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        emptyStateText = findViewById(R.id.textEmptyState)
        btnAddSchedule = findViewById(R.id.btnAddSchedule)
        recyclerView = findViewById(R.id.recyclerViewSchedule)

        scheduleList = arrayListOf()
        scheduleAdapter = ScheduleAdapter(scheduleList)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = scheduleAdapter

        btnAddSchedule.setOnClickListener {
            startActivity(Intent(this, AddScheduleActivity::class.java).apply {
                putExtra("babyId", babyId)
            })
        }

        loadScheduleItems(familyId, babyId!!)
    }

    private fun loadScheduleItems(familyId: String, babyId: String) {
        registration = db.collection("families").document(familyId)
            .collection("schedule")
            .whereEqualTo("babyId", babyId)
            .orderBy("time")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "שגיאה בטעינת המשימות", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                scheduleList.clear()
                value?.forEach {
                    val item = it.toObject(ScheduleItem::class.java).copy(id = it.id)
                    scheduleList.add(item)
                }

                scheduleAdapter.notifyDataSetChanged()
                emptyStateText.visibility = if (scheduleList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        registration.remove()
    }
}
