package com.dapurandia.app.pembeli

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dapurandia.app.LandingActivity
import com.dapurandia.app.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

object PembeliToolbarHelper {

    fun setup(
        fragment: Fragment,
        toolbar: Toolbar,
        title: String,
        onSearchClick: (() -> Unit)? = null
    ) {
        toolbar.title = title
        toolbar.setTitleTextColor(ContextCompat.getColor(fragment.requireContext(), android.R.color.white))
        toolbar.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.red_coral))
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.pembeli_menu_topbar)
        toolbar.overflowIcon?.setTint(ContextCompat.getColor(fragment.requireContext(), android.R.color.white))
        for (i in 0 until toolbar.menu.size()) {
            toolbar.menu.getItem(i).icon?.setTint(
                ContextCompat.getColor(fragment.requireContext(), android.R.color.white)
            )
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    if (onSearchClick != null) {
                        onSearchClick()
                    } else {
                        fragment.requireActivity()
                            .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                            ?.selectedItemId = R.id.nav_home
                    }
                    true
                }
                R.id.menu_cart -> {
                    fragment.startActivity(Intent(fragment.requireContext(), KeranjangActivity::class.java))
                    true
                }
                R.id.action_logout -> {
                    AlertDialog.Builder(fragment.requireContext())
                        .setTitle("Logout")
                        .setMessage("Apakah yakin ingin logout?")
                        .setPositiveButton("Ya") { _, _ ->
                            FirebaseAuth.getInstance().signOut()
                            val intent = Intent(fragment.requireContext(), LandingActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            fragment.startActivity(intent)
                            fragment.requireActivity().finish()
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                    true
                }
                else -> false
            }
        }
    }
}
