package com.dhananjaysaini.musicplayerapp.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.fragments.PlaylistSongsFragment
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager

class PlaylistSongsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_songs)

        val rootView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        val isDark = ThemeManager.isDarkTheme(this)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark

        if (savedInstanceState == null) {

            val fragment = PlaylistSongsFragment().apply {
                arguments = intent.extras
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.playlistSongsContainer, fragment) 
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(this)
    }
}
