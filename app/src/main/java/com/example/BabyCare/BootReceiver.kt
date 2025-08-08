package com.example.babycare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "BOOT_COMPLETED received, restoring checklist alarms")

            val sharedPref = context.getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
            val familyId = sharedPref.getString("familyId", null)

            if (familyId.isNullOrEmpty()) {
                Log.w("BootReceiver", "No familyId found, skipping alarm restore")
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val db = FirebaseFirestore.getInstance()

                    val snapshot = db.collection("families").document(familyId)
                        .collection("checklist")
                        .whereEqualTo("date", today)
                        .get()
                        .await()

                    for (doc in snapshot.documents) {
                        val item = doc.toObject(ChecklistItem::class.java)?.copy(id = doc.id)
                        if (item != null) {
                            ChecklistAlarmScheduler.scheduleChecklistReminder(context, item)
                        }
                    }

                    Log.d("BootReceiver", "Checklist alarms restored successfully")

                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to restore checklist alarms", e)
                }
            }
        }
    }
}
