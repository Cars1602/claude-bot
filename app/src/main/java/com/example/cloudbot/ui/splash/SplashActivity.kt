package com.example.cloudbot.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.cloudbot.databinding.ActivitySplashBinding
import com.example.cloudbot.ui.devices.DeviceListActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var b: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(b.root)

        playIntro()
    }

    private fun playIntro() {
        val titleFade = ObjectAnimator.ofFloat(b.txtCloudBot, "alpha", 0f, 1f).apply {
            duration = 650
        }
        val titleRise = ObjectAnimator.ofFloat(b.txtCloudBot, "translationY", 24f, 0f).apply {
            duration = 650
        }
        val taglineFade = ObjectAnimator.ofFloat(b.txtTagline, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 180
        }
        val dotFade = ObjectAnimator.ofFloat(b.dotIndicator, "alpha", 0f, 1f).apply {
            duration = 450
            startDelay = 300
        }

        AnimatorSet().apply {
            playTogether(titleFade, titleRise, taglineFade, dotFade)
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        b.root.postDelayed({
            startActivity(Intent(this, DeviceListActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1900L)
    }
}
