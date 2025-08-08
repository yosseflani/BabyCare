package com.example.babycare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var checkBoxRemember: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        checkBoxRemember = findViewById(R.id.checkBoxRemember)

        val sharedPref = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
        val rememberMe = sharedPref.getBoolean("rememberMe", false)

        if (auth.currentUser != null && rememberMe) {
            showLoading(true)
            checkFamilyStatus()
            return
        }

        btnLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            if (validateInputs(email, password)) {
                sharedPref.edit().putBoolean("rememberMe", checkBoxRemember.isChecked).apply()
                signIn(email, password)
            }
        }

        btnRegister.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            if (validateInputs(email, password)) {
                sharedPref.edit().putBoolean("rememberMe", checkBoxRemember.isChecked).apply()
                registerUser(email, password)
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                editTextEmail.error = "נא להזין אימייל"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                editTextEmail.error = "אימייל לא תקין"
                false
            }
            password.isEmpty() -> {
                editTextPassword.error = "נא להזין סיסמה"
                false
            }
            password.length < 6 -> {
                editTextPassword.error = "הסיסמה חייבת להכיל לפחות 6 תווים"
                false
            }
            else -> true
        }
    }

    private fun signIn(email: String, password: String) {
        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signIn: הצלחה")
                    checkFamilyStatus()
                } else {
                    showLoading(false)
                    handleAuthError(task.exception)
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "registerUser: הצלחה")
                    createUserDocument()
                } else {
                    showLoading(false)
                    handleAuthError(task.exception)
                }
            }
    }

    private fun createUserDocument() {
        val user = auth.currentUser ?: run {
            showLoading(false)
            return
        }

        val userData = hashMapOf(
            "userId" to user.uid,
            "email" to user.email,
            "familyId" to null,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                Log.d("LoginActivity", "createUserDocument: נשמר בהצלחה")
                startActivity(Intent(this, FamilySetupActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "שגיאה בשמירת נתוני משתמש", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkFamilyStatus() {
        val user = auth.currentUser ?: run {
            showLoading(false)
            startActivity(Intent(this, FamilySetupActivity::class.java))
            return
        }

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                val familyId = document.getString("familyId")
                Log.d("LoginActivity", "checkFamilyStatus: familyId=$familyId")
                val sharedPref = getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
                if (!familyId.isNullOrEmpty()) {
                    sharedPref.edit().putString("familyId", familyId).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    sharedPref.edit().remove("familyId").apply()
                    startActivity(Intent(this, FamilySetupActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "שגיאה בבדיקת סטטוס משפחה", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleAuthError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthInvalidUserException -> "משתמש לא קיים"
            is FirebaseAuthInvalidCredentialsException -> "אימייל או סיסמה לא נכונים"
            is FirebaseAuthUserCollisionException -> "האימייל כבר רשום במערכת"
            is FirebaseAuthWeakPasswordException -> "הסיסמה חלשה מדי"
            else -> "שגיאת התחברות: ${exception?.localizedMessage}"
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        Log.e("LoginActivity", "handleAuthError: ${exception?.localizedMessage}")
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnRegister.isEnabled = !show
    }
}
