package com.dhananjaysaini.musicplayerapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.AppTheme
import com.dhananjaysaini.musicplayerapp.model.ThemeType
import com.dhananjaysaini.musicplayerapp.repository.ThemeRepository
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

object ThemeManager {

    const val ACTION_THEME_CHANGED = "com.dhananjaysaini.musicplayerapp.ACTION_THEME_CHANGED"
    private const val PREF_NAME = "app_theme_v2"
    private const val KEY_THEME_ID = "selected_theme_id"
    private const val KEY_IS_CUSTOM = "is_custom_theme"
    private const val KEY_CUSTOM_URI = "custom_theme_uri"

    fun getCurrentTheme(context: Context): AppTheme {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isCustom = prefs.getBoolean(KEY_IS_CUSTOM, false)
        if (isCustom) {
            val uri = prefs.getString(KEY_CUSTOM_URI, null)
            if (uri != null) {
                return AppTheme(
                    id = "custom",
                    name = "Custom Wallpaper",
                    type = ThemeType.CUSTOM,
                    category = "Custom",
                    customImageUri = uri,
                    isDark = true
                )
            }
        }
        val themeId = prefs.getString(KEY_THEME_ID, "color_2") ?: "color_2"
        return ThemeRepository.findThemeById(themeId) ?: ThemeRepository.getColorThemes()[1]
    }

    fun isDarkTheme(context: Context): Boolean {
        val currentTheme = getCurrentTheme(context)
        return currentTheme.isDark
    }

