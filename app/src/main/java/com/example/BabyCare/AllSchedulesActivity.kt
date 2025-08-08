package com.example.babycare

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject

class AllSchedulesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var scheduleAdapter: ScheduleWithBabyAdapter
    private lateinit var scheduleList: ArrayList<ScheduleWithBaby>
    private var listener: ListenerRegistration? = null
    private var familyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_schedules)

        db = FirebaseFirestore.getInstance()
        familyId = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
            .getString("familyId", null)

        if (familyId == null) {
            Toast.makeText(this, "שגיאה: לא מחובר למשפחה", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        scheduleList = arrayListOf()
        scheduleAdapter = ScheduleWithBabyAdapter(
            schedules = scheduleList,
            onDeleteClick = { deleteSchedule(it) }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAllSchedules)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = scheduleAdapter
    }

    override fun onStart() {
        super.onStart()
        listenForSchedules()
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
    }

    private fun listenForSchedules() {
        listener = db.collection("families").document(familyId!!)
            .collection("schedule")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "שגיאה בטעינה: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                loadSchedulesWithBabyNames(snapshots)
            }
    }

    private fun loadSchedulesWithBabyNames(scheduleSnapshot: QuerySnapshot?) {
        if (scheduleSnapshot == null) return

        val babyIdNameMap = mutableMapOf<String, String>()

        db.collection("families").document(familyId!!)
            .collection("babies")
            .get()
            .addOnSuccessListener { babySnapshot ->
                for (babyDoc in babySnapshot) {
                    val babyId = babyDoc.id
                    val baby = babyDoc.toObject<Baby>()
                    babyIdNameMap[babyId] = baby.name
                }

                val tempList = mutableListOf<ScheduleWithBaby>()

                for (scheduleDoc in scheduleSnapshot) {
                    val title = scheduleDoc.getString("title") ?: continue
                    val desc = scheduleDoc.getString("description") ?: ""
                    val time = scheduleDoc.getString("time") ?: ""
                    val frequency = scheduleDoc.get("frequencyHours")?.toString() ?: ""
                    val babyId = scheduleDoc.getString("babyId") ?: ""
                    val babyName = babyIdNameMap[babyId] ?: "לא ידוע"
                    val isDone = scheduleDoc.getBoolean("isDone") ?: false // ✅ חדש
                    val id = scheduleDoc.id // ✅ מזהה מתועד כדי שנוכל לעדכן

                    val scheduleItem = ScheduleWithBaby(
                        id = id,
                        title = title,
                        description = desc,
                        time = time,
                        frequency = frequency,
                        babyId = babyId,
                        babyName = babyName,
                        isDone = isDone
                    )

                    tempList.add(scheduleItem)
                }

                tempList.sortBy { it.time }

                scheduleList.clear()
                scheduleList.addAll(tempList)
                scheduleAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "שגיאה בטעינת שמות התינוקות", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteSchedule(item: ScheduleWithBaby) {
        if (item.id.isBlank()) {
            Toast.makeText(this, "שגיאה: לא ניתן למחוק משימה ללא מזהה", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("families").document(familyId!!)
            .collection("schedule")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "המשימה נמחקה", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "שגיאה במחיקה: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
