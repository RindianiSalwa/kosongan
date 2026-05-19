package com.example.dapurandia.ui.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dapurandia.R

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            cekStatusLogin()
        }, 2000)
    }

    private fun cekStatusLogin() {
        val user = auth.currentUser

        if (user == null) {
            keHalamanLogin()
        } else {
            db.child("users").child(user.uid).get()
                .addOnSuccessListener { snapshot ->
                    val role = snapshot.child("role").value.toString()
                    when (role) {
                        "admin" -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                        "kurir" -> startActivity(Intent(this, KurirDashboardActivity::class.java))
                        else    -> startActivity(Intent(this, HomeActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    keHalamanLogin()
                }
        }
    }

    private fun keHalamanLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}