package com.dapurandia.app.admin


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dapurandia.app.admin.*
class PesananPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MenungguFragment()
            1 -> DimasakFragment()
            2 -> DiantarFragment()
            3 -> SelesaiFragment()
            4 -> DibatalkanFragment()
            else -> MenungguFragment() // fallback
        }
    }
}
