package com.example.babycare

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: NotificationAdapter
    private lateinit var notifications: ArrayList<NotificationItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        db = FirebaseFirestore.getInstance()
        val sharedPref = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
        val familyId = sharedPref.getString("familyId", null)

        if (familyId == null) {
            Toast.makeText(this, "שגיאה: לא מחובר למשפחה", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        notifications = arrayListOf()
        adapter = NotificationAdapter(notifications)

        findViewById<RecyclerView>(R.id.recyclerViewNotifications).apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = this@NotificationsActivity.adapter
        }

        db.collection("families").document(familyId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "שגיאה בטעינת ההתראות", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                notifications.clear()

                value?.forEach { doc ->
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()
                    val formattedTime = timestamp?.let {
                        SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(it)
                    } ?: ""

                    val item = NotificationItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        timestamp = formattedTime
                    )

                    notifications.add(item)
                }

                adapter.notifyDataSetChanged()
            }
    }
}
