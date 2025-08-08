package com.example.babycare

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AllChecklistsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AllChecklistsAdapter
    private val allChecklistItems = arrayListOf<ChecklistWithBabyName>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_checklists)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAllChecklists)
        recyclerView.layoutManager = LinearLayoutManager(this)

        db = FirebaseFirestore.getInstance()
        val familyId = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
            .getString("familyId", null)

        if (familyId == null) {
            Toast.makeText(this, "לא נמצא מזהה משפחה", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = AllChecklistsAdapter(
            allChecklistItems,
            onCheckedChange = { checklistItem, isChecked ->
                db.collection("families").document(familyId)
                    .collection("checklist").document(checklistItem.id)
                    .update("isCompleted", isChecked)
            },
            onDelete = { checklistItem ->
                db.collection("families").document(familyId)
                    .collection("checklist").document(checklistItem.id)
                    .delete()
                    .addOnSuccessListener {
                        allChecklistItems.removeAll { it.checklist.id == checklistItem.id }
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this, "המשימה נמחקה", Toast.LENGTH_SHORT).show()
                    }
            }
        )

        recyclerView.adapter = adapter

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        loadAllChecklists(familyId, today)
    }

    private fun loadAllChecklists(familyId: String, date: String) {
        db.collection("families").document(familyId)
            .collection("checklist")
            .whereEqualTo("date", date)
            .addSnapshotListener { checklistDocs, error ->
                if (error != null || checklistDocs == null) {
                    Toast.makeText(this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val babyIdToName = mutableMapOf<String, String>()
                db.collection("families").document(familyId).collection("babies")
                    .get()
                    .addOnSuccessListener { babyDocs ->
                        babyIdToName.putAll(babyDocs.associate {
                            it.id to (it.getString("name") ?: "תינוק")
                        })

                        allChecklistItems.clear()
                        for (doc in checklistDocs) {
                            val item = doc.toObject(ChecklistItem::class.java).copy(id = doc.id)
                            val babyName = babyIdToName[item.babyId] ?: "תינוק לא ידוע"
                            allChecklistItems.add(ChecklistWithBabyName(item, babyName))
                        }

                        allChecklistItems.sortWith(compareBy {
                            try {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).parse(it.checklist.time)
                            } catch (e: Exception) {
                                null
                            }
                        })
                        adapter.notifyDataSetChanged()
                    }
            }
    }
}