    fun saveTheme(context: Context, theme: AppTheme) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (theme.type == ThemeType.CUSTOM && theme.customImageUri != null) {
                putBoolean(KEY_IS_CUSTOM, true)
                putString(KEY_CUSTOM_URI, theme.customImageUri)
                putString(KEY_THEME_ID, "custom")
            } else {
                putBoolean(KEY_IS_CUSTOM, false)
                putString(KEY_THEME_ID, theme.id)
            }
            apply()
        }

        // Keep dark_mode in app_settings always in sync
        val settingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        settingsPrefs.edit().putBoolean("dark_mode", theme.isDark).apply()

        // Synchronize AppCompat night mode
        val targetMode = if (theme.isDark) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        } else {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        }
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != targetMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode)
        }

        context.sendBroadcast(Intent(ACTION_THEME_CHANGED).setPackage(context.packageName))
    }

    fun applyThemeToActivity(activity: Activity) {
        val theme = getCurrentTheme(activity)
        val isDark = isDarkTheme(activity)

        val targetMode = if (isDark) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        } else {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        }
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != targetMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode)
        }

        // 1. Edge-to-edge + Transparent Status & Navigation Bars with dynamic icon tint
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark

        // 2. Window Decor Background & Root Containers
        applyThemeBackground(activity, activity.window.decorView, theme)
        activity.findViewById<View>(android.R.id.content)?.let { contentView ->
            applyThemeBackground(activity, contentView, theme)
        }
        activity.findViewById<View>(R.id.drawer_btn)?.let { drawer ->
            applyThemeBackground(activity, drawer, theme)
            if (drawer is DrawerLayout) {
                drawer.setStatusBarBackgroundColor(Color.TRANSPARENT)
                drawer.setStatusBarBackground(null)
            }
        }

        // 3. Navigation Drawer (if present)
        activity.findViewById<View>(R.id.customDrawer)?.let { drawer ->
            applyThemeBackground(activity, drawer, theme)
            applyThemeToDrawer(drawer, isDark)
        }

        // 4. Floating Mini Player (if present)
        activity.findViewById<MaterialCardView>(R.id.mini_player_root)?.let { miniRoot ->
            applyThemeToMiniPlayerView(miniRoot, theme)
        }

        // 5. AppBarLayout & Toolbar (if present) - make 100% transparent so theme flows from top status bar
        activity.findViewById<View>(R.id.appBarLayout)?.let { appBar ->
            appBar.setBackgroundColor(Color.TRANSPARENT)
            if (appBar is AppBarLayout) {
                appBar.outlineProvider = null
                appBar.elevation = 0f
                appBar.setLiftOnScroll(false)
            }
        }
        activity.findViewById<Toolbar>(R.id.toolbar)?.let { toolbar ->
            toolbar.setBackgroundColor(Color.TRANSPARENT)
        }

        // 6. Bottom Navigation (if present) - semi-transparent glassmorphic look
        activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.let { bottomNav ->
            val navColor = if (isDark) Color.parseColor("#D90E0818") else Color.parseColor("#F5FFFFFF")
            bottomNav.setBackgroundColor(navColor)
        }
    }

    private fun applyThemeToDrawer(drawer: View, isDark: Boolean) {
        val textColor = if (isDark) Color.WHITE else Color.parseColor("#111827")
        val tintList = ColorStateList.valueOf(textColor)

        drawer.findViewById<TextView>(R.id.tvDrawerHeaderTitle)?.setTextColor(textColor)

        val itemIds = intArrayOf(
            R.id.navItemLibrary,
            R.id.navItemSettings,
            R.id.navItemEqualizer,
            R.id.navItemSleepTimer,
            R.id.navItemSkinTheme,
            R.id.navItemWidgets,
            R.id.navItemRemoveAds,
            R.id.navItemVolumeBooster,
            R.id.navItemVoiceRecorder,
            R.id.navItemFreeRingtones
        )

        for (id in itemIds) {
            val item = drawer.findViewById<ViewGroup>(id) ?: continue
            for (i in 0 until item.childCount) {
                val child = item.getChildAt(i)
                if (child is TextView) {
                    child.setTextColor(textColor)
                }
            }
        }
    }

    fun applyThemeToMiniPlayerView(root: View, theme: AppTheme) {
        val isDark = theme.isDark
        val bgView = root.findViewById<ImageView>(R.id.mini_theme_bg)
        val scrimView = root.findViewById<View>(R.id.mini_theme_scrim)
        val cardView = root as? MaterialCardView

        if (cardView != null) {
            cardView.setCardBackgroundColor(Color.TRANSPARENT)
            cardView.strokeWidth = (1 * root.resources.displayMetrics.density).toInt()
            cardView.strokeColor = if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#E5E7EB")
        }

        if (bgView != null) {
            when (theme.type) {
                ThemeType.CUSTOM -> {
                    if (theme.customImageUri != null) {
                        Glide.with(root.context)
                            .load(Uri.parse(theme.customImageUri))
                            .centerCrop()
                            .into(bgView)
                    } else {
                        val fallback = if (isDark) R.drawable.gradient_theme_2 else R.drawable.gradient_theme_6
                        bgView.setImageResource(fallback)
                    }
                }
                ThemeType.GRADIENT, ThemeType.IMAGE -> {
                    if (theme.drawableRes != 0) {
                        bgView.setImageResource(theme.drawableRes)
                    } else {
                        val fallback = if (isDark) R.drawable.gradient_theme_2 else R.drawable.gradient_theme_6
                        bgView.setImageResource(fallback)
                    }
                }
            }
        }

        if (scrimView != null) {
            val scrimColor = if (isDark) Color.parseColor("#8C080612") else Color.parseColor("#E6FFFFFF")
            scrimView.setBackgroundColor(scrimColor)
        }

        val textColor = if (isDark) Color.WHITE else Color.parseColor("#111827")
        val subColor = if (isDark) Color.parseColor("#CCFFFFFF") else Color.parseColor("#4B5563")

        root.findViewById<TextView>(R.id.mini_title)?.setTextColor(textColor)
        root.findViewById<TextView>(R.id.mini_artist)?.setTextColor(subColor)

        val tintList = ColorStateList.valueOf(textColor)
        root.findViewById<ImageView>(R.id.mini_prev)?.imageTintList = tintList
        root.findViewById<ImageView>(R.id.mini_next)?.imageTintList = tintList
        root.findViewById<ImageView>(R.id.mini_playPause)?.imageTintList = ColorStateList.valueOf(
            if (isDark) Color.WHITE else Color.parseColor("#E65100")
        )
    }

    fun applyThemeBackground(context: Context, targetView: View, theme: AppTheme) {
        val isDark = theme.isDark

        when (theme.type) {
            ThemeType.CUSTOM -> {
                if (theme.customImageUri != null) {
                    val uri = Uri.parse(theme.customImageUri)
                    Glide.with(context)
                        .load(uri)
                        .centerCrop()
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                val scrimColor = if (isDark) Color.parseColor("#4D000000") else Color.parseColor("#33FFFFFF")
                                val scrim = ColorDrawable(scrimColor)
                                val layered = LayerDrawable(arrayOf(resource, scrim))
                                targetView.background = layered
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
                } else {
                    val fallback = if (isDark) R.drawable.gradient_theme_2 else R.drawable.gradient_theme_6
                    targetView.setBackgroundResource(fallback)
                }
            }
            ThemeType.GRADIENT, ThemeType.IMAGE -> {
                if (theme.drawableRes != 0) {
                    targetView.setBackgroundResource(theme.drawableRes)
                } else {
                    val fallback = if (isDark) R.drawable.gradient_theme_2 else R.drawable.gradient_theme_6
                    targetView.setBackgroundResource(fallback)
                }
            }
        }
    }
}
