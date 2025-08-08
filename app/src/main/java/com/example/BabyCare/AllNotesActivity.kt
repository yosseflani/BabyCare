package com.example.babycare

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AllNotesActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: AllNotesGroupedAdapter
    private val groupedNotes = mutableListOf<NotesGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_notes)

        db = FirebaseFirestore.getInstance()
        adapter = AllNotesGroupedAdapter(groupedNotes, onDelete = { note, group ->
            deleteNote(note, group)
        })

        findViewById<RecyclerView>(R.id.recyclerViewAllNotes).apply {
            layoutManager = LinearLayoutManager(this@AllNotesActivity)
            adapter = this@AllNotesActivity.adapter
        }

        val familyId = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
            .getString("familyId", null)

        if (familyId == null) {
            Toast.makeText(this, "לא מחובר למשפחה", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadNotes(familyId)
    }


    private fun loadNotes(familyId: String) {
        db.collection("families").document(familyId)
            .collection("babies")
            .get()
            .addOnSuccessListener { babyDocs ->
                val babyIdToName = babyDocs.associateBy({ it.id }, { it.getString("name") ?: "לא ידוע" })
                db.collection("families").document(familyId)
                    .collection("notes").get().addOnSuccessListener { noteDocs ->

                        groupedNotes.clear()
                        val notesByBaby = mutableMapOf<String, MutableList<Note>>()

                        for (doc in noteDocs) {
                            val note = doc.toObject(Note::class.java).copy(id = doc.id)
                            notesByBaby.getOrPut(note.babyId) { mutableListOf() }.add(note)
                        }

                        for ((babyId, notes) in notesByBaby) {
                            val babyName = babyIdToName[babyId] ?: "לא ידוע"
                            val sortedNotes = notes.sortedByDescending { it.datetime }
                            groupedNotes.add(NotesGroup(babyName, ArrayList(sortedNotes)))
                        }

                        groupedNotes.sortBy { it.babyName }
                        adapter.notifyDataSetChanged()
                    }
            }
    }

    private fun deleteNote(note: Note, group: NotesGroup) {
        val familyId = getSharedPreferences("BabyCare", Context.MODE_PRIVATE).getString("familyId", null)
        if (familyId == null) return
        db.collection("families").document(familyId)
            .collection("notes").document(note.id)
            .delete()
            .addOnSuccessListener {
                group.notes.remove(note)
                if (group.notes.isEmpty()) {
                    groupedNotes.remove(group)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show()
            }
    }
}


data class NotesGroup(val babyName: String, val notes: ArrayList<Note>)
