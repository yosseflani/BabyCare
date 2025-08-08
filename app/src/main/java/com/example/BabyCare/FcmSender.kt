package com.example.babycare

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object FCMService {
    private const val FCM_API_URL = "https://fcm.googleapis.com/fcm/send"
    private const val SERVER_KEY = "key=YOUR_SERVER_KEY_HERE"

    fun sendNotificationToTopic(context: Context, topic: String, title: String, body: String) {
        val json = JSONObject().apply {
            put("to", "/topics/$topic")
            put("notification", JSONObject().apply {
                put("title", title)
                put("body", body)
            })
        }

        val requestBody = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(FCM_API_URL)
            .post(requestBody)
            .addHeader("Authorization", SERVER_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCMService", "כשלון בשליחת ההתראה", e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("FCMService", "התראה נשלחה: ${response.body?.string()}")
            }
        })

        val sharedPref = context.getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
        val familyId = sharedPref.getString("familyId", null)

        if (familyId != null) {
            val db = FirebaseFirestore.getInstance()
            val notification = mapOf(
                "title" to title,
                "body" to body,
                "timestamp" to FieldValue.serverTimestamp()
            )
            db.collection("families").document(familyId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener {
                    Log.d("FCMService", "התראה נשמרה למסד")
                }
                .addOnFailureListener {
                    Log.e("FCMService", "שגיאה בשמירה", it)
                }
        } else {
            Log.e("FCMService", "לא נמצא familyId ב-SharedPreferences")
        }
    }
}
