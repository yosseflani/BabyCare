package com.example.babycare

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ChecklistGeneratorWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()
    private val sharedPref = context.getSharedPreferences("BabyCare", Context.MODE_PRIVATE)

    override fun doWork(): Result {
        val familyId = sharedPref.getString("familyId", null)
        if (familyId.isNullOrEmpty()) {
            Log.e("ChecklistWorker", "Missing familyId")
            return Result.failure()
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return try {
            val scheduleSnapshot = Tasks.await(
                db.collection("families").document(familyId)
                    .collection("schedule")
                    .get()
            )

            val tasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

            for (doc in scheduleSnapshot.documents) {
                val title = doc.getString("title") ?: continue
                val babyId = doc.getString("babyId") ?: continue
                val description = doc.getString("description") ?: ""
                val time = doc.getString("time") ?: ""

                val checklistItem = hashMapOf(
                    "title" to title,
                    "description" to description,
                    "time" to time,
                    "babyId" to babyId,
                    "date" to today,
                    "isCompleted" to false
                )

                val task = db.collection("families").document(familyId)
                    .collection("checklist")
                    .add(checklistItem)

                tasks.add(task)
            }

            Tasks.await(Tasks.whenAll(tasks))
            Log.d("ChecklistWorker", "Checklist daily items created for $today")
            return Result.success()
        } catch (e: Exception) {
            Log.e("ChecklistWorker", "Error generating checklist", e)
            return Result.failure()
        }
    }
}
