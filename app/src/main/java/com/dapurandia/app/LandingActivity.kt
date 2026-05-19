package com.dapurandia.app

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import cn.pedant.SweetAlert.SweetAlertDialog
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.admin.LoginAdminActivity
import com.dapurandia.app.kurir.LoginKurirActivity
import com.dapurandia.app.pembeli.LoginPembeliActivity
import com.dapurandia.app.pembeli.RegisterPembeliActivity
import com.google.android.material.button.MaterialButton

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        val actRole = findViewById<AutoCompleteTextView>(R.id.actRole)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val txtRegister = findViewById<TextView>(R.id.txtRegister)

        val roleList = listOf("Pembeli", "Kurir", "Admin")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roleList)
        actRole.setAdapter(adapter)
        actRole.keyListener = null
        actRole.setOnClickListener { actRole.showDropDown() }

        btnLogin.setOnClickListener {
            val selectedRole = actRole.text.toString()

            if (selectedRole.isBlank()) {
                val dialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                dialog.titleText = "Oops 😅"
                dialog.contentText = "Silakan pilih peran terlebih dahulu"
                dialog.setCancelable(true)
                dialog.show()
                dialog.findViewById<View>(cn.pedant.SweetAlert.R.id.confirm_button)?.visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({ dialog.dismissWithAnimation() }, 1500)
                return@setOnClickListener
            }

            when (selectedRole) {
                "Pembeli" -> startActivity(Intent(this, LoginPembeliActivity::class.java))
                "Kurir" -> startActivity(Intent(this, LoginKurirActivity::class.java))
                "Admin" -> startActivity(Intent(this, LoginAdminActivity::class.java))
            }
        }

        txtRegister.setOnClickListener {
            startActivity(Intent(this, RegisterPembeliActivity::class.java))
        }
    }
}