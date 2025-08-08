package com.example.babycare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class BabyListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BabyAdapter
    private val babyList = mutableListOf<Baby>()
    private lateinit var firestore: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private var familyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_baby_list)

        familyId = getSharedPreferences("BabyCare", MODE_PRIVATE).getString("familyId", null)
            ?: FirebaseAuth.getInstance().currentUser?.uid
        Log.d("BabyListActivity", "onCreate - familyId: $familyId")

        firestore = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.recyclerViewBabies)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = BabyAdapter(
            babyList,
            onBabyClick = { baby ->
                Log.d("BabyListActivity", "Clicked on baby: ${baby.name}, id=${baby.id}")
                val intent = Intent(this, BabyDetailsActivity::class.java)
                intent.putExtra("babyId", baby.id)
                startActivity(intent)
            }
        )

        recyclerView.adapter = adapter

        val btnAddBaby = findViewById<Button>(R.id.btnAddBaby)
        btnAddBaby.setOnClickListener {
            Log.d("BabyListActivity", "Add baby button clicked")
            val intent = Intent(this, AddBabyActivity::class.java)
            startActivity(intent)
        }

        loadBabies()
    }

    private fun loadBabies() {
        if (familyId == null) {
            Log.e("BabyListActivity", "familyId is null, cannot load babies")
            return
        }

        Log.d("BabyListActivity", "Loading babies for familyId: $familyId")

        listenerRegistration = firestore.collection("families")
            .document(familyId!!)
            .collection("babies")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BabyListActivity", "Error loading babies: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    babyList.clear()
                    for (doc in snapshot.documents) {
                        val baby = doc.toObject(Baby::class.java)
                        if (baby != null) {
                            baby.id = doc.id
                            babyList.add(baby)
                            Log.d("BabyListActivity", "Added baby: ${baby.name}")
                        }
                    }
                    adapter.notifyDataSetChanged()
                    Log.d("BabyListActivity", "Loaded ${babyList.size} babies")
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
