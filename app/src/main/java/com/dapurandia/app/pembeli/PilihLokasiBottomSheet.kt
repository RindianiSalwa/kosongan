package com.dapurandia.app.pembeli

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dapurandia.app.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import java.util.Locale

class PilihLokasiBottomSheet(
    private val latAwal: Double?,
    private val lngAwal: Double?,
    private val alamatAwal: String?,
    private val onAlamatDipilih: (Double, Double, String, String) -> Unit
) : BottomSheetDialogFragment() {

    // DI DALAM PilihLokasiBottomSheet.kt
    private val pilihLokasiLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null) {

                val lat = result.data!!.getDoubleExtra("LAT", 0.0)
                val lng = result.data!!.getDoubleExtra("LNG", 0.0)

                // 🔥 AMBIL ALAMAT UTAMA DARI INTENT (HASIL GEOCODER DI ACTIVITY)
                val alamatUtama = result.data!!.getStringExtra("ALAMAT") ?: "-"
                val alamatDetail = result.data!!.getStringExtra("ALAMAT_DETAIL") ?: "-"

                // Tidak perlu Geocoder lagi di sini karena PilihLokasiActivity sudah melakukannya
                onAlamatDipilih(
                    lat,
                    lng,
                    alamatUtama,
                    alamatDetail
                )

                dismiss()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_pilih_lokasi, container, false)

        val btnPinMap = view.findViewById<MaterialButton>(R.id.btnPinMap)
        val btnLokasiSaatIni= view.findViewById<MaterialButton>(R.id.btnLokasiSaatIni)

        btnPinMap.setOnClickListener {
            val intent = Intent(requireContext(), PilihLokasiActivity::class.java).apply {
                putExtra("LAT_AWAL", latAwal)
                putExtra("LNG_AWAL", lngAwal)
                putExtra("ALAMAT", alamatAwal)
            }
            pilihLokasiLauncher.launch(intent)
        }
        btnLokasiSaatIni.setOnClickListener {
            val intent = Intent(requireContext(), LokasiSaatIniActivity::class.java)
            pilihLokasiLauncher.launch(intent)
        }


        return view
    }
}
