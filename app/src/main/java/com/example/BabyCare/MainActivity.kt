package com.example.babycare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var familyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
        familyId = sharedPref.getString("familyId", null)

        if (familyId.isNullOrEmpty()) {
            Toast.makeText(this, "לא מחובר למשפחה", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, FamilySetupActivity::class.java))
            finish()
            return
        }

        findViewById<Button>(R.id.btnBabyList)?.setOnClickListener {
            startActivity(Intent(this, BabyListActivity::class.java))
        }

        findViewById<Button>(R.id.btnAddBaby)?.setOnClickListener {
            startActivity(Intent(this, AddBabyActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewAllChecklists)?.setOnClickListener {
            startActivity(Intent(this, AllChecklistsActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewAllNotes)?.setOnClickListener {
            startActivity(Intent(this, AllNotesActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewAllSchedules)?.setOnClickListener {
            startActivity(Intent(this, AllSchedulesActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM Token", token)
            } else {
                Log.e("FCM Token", "שגיאה בקבלת טוקן: ${task.exception}")
            }
        }

        FirebaseMessaging.getInstance().subscribeToTopic("family_$familyId")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "נרשמת ל-topic family_$familyId")
                } else {
                    Log.e("MainActivity", "נכשל הרישום ל-topic", task.exception)
                }
            }

        scheduleDailyChecklistWorker(this)
    }

    private fun scheduleDailyChecklistWorker(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<ChecklistGeneratorWorker>(
            1, TimeUnit.DAYS
        ).setInitialDelay(10, TimeUnit.SECONDS).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyChecklistGenerator",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        Log.d("MainActivity", "ChecklistGeneratorWorker נרשם")
    }
}
