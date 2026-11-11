package com.dhananjaysaini.musicplayerapp.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.dhananjaysaini.musicplayerapp.adapter.ThemePreviewAdapter
import com.dhananjaysaini.musicplayerapp.databinding.ActivityThemePreviewBinding
import com.dhananjaysaini.musicplayerapp.model.AppTheme
import com.dhananjaysaini.musicplayerapp.model.ThemeType
import com.dhananjaysaini.musicplayerapp.repository.ThemeRepository
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import kotlin.math.abs

class ThemePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemePreviewBinding
    private lateinit var previewAdapter: ThemePreviewAdapter
    private val themeList = ArrayList<AppTheme>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Safe status bar and navigation bar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.previewTopBar.updatePadding(top = systemBars.top)
            binding.bottomContainer.updatePadding(bottom = systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupThemesData()
        setupViewPager()
        setupApplyButton()
    }

    private fun setupThemesData() {
        val selectedThemeId = intent.getStringExtra("selected_theme_id") ?: "color_2"
        val customUri = intent.getStringExtra("custom_theme_uri")

        themeList.clear()

        // If custom theme was passed or active, add it
        if (customUri != null || selectedThemeId == "custom") {
            themeList.add(
                AppTheme(
                    id = "custom",
                    name = "Custom",
                    type = ThemeType.CUSTOM,
                    category = "Custom",
                    customImageUri = customUri ?: ThemeManager.getCurrentTheme(this).customImageUri,
                    isDark = true
                )
            )
        }

        // Add 21 Color themes
        themeList.addAll(ThemeRepository.getColorThemes())

        // Add 48 Category Image themes
        themeList.addAll(ThemeRepository.getAllImageThemes())
    }

    private fun setupViewPager() {
        previewAdapter = ThemePreviewAdapter(themeList)
        binding.vpThemePreview.adapter = previewAdapter

        // Carousel transformation: scaling + margins for peek effect
        val marginPx = (16 * resources.displayMetrics.density).toInt()
        val transformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(marginPx))
            addTransformer { page, position ->
                val r = 1 - abs(position)
                page.scaleY = 0.88f + r * 0.12f
                page.alpha = 0.55f + r * 0.45f
            }
        }

        binding.vpThemePreview.setPageTransformer(transformer)
        binding.vpThemePreview.offscreenPageLimit = 3

        val targetThemeId = intent.getStringExtra("selected_theme_id") ?: "color_2"
        val targetIndex = themeList.indexOfFirst { it.id == targetThemeId }.coerceAtLeast(0)
        binding.vpThemePreview.setCurrentItem(targetIndex, false)

        // Update overall activity background when user scrolls through themes
        binding.vpThemePreview.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentTheme = themeList.getOrNull(position) ?: return
                ThemeManager.applyThemeBackground(this@ThemePreviewActivity, binding.previewRoot, currentTheme)
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightStatusBars = !currentTheme.isDark
                controller.isAppearanceLightNavigationBars = !currentTheme.isDark
            }
        })

        themeList.getOrNull(targetIndex)?.let {
            ThemeManager.applyThemeBackground(this, binding.previewRoot, it)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = !it.isDark
            controller.isAppearanceLightNavigationBars = !it.isDark
        }
    }

    private fun setupApplyButton() {
        binding.btnApplyTheme.setOnClickListener {
            val currentPos = binding.vpThemePreview.currentItem
            val themeToApply = previewAdapter.getThemeAt(currentPos) ?: return@setOnClickListener

            // 1. Save and apply theme globally (handles dark_mode and AppCompatDelegate)
            ThemeManager.saveTheme(this, themeToApply)

            Toast.makeText(this, "Theme applied successfully!", Toast.LENGTH_SHORT).show()

            // 2. Return to MainActivity keeping whatever fragment was already open
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }
}
