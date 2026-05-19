package com.dapurandia.app.pembeli

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dapurandia.app.databinding.PembeliFragmentDeliveryBinding
import com.google.android.material.tabs.TabLayoutMediator

class DeliveryFragment : Fragment() {

    private var _binding: PembeliFragmentDeliveryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DeliveryPagerAdapter

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PembeliFragmentDeliveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = DeliveryPagerAdapter(requireActivity())
        binding.viewPagerPesanan.adapter = adapter

        TabLayoutMediator(binding.tabLayoutPesanan, binding.viewPagerPesanan) { tab, position ->
            tab.text = when (position) {
                0 -> "Menunggu"
                1 -> "Dimasak"
                2 -> "Diantar"
                3 -> "Selesai"
                4 -> "Batal"
                else -> ""
            }
            if (position == 4) {
                tab.view.post {
                    val tabTextView = tab.view.getChildAt(1) as? android.widget.TextView
                    tabTextView?.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
