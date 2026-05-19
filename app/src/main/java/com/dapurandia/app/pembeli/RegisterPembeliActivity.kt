package com.dapurandia.app.pembeli

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dapurandia.app.R
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import android.view.inputmethod.InputMethodManager

class RegisterPembeliActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_register)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootLayout)

        rootLayout.setOnClickListener {
            hideKeyboard()
        }


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        nameEditText = findViewById(R.id.namaEditText)
        phoneEditText = findViewById(R.id.noHpEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showAutoDismissDialog(
                    SweetAlertDialog.WARNING_TYPE,
                    "Peringatan",
                    "Semua kolom harus diisi!"
                )
                return@setOnClickListener
            }

            val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
            progressDialog.titleText = "Sedang mendaftar..."
            progressDialog.setCancelable(false)
            progressDialog.show()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        val pembeliData = hashMapOf(
                            "nama" to name,
                            "no_hp" to phone,
                            "email" to email,
                            "role" to "pembeli"
                        )

                        if (uid != null) {
                            firestore.collection("pembeli").document(uid)
                                .set(pembeliData)
                                .addOnSuccessListener {
                                    progressDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                                    progressDialog.titleText = "Berhasil!"
                                    progressDialog.contentText = "Registrasi berhasil dilakukan."
                                    hideConfirmAndAutoDismiss(progressDialog, true)
                                }
                                .addOnFailureListener {
                                    progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                                    progressDialog.titleText = "Oops..."
                                    progressDialog.contentText = "Gagal menyimpan data: ${it.message}"
                                    hideConfirmAndAutoDismiss(progressDialog, false)
                                }
                        }
                    } else {
                        progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                        progressDialog.titleText = "Registrasi Gagal"
                        progressDialog.contentText = getRegisterErrorMessage(task.exception)
                        hideConfirmAndAutoDismiss(progressDialog, false)
                    }
                }
        }
    }

    private fun showAutoDismissDialog(type: Int, title: String, message: String) {
        val dialog = SweetAlertDialog(this, type)
            .setTitleText(title)
            .setContentText(message)
        dialog.show()

        dialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismissWithAnimation()
        }, 2000)
    }

    private fun hideConfirmAndAutoDismiss(dialog: SweetAlertDialog, finishAfter: Boolean) {
        dialog.findViewById<Button>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismissWithAnimation()
            if (finishAfter) finish()
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

    private fun getRegisterErrorMessage(e: Exception?): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException ->
                "Password terlalu lemah, gunakan minimal 6 karakter."
            is FirebaseAuthUserCollisionException ->
                "Email sudah digunakan, silakan gunakan email lain."
            is FirebaseNetworkException ->
                "Tidak ada koneksi internet. Silakan periksa jaringan Anda."
            else ->
                "Registrasi gagal, silakan coba lagi."
        }
    }
}
