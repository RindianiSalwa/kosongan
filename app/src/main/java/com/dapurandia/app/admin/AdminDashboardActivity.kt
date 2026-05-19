package com.dapurandia.app.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.airbnb.lottie.LottieAnimationView
import com.dapurandia.app.LandingActivity
import com.dapurandia.app.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_activity_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAdmin)
        setSupportActionBar(toolbar)

        val cardKelolaMenu = findViewById<CardView>(R.id.cardKelolaMenu)
        val cardKelolaPesanan = findViewById<CardView>(R.id.cardKelolaPesanan)
        val cardRiwayatPesanan = findViewById<CardView>(R.id.cardRiwayatPesanan)
        val adminGreeting = findViewById<android.widget.TextView>(R.id.adminGreeting)
        val adminSubtext = findViewById<android.widget.TextView>(R.id.adminSubtext)
        val lottie = findViewById<LottieAnimationView>(R.id.adminImage)
        val btnKelolaMenu = findViewById<MaterialButton>(R.id.btnKelolaMenu)
        val btnKelolaPesanan = findViewById<MaterialButton>(R.id.btnKelolaPesanan)
        val btnRiwayatPesanan = findViewById<MaterialButton>(R.id.btnRiwayatPesanan)

        listOf(lottie, adminGreeting, adminSubtext, cardKelolaMenu, cardKelolaPesanan, cardRiwayatPesanan).forEach {
            it.alpha = 0f
            it.translationY = 40f
        }

        val delays = listOf(0L, 100L, 160L, 260L, 360L, 460L)
        listOf(lottie, adminGreeting, adminSubtext, cardKelolaMenu, cardKelolaPesanan, cardRiwayatPesanan)
            .zip(delays)
            .forEach { (view, delay) ->
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(400)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }

        btnKelolaMenu.setOnClickListener {
            startActivity(Intent(this, KelolaMenuActivity::class.java))
        }
        btnKelolaPesanan.setOnClickListener {
            startActivity(Intent(this, KelolaPesananActivity::class.java))
        }
        btnRiwayatPesanan.setOnClickListener {
            startActivity(Intent(this, RiwayatPesananActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_admin_dashboard, menu)
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