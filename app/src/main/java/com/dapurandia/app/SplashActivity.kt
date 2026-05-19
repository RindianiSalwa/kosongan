package com.dapurandia.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val lottie = findViewById<LottieAnimationView>(R.id.lottieAnimation)
        val title = findViewById<TextView>(R.id.tvDapurAndia)

        val popIn = AnimationUtils.loadAnimation(this, R.anim.pop_in)

        Handler(Looper.getMainLooper()).postDelayed({
            title.visibility = View.VISIBLE
            title.startAnimation(popIn)
        }, 800)

        lottie.playAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }, 5000)
    }
}
