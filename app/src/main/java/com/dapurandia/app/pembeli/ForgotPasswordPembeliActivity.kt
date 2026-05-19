package com.dapurandia.app.pembeli

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.dapurandia.app.BuildConfig
import com.dapurandia.app.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ForgotPasswordPembeliActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private lateinit var phoneEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var sendResetButton: Button
    private lateinit var progressBar: ProgressBar
    private var resetSessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_forgot_password)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        phoneEditText = findViewById(R.id.phoneEditText)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        sendResetButton = findViewById(R.id.sendResetButton)
        progressBar = findViewById(R.id.progressBar)

        progressBar.indeterminateTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.maroon_700))
        progressBar.visibility = View.GONE

        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        rootLayout.setOnClickListener {
            hideKeyboard()
        }

        sendResetButton.setOnClickListener {
            requestPasswordResetOtp()
        }
    }

    private fun requestPasswordResetOtp() {
        val phone = phoneEditText.text.toString().trim()
        val newPassword = newPasswordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        when {
            phone.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                showAutoDismissDialog(
                    SweetAlertDialog.WARNING_TYPE,
                    "Peringatan",
                    "Nomor WhatsApp dan kata sandi baru harus diisi."
                )
                return
            }
            newPassword.length < 6 -> {
                showAutoDismissDialog(
                    SweetAlertDialog.WARNING_TYPE,
                    "Password Lemah",
                    "Gunakan minimal 6 karakter."
                )
                return
            }
            newPassword != confirmPassword -> {
                showAutoDismissDialog(
                    SweetAlertDialog.WARNING_TYPE,
                    "Password Berbeda",
                    "Ulangi kata sandi baru dengan benar."
                )
                return
            }
        }

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Mengirim OTP..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val payload = JSONObject()
            .put("phone", phone)
            .toString()
            .toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(BuildConfig.PASSWORD_RESET_REQUEST_URL)
            .post(payload)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                    progressDialog.titleText = "OTP Gagal Dikirim"
                    progressDialog.contentText = "Periksa koneksi internet Anda."
                    hideConfirmAndAutoDismiss(progressDialog, false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(responseBody) }.getOrNull()
                val sessionId = json?.optString("sessionId").orEmpty()
                val message = json?.optString("message").orEmpty()

                runOnUiThread {
                    if (response.isSuccessful && sessionId.isNotBlank()) {
                        resetSessionId = sessionId
                        progressDialog.dismissWithAnimation()
                        showOtpDialog()
                    } else {
                        progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                        progressDialog.titleText = "OTP Gagal Dikirim"
                        progressDialog.contentText = message.ifEmpty { "Nomor WhatsApp tidak ditemukan." }
                        hideConfirmAndAutoDismiss(progressDialog, false)
                    }
                }
            }
        })
    }

    private fun showOtpDialog() {
        val otpInput = EditText(this).apply {
            hint = "Masukkan kode OTP"
            inputType = InputType.TYPE_CLASS_NUMBER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        val container = FrameLayout(this).apply {
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_padding)
            setPadding(padding, 0, padding, 0)
            addView(otpInput)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Verifikasi OTP")
            .setMessage("Kode OTP sudah dikirim ke WhatsApp. Masukkan kode untuk mengganti kata sandi.")
            .setView(container)
            .setPositiveButton("Verifikasi", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val otp = otpInput.text.toString().trim()
                if (otp.isEmpty()) {
                    otpInput.error = "Kode OTP wajib diisi"
                } else {
                    dialog.dismiss()
                    confirmPasswordResetOtp(otp)
                }
            }
        }
        dialog.show()
    }

    private fun confirmPasswordResetOtp(otp: String) {
        val sessionId = resetSessionId
        val newPassword = newPasswordEditText.text.toString().trim()
        if (sessionId.isNullOrBlank()) {
            showAutoDismissDialog(
                SweetAlertDialog.ERROR_TYPE,
                "Reset Gagal",
                "Sesi reset tidak ditemukan. Silakan kirim OTP ulang."
            )
            return
        }

        val progressDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        progressDialog.titleText = "Mengganti password..."
        progressDialog.setCancelable(false)
        progressDialog.show()

        val payload = JSONObject()
            .put("sessionId", sessionId)
            .put("otp", otp)
            .put("newPassword", newPassword)
            .toString()
            .toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(BuildConfig.PASSWORD_RESET_CONFIRM_URL)
            .post(payload)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                    progressDialog.titleText = "Reset Gagal"
                    progressDialog.contentText = "Periksa koneksi internet Anda."
                    hideConfirmAndAutoDismiss(progressDialog, false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(responseBody) }.getOrNull()
                val message = json?.optString("message").orEmpty()

                runOnUiThread {
                    if (response.isSuccessful) {
                        resetSessionId = null
                        progressDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                        progressDialog.titleText = "Password Berhasil Diganti"
                        progressDialog.contentText = "Silakan login dengan kata sandi baru."
                        hideConfirmAndAutoDismiss(progressDialog, true)
                    } else {
                        progressDialog.changeAlertType(SweetAlertDialog.ERROR_TYPE)
                        progressDialog.titleText = "Reset Gagal"
                        progressDialog.contentText = message.ifEmpty { "OTP salah atau sudah kedaluwarsa." }
                        hideConfirmAndAutoDismiss(progressDialog, false)
                    }
                }
            }
        })
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
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }
}
