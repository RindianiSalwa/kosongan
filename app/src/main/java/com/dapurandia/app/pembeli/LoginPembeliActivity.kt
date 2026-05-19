package com.dapurandia.app.pembeli

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
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

class LoginPembeliActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerText: TextView
    private lateinit var forgotPasswordText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_login)

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
        registerText = findViewById(R.id.registerText)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)

        progressBar = findViewById(R.id.progressBar)
        progressBar.indeterminateTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.maroon_700))
        progressBar.visibility = View.GONE

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showErrorDialog("Email dan password tidak boleh kosong")
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val db = FirebaseFirestore.getInstance()

                        db.collection("pembeli").document(userId)
                            .get()
                            .addOnSuccessListener { document ->
                                progressBar.visibility = View.GONE
                                if (document.exists()) {
                                    startActivity(Intent(this, MainPembeliActivity::class.java))
                                    finish()
                                } else {
                                    auth.signOut()
                                    showErrorDialog("Login gagal, mohon cek ulang email dan password Anda.")
                                }
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                showErrorDialog("Gagal memeriksa role: ${e.message}")
                            }
                    } else {
                        progressBar.visibility = View.GONE
                        showErrorDialog(getLoginErrorMessage(task.exception))
                    }
                }
        }

        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordPembeliActivity::class.java)
            startActivity(intent)
        }

        val text = "Belum punya akun? Daftar di sini"
        val spannable = SpannableString(text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(this@LoginPembeliActivity, RegisterPembeliActivity::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@LoginPembeliActivity, R.color.blue_link)
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(
            clickableSpan,
            text.indexOf("Daftar di sini"),
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        registerText.text = spannable
        registerText.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun showErrorDialog(message: String) {
        val errorDialog = SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("Login Gagal")
            .setContentText(message)
        errorDialog.show()

        errorDialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            errorDialog.dismissWithAnimation()
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
