package com.example.dapurandia.ui.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dapurandia.R
import com.example.dapurandia.ui.admin.AdminDashboardActivity

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Email wajib diisi"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password wajib diisi"
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Loading..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid
                    cekRole(uid)
                }
                .addOnFailureListener {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                    Toast.makeText(this, "Login gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvKeRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun cekRole(uid: String) {
        db.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.child("role").value.toString()
                val intent = when (role) {
                    "admin" -> Intent(this, AdminDashboardActivity::class.java)
                    "kurir" -> Intent(this, KurirDashboardActivity::class.java)
                    else    -> Intent(this, HomeActivity::class.java)
                }
                startActivity(intent)
                finish()
            }
    }
}