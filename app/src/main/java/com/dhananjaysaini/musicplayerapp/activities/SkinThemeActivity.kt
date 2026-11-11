package com.dhananjaysaini.musicplayerapp.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.dhananjaysaini.musicplayerapp.adapter.ColorThemeAdapter
import com.dhananjaysaini.musicplayerapp.adapter.ImageThemeAdapter
import com.dhananjaysaini.musicplayerapp.databinding.ActivitySkinThemeBinding
import com.dhananjaysaini.musicplayerapp.model.AppTheme
import com.dhananjaysaini.musicplayerapp.model.ThemeType
import com.dhananjaysaini.musicplayerapp.repository.ThemeRepository
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.FileOutputStream

class SkinThemeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkinThemeBinding
    private lateinit var colorAdapter: ColorThemeAdapter
    private lateinit var imageAdapter: ImageThemeAdapter
    private var currentActiveThemeId: String = "color_2"

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sourceUri ->
            try {
                val destFile = File(filesDir, "custom_skin_theme.jpg")
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val savedUri = Uri.fromFile(destFile).toString()
                val intent = Intent(this, ThemePreviewActivity::class.java).apply {
                    putExtra("selected_theme_id", "custom")
                    putExtra("custom_theme_uri", savedUri)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkinThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar & display cutout padding with safe clearance
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val extraTop = (16 * resources.displayMetrics.density).toInt()
            binding.topBar.updatePadding(top = systemBars.top + extraTop)
            insets
        }

        val initialTheme = ThemeManager.getCurrentTheme(this)
        currentActiveThemeId = initialTheme.id
        ThemeManager.applyThemeToActivity(this)
        applySkinThemeUI(initialTheme)

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupColorSection()
        setupCategoryTabs()
        setupImageSection()
    }

    override fun onResume() {
        super.onResume()
        val currentTheme = ThemeManager.getCurrentTheme(this)
        currentActiveThemeId = currentTheme.id
        ThemeManager.applyThemeToActivity(this)
        applySkinThemeUI(currentTheme)
        refreshSelection(currentActiveThemeId)
    }

    private fun applySkinThemeUI(theme: AppTheme) {
        ThemeManager.applyThemeBackground(this, binding.skinThemeRoot, theme)
        val isDark = theme.isDark

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark

        val topColor = if (isDark) Color.WHITE else Color.parseColor("#111827")
        val subColor = if (isDark) Color.parseColor("#99FFFFFF") else Color.parseColor("#6B7280")
        val accentColor = if (isDark) Color.parseColor("#FF9800") else Color.parseColor("#E65100")

        binding.btnBack.imageTintList = ColorStateList.valueOf(topColor)
        binding.tvSkinThemeTitle.setTextColor(topColor)
        binding.tvColorSectionTitle.setTextColor(topColor)

        binding.tabLayoutCategories.setTabTextColors(subColor, topColor)
        binding.tabLayoutCategories.setSelectedTabIndicatorColor(accentColor)
    }

    private fun setupColorSection() {
        val colorThemes = ThemeRepository.getColorThemes()
        colorAdapter = ColorThemeAdapter(colorThemes, currentActiveThemeId) { selectedTheme ->
            val intent = Intent(this, ThemePreviewActivity::class.java).apply {
                putExtra("selected_theme_id", selectedTheme.id)
            }
            startActivity(intent)
        }

        binding.rvColorThemes.layoutManager = GridLayoutManager(this, 5)
        binding.rvColorThemes.adapter = colorAdapter
    }

    private fun setupCategoryTabs() {
        val categories = listOf("All images", "Nature", "Pet", "Cartoon", "Other")
        categories.forEach { title ->
            val tab = binding.tabLayoutCategories.newTab().setText(title)
            binding.tabLayoutCategories.addTab(tab)
        }

        binding.tabLayoutCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val categoryName = tab?.text?.toString() ?: "All images"
                loadCategoryThemes(categoryName)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupImageSection() {
        imageAdapter = ImageThemeAdapter(
            themeList = emptyList(),
            selectedThemeId = currentActiveThemeId,
            onThemeSelected = { selectedTheme ->
                val intent = Intent(this, ThemePreviewActivity::class.java).apply {
                    putExtra("selected_theme_id", selectedTheme.id)
                }
                startActivity(intent)
            },
            onCustomSelected = {
                imagePickerLauncher.launch("image/*")
            }
        )

        binding.rvImageThemes.layoutManager = GridLayoutManager(this, 3)
        binding.rvImageThemes.adapter = imageAdapter

        loadCategoryThemes("All images")
    }

    private fun loadCategoryThemes(category: String) {
        val currentTheme = ThemeManager.getCurrentTheme(this)
        val customUri = if (currentTheme.type == ThemeType.CUSTOM) currentTheme.customImageUri else null

        val list = if (category == "All images") {
            // First item is Custom card
            val customCard = AppTheme(
                id = "custom",
                name = "Custom",
                type = ThemeType.CUSTOM,
                category = "Custom",
                customImageUri = customUri,
                isDark = true
            )
            listOf(customCard) + ThemeRepository.getAllImageThemes()
        } else {
            ThemeRepository.getThemesByCategory(category)
        }

        imageAdapter.updateList(list, currentActiveThemeId)
    }

    private fun refreshSelection(selectedId: String) {
        currentActiveThemeId = selectedId
        colorAdapter.setSelectedId(selectedId)
        val selectedTabPosition = binding.tabLayoutCategories.selectedTabPosition
        val category = binding.tabLayoutCategories.getTabAt(selectedTabPosition)?.text?.toString() ?: "All images"
        loadCategoryThemes(category)
    }
}
