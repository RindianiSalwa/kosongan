package com.dapurandia.app.pembeli

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DeliveryPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PesananMenungguFragment()
            1 -> PesananDimasakFragment()
            2 -> PesananDiantarFragment()
            3 -> PesananSelesaiFragment()
            4 -> PesananDibatalkanFragment()
            else -> PesananMenungguFragment()
        }
    }
}
