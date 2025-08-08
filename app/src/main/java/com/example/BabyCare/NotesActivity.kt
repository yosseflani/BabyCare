package com.example.babycare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: NoteAdapter
    private lateinit var noteList: ArrayList<Note>
    private var babyId: String? = null
    private var familyId: String? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoNotes: TextView
    private lateinit var btnAddNote: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        babyId = intent.getStringExtra("babyId")
        familyId = getSharedPreferences("BabyCare", Context.MODE_PRIVATE).getString("familyId", null)

        if (babyId == null || familyId == null) {
            Toast.makeText(this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerViewNotes)
        tvNoNotes = findViewById(R.id.textNoNotes)
        btnAddNote = findViewById(R.id.btnAddNote)

        noteList = ArrayList()
        adapter = NoteAdapter(familyId!!, babyId!!, noteList)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnAddNote.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            intent.putExtra("babyId", babyId)
            startActivity(intent)
        }

        loadNotes()
    }

    private fun loadNotes() {
        db = FirebaseFirestore.getInstance()

        db.collection("families").document(familyId!!)
            .collection("notes")
            .whereEqualTo("babyId", babyId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "שגיאה בטעינת הערות", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                noteList.clear()
                value?.forEach { doc ->
                    val note = doc.toObject(Note::class.java).copy(id = doc.id)
                    noteList.add(note)
                }

                adapter.notifyDataSetChanged()
                tvNoNotes.visibility = if (noteList.isEmpty()) View.VISIBLE else View.GONE
            }
    }
}
