package com.dhananjaysaini.musicplayerapp.activities

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dhananjaysaini.musicplayerapp.databinding.ActivitySplashBinding
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private val animatorList = mutableListOf<ValueAnimator>()
    private var isNavigated = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot) { _, insets ->
            insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            insets
        }

        setupInitialViewState()
        startSplashAnimations()

        handler.postDelayed({
            proceedToNextScreen()
        }, SPLASH_DURATION_MS)
    }

    private fun setupInitialViewState() {
        binding.ivSplashLogo.scaleX = 0.5f
        binding.ivSplashLogo.scaleY = 0.5f
        binding.ivSplashLogo.alpha = 0f

        binding.viewGlowAura.scaleX = 0.6f
        binding.viewGlowAura.scaleY = 0.6f
        binding.viewGlowAura.alpha = 0f

        binding.tvSplashTitle.alpha = 0f
        binding.tvSplashTitle.translationY = 40f

        binding.tvSplashTagline.alpha = 0f
        binding.tvSplashTagline.translationY = 30f

        binding.layoutEqualizerBars.alpha = 0f
        binding.tvSplashFooter.alpha = 0f
    }

    private fun startSplashAnimations() {
        // 1. Logo Pop & Glow Aura entrance
        binding.ivSplashLogo.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(750)
            .setInterpolator(OvershootInterpolator(1.3f))
            .start()

        binding.viewGlowAura.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(0.85f)
            .setDuration(850)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                startGlowPulseAnimation()
            }
            .start()

        // 2. Title Fade-in & Slide-up
        binding.tvSplashTitle.animate()
            .alpha(1.0f)
            .translationY(0f)
            .setStartDelay(250)
            .setDuration(600)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 3. Tagline Fade-in & Slide-up
        binding.tvSplashTagline.animate()
            .alpha(1.0f)
            .translationY(0f)
            .setStartDelay(420)
            .setDuration(550)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 4. Equalizer Bars entrance & continuous bounce
        binding.layoutEqualizerBars.animate()
            .alpha(1.0f)
            .setStartDelay(500)
            .setDuration(400)
            .withEndAction {
                startEqualizerBarsAnimation()
            }
            .start()

        // 5. Footer subtle fade-in
        binding.tvSplashFooter.animate()
            .alpha(0.85f)
            .setStartDelay(650)
            .setDuration(500)
            .start()
    }

    private fun startGlowPulseAnimation() {
        if (isDestroyed || isFinishing) return
        val pulseScaleX = ObjectAnimator.ofFloat(binding.viewGlowAura, View.SCALE_X, 1.0f, 1.15f, 1.0f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val pulseScaleY = ObjectAnimator.ofFloat(binding.viewGlowAura, View.SCALE_Y, 1.0f, 1.15f, 1.0f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val pulseAlpha = ObjectAnimator.ofFloat(binding.viewGlowAura, View.ALPHA, 0.85f, 0.45f, 0.85f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        animatorList.add(pulseScaleX)
        animatorList.add(pulseScaleY)
        animatorList.add(pulseAlpha)

        AnimatorSet().apply {
            playTogether(pulseScaleX, pulseScaleY, pulseAlpha)
            start()
        }
    }

    private fun startEqualizerBarsAnimation() {
        if (isDestroyed || isFinishing) return
        val bars = listOf(
            Pair(binding.eqBar1, 180L),
            Pair(binding.eqBar2, 260L),
            Pair(binding.eqBar3, 220L),
            Pair(binding.eqBar4, 290L),
            Pair(binding.eqBar5, 200L)
        )

        bars.forEach { (bar, dur) ->
            val anim = ObjectAnimator.ofFloat(bar, View.SCALE_Y, 0.35f, 1.25f, 0.5f).apply {
                duration = dur
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            animatorList.add(anim)
        }
    }

    private fun proceedToNextScreen() {
        if (isNavigated || isFinishing || isDestroyed) return
        isNavigated = true

        val nextIntent = if (hasMediaPermission()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, PermissionActivity::class.java)
        }
        nextIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(nextIntent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun hasMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        animatorList.forEach { it.cancel() }
        animatorList.clear()
    }

    companion object {
        private const val SPLASH_DURATION_MS = 1600L
    }
}
