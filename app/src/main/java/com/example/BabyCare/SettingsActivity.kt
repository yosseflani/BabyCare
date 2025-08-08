package com.example.babycare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val emailText = findViewById<TextView>(R.id.textUserEmail)
        val user = auth.currentUser

        if (emailText == null) {
            Log.e("SettingsActivity", "TextView לא נמצא ב-layout")
            Toast.makeText(this, "שגיאת מערכת: לא נמצא textUserEmail ב-layout", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (user == null) {
            Log.e("SettingsActivity", "משתמש לא מחובר")
            Toast.makeText(this, "לא נמצא משתמש מחובר. אנא התחבר מחדש.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        emailText.text = user.email ?: "משתמש לא מזוהה"

        findViewById<Button>(R.id.btnSignOut).setOnClickListener {
            auth.signOut()
            getSharedPreferences("BabyCare", Context.MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        findViewById<Button>(R.id.btnLeaveFamily).setOnClickListener {
            val sharedPref = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
            val familyId = sharedPref.getString("familyId", null)
            val userId = user.uid

            if (familyId != null) {
                db.collection("families").document(familyId)
                    .get()
                    .addOnSuccessListener { document ->
                        val members = (document.get("members") as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                        members.remove(userId)
                        db.collection("families").document(familyId)
                            .update("members", members)
                            .addOnSuccessListener {
                                Log.d("SettingsActivity", "המשתמש הוסר מהמשפחה")
                            }
                            .addOnFailureListener { e ->
                                Log.e("SettingsActivity", "שגיאה בעדכון חברי המשפחה: ${e.localizedMessage}")
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("SettingsActivity", "שגיאה בשליפת מסמך משפחה: ${e.localizedMessage}")
                    }
                db.collection("users").document(userId).update("familyId", null)
            }

            sharedPref.edit().remove("familyId").apply()
            Toast.makeText(this, "יצאת מהמשפחה", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, FamilySetupActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        findViewById<Button>(R.id.btnBackToMenu).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }
}
