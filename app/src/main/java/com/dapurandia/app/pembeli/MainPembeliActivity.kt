package com.dapurandia.app.pembeli

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dapurandia.app.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainPembeliActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pembeli_activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            loadFragment(HomePembeliFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomePembeliFragment())
                    true
                }
                R.id.nav_delivery -> {
                    loadFragment(DeliveryFragment())
                    true
                }
                R.id.nav_history -> {
                    loadFragment(HistoryFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        Log.d("MainPembeliActivity", "Memuat fragment: ${fragment::class.java.simpleName}")

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(false)
            it.setHomeAsUpIndicator(null)
            it.setDisplayShowHomeEnabled(false)
            it.title = null
        } ?: Log.d("MainPembeliActivity", "Tidak ada ActionBar")
    }
}

