package com.dapurandia.app.pembeli

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.dapurandia.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var textViewNama: TextView
    private lateinit var textViewNoHP: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var buttonEditProfile: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.pembeli_fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileImage = view.findViewById(R.id.profileImage)
        textViewNama = view.findViewById(R.id.textViewNama)
        textViewNoHP = view.findViewById(R.id.textViewNoHP)
        textViewEmail = view.findViewById(R.id.textViewEmail)
        buttonEditProfile = view.findViewById(R.id.buttonEditProfile)
        PembeliToolbarHelper.setup(this, view.findViewById(R.id.topAppBar), "Profil")

        buttonEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfilePembeliActivity::class.java)
            startActivity(intent)
        }

        loadProfileData()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun loadProfileData() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("pembeli").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    textViewNama.text = document.getString("nama") ?: "-"
                    textViewNoHP.text = document.getString("no_hp") ?: "-"
                    textViewEmail.text = document.getString("email") ?: auth.currentUser?.email ?: "-"

                    val photoUrl = document.getString("fotoProfil")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_default_profile)
                            .into(profileImage)
                    } else {
                        profileImage.setImageResource(R.drawable.ic_default_profile)
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Data pembeli tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Gagal memuat data profil",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
