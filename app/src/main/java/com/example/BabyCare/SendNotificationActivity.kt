package com.example.babycare

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SendNotificationActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val SERVER_KEY = "BGUQIAoDFj7dbI2Ur_6AvNWtbTcQnjNXwOYGQ8Hb0-suw_sZ4Qv7qF01ub8d7qMgAEmK5ZwK6sDKbivOzK0oXIs"
    private val FCM_API_URL = "https://fcm.googleapis.com/fcm/send"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_notification)

        val etTitle = findViewById<EditText>(R.id.etNotificationTitle)
        val etBody = findViewById<EditText>(R.id.etNotificationBody)
        val btnSend = findViewById<Button>(R.id.btnSendNotification)

        btnSend.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val body = etBody.text.toString().trim()

            if (title.isEmpty() || body.isEmpty()) {
                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val familyId = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
                .getString("familyId", null)

            if (familyId == null) {
                Toast.makeText(this, "שגיאה: לא נמצא מזהה משפחה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendNotificationToBackend(familyId, title, body)
        }
    }

    private fun sendNotificationToBackend(topic: String, title: String, body: String) {
        val json = JSONObject().apply {
            put("topic", "family_$topic")
            put("title", title)
            put("body", body)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(FCM_API_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@SendNotificationActivity,
                        "שגיאה בשליחה לשרת: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@SendNotificationActivity,
                            "התראה נשלחה בהצלחה!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@SendNotificationActivity,
                            "שגיאת שרת: ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }
}
