package com.dapurandia.app.kurir

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.LandingActivity
import com.dapurandia.app.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class KurirDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kurir_activity_dashboard)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarKurir)
        setSupportActionBar(toolbar)

        val btnPesananAktif = findViewById<MaterialButton>(R.id.btnPesananAktif)
        val btnRiwayatPesanan = findViewById<MaterialButton>(R.id.btnRiwayatPesanan)

        btnPesananAktif.setOnClickListener {
            val intent = Intent(this, PesananAktifActivity::class.java)
            startActivity(intent)
        }


        btnRiwayatPesanan.setOnClickListener {
            Toast.makeText(this, "Fitur riwayat pesanan belum tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_kurir_dashboard, menu)
        val logoutIcon = menu?.findItem(R.id.action_logout)?.icon
        logoutIcon?.setTint(resources.getColor(android.R.color.white, theme))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah yakin ingin logout?")
            .setPositiveButton("Ya") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LandingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
