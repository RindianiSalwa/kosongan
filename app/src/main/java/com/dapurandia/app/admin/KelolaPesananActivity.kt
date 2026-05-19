package com.dapurandia.app.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.databinding.AdminActivityKelolaPesananBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.dapurandia.app.admin.PesananPagerAdapter

class KelolaPesananActivity : AppCompatActivity() {

    private lateinit var binding: AdminActivityKelolaPesananBinding
    private val tabTitles = listOf("Menunggu", "Dimasak", "Diantar", "Selesai", "Batal")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminActivityKelolaPesananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarKelolaPesanan)
        supportActionBar?.title = "Kelola Pesanan"
        binding.toolbarKelolaPesanan.setNavigationOnClickListener {
            onBackPressed()
        }


        val adapter = PesananPagerAdapter(this)
        binding.viewPagerPesanan.adapter = adapter

        TabLayoutMediator(binding.tabLayoutPesanan, binding.viewPagerPesanan) { tab, position ->
            tab.text = tabTitles[position]

            if (tabTitles[position] == "Batal") {
                tab.view.post {
                    val tabTextView = tab.view.getChildAt(1) as? android.widget.TextView
                    tabTextView?.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
            }
        }.attach()

    }
}
