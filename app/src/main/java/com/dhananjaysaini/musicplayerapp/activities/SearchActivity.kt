package com.dhananjaysaini.musicplayerapp.activities

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.dhananjaysaini.musicplayerapp.databinding.ActivitySearchBinding
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager

class SearchActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.searchRoot) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.searchRoot.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }

        ThemeManager.applyThemeToActivity(this)
    }
}
