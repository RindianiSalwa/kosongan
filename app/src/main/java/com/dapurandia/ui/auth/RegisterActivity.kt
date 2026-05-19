package com.example.dapurandia.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dapurandia.R
class RegisterActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        btnRegister.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val noHp = etNoHp.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nama.isEmpty()) {
                etNama.error = "Nama wajib diisi"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                etEmail.error = "Email wajib diisi"
                return@setOnClickListener
            }
            if (noHp.isEmpty()) {
                etNoHp.error = "Nomor HP wajib diisi"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Password wajib diisi"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Password minimal 6 karakter"
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Loading..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid

                    val dataUser = mapOf(
                        "uid"   to uid,
                        "nama"  to nama,
                        "email" to email,
                        "noHp"  to noHp,
                        "role"  to "pembeli"
                    )

                    db.child("users").child(uid).setValue(dataUser)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Akun berhasil dibuat!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            btnRegister.isEnabled = true
                            btnRegister.text = "Daftar"
                            Toast.makeText(this, "Gagal simpan data", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Daftar"
                    Toast.makeText(this, "Registrasi gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvKeLogin.setOnClickListener {
            finish()
        }
    }
}