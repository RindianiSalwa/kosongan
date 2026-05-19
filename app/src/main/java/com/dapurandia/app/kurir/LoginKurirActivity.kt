package com.dapurandia.app.kurir

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dapurandia.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import android.view.inputmethod.InputMethodManager


class LoginKurirActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kurir_activity_login)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)

        rootLayout.setOnClickListener {
            hideKeyboard()
        }


        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        progressBar = findViewById(R.id.progressBar)

        progressBar.indeterminateTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.maroon_700))
        progressBar.visibility = View.GONE

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showAutoDismissDialog(
                    "Login Gagal",
                    "Email dan password tidak boleh kosong"
                )
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val db = FirebaseFirestore.getInstance()

                        // ⚠️ CUMA INI YANG BEDA: collection "kurir"
                        db.collection("kurir").document(userId)
                            .get()
                            .addOnSuccessListener { document ->
                                progressBar.visibility = View.GONE
                                if (document.exists()) {
                                    startActivity(
                                        Intent(
                                            this,
                                            KurirDashboardActivity::class.java
                                        )
                                    )
                                    finish()
                                } else {
                                    auth.signOut()
                                    showAutoDismissDialog(
                                        "Login gagal",
                                        "mohon cek ulang email dan password Anda."
                                    )
                                }
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                showAutoDismissDialog(
                                    "Error",
                                    "Gagal memeriksa role: ${e.message}"
                                )
                            }
                    } else {
                        progressBar.visibility = View.GONE
                        showAutoDismissDialog(
                            "Login Gagal",
                            getLoginErrorMessage(task.exception)
                        )
                    }
                }
        }
    }

    private fun showAutoDismissDialog(title: String, message: String) {
        val dialog = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(message)
        dialog.show()

        dialog.findViewById<Button>(
            cn.pedant.SweetAlert.R.id.confirm_button
        )?.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismissWithAnimation()
        }, 2000)
    }
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }


    private fun getLoginErrorMessage(e: Exception?): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException ->
                "Login gagal, mohon cek ulang email dan password Anda."
            is FirebaseAuthInvalidUserException ->
                "Akun tidak ditemukan atau telah dinonaktifkan."
            is FirebaseNetworkException ->
                "Tidak ada koneksi internet. Silakan periksa jaringan Anda."
            else ->
                "Login gagal, silakan coba lagi."
        }
    }
}
