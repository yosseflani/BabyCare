package com.example.babycare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FamilySetupActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.family_setup)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            Toast.makeText(this, "יש להתחבר קודם", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        findViewById<Button>(R.id.btnCreateFamily).setOnClickListener {
            createNewFamily()
        }

        findViewById<Button>(R.id.btnJoinFamily).setOnClickListener {
            joinExistingFamily()
        }
    }

    private fun createNewFamily() {
        val userId = auth.currentUser?.uid ?: return showError("משתמש לא מחובר")

        val familyData = hashMapOf(
            "familyId" to userId,
            "members" to listOf(userId)
        )

        db.collection("families").document(userId)
            .set(familyData)
            .addOnSuccessListener {
                Log.d("FamilySetup", "משפחה חדשה נוצרה בהצלחה")
                saveFamilyIdToPrefs(userId)
                updateUserFamily(userId, userId)
            }
            .addOnFailureListener { e ->
                Log.e("FamilySetup", "שגיאה ביצירת משפחה: ${e.localizedMessage}")
                showError("שגיאה ביצירת משפחה")
            }
    }

    private fun joinExistingFamily() {
        val familyId = findViewById<EditText>(R.id.editTextFamilyId).text.toString().trim()
        if (familyId.isEmpty()) return showError("אנא הזן מזהה משפחה")

        val userId = auth.currentUser?.uid ?: return showError("משתמש לא מחובר")

        db.collection("families").document(familyId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.w("FamilySetup", "משפחה לא קיימת: $familyId")
                    return@addOnSuccessListener showError("משפחה לא קיימת")
                }

                val members = (document.get("members") as? List<*>)
                    ?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()

                if (!members.contains(userId)) {
                    members.add(userId)
                    db.collection("families").document(familyId)
                        .update("members", members)
                        .addOnSuccessListener {
                            Log.d("FamilySetup", "הצטרפת למשפחה $familyId")
                            saveFamilyIdToPrefs(familyId)
                            updateUserFamily(userId, familyId)
                        }
                        .addOnFailureListener { e ->
                            Log.e("FamilySetup", "שגיאה בעדכון חברים: ${e.localizedMessage}")
                            showError("שגיאה בעדכון חברים")
                        }
                } else {
                    saveFamilyIdToPrefs(familyId)
                    updateUserFamily(userId, familyId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FamilySetup", "שגיאה בחיפוש משפחה: ${e.localizedMessage}")
                showError("שגיאה בחיפוש משפחה")
            }
    }

    private fun updateUserFamily(userId: String, familyId: String) {
        db.collection("users").document(userId)
            .update("familyId", familyId)
            .addOnSuccessListener {
                Log.d("FamilySetup", "עודכן familyId למשתמש $userId")
                openMainActivity()
            }
            .addOnFailureListener { e ->
                Log.e("FamilySetup", "שגיאה בעדכון משתמש: ${e.localizedMessage}")
                showError("שגיאה בעדכון משתמש")
            }
    }

    private fun saveFamilyIdToPrefs(familyId: String) {
        getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
            .edit()
            .putString("familyId", familyId)
            .apply()
        Log.d("FamilySetup", "Saved familyId: $familyId")
    }

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
