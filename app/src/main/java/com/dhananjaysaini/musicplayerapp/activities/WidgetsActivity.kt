package com.dhananjaysaini.musicplayerapp.activities

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.WidgetsAdapter
import com.dhananjaysaini.musicplayerapp.databinding.ActivityWidgetsBinding
import com.dhananjaysaini.musicplayerapp.model.WidgetStyleItem
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.widget.*

class WidgetsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetsBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge transparent status & navigation bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivityWidgetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Physically reserve status bar space with statusBarSpacer
        val sbHeight = getStatusBarHeight()
        val initialHeight = if (sbHeight > 0) sbHeight else (28 * resources.displayMetrics.density).toInt()
        binding.statusBarSpacer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            initialHeight
        )

        // Dynamic system insets listener with display cutout support
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val topInset = maxOf(systemBars.top, sbHeight)
            if (binding.statusBarSpacer.layoutParams.height != topInset) {
                binding.statusBarSpacer.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    topInset
                )
            }
            binding.rvWidgets.updatePadding(
                bottom = systemBars.bottom + (20 * resources.displayMetrics.density).toInt()
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        binding.btnWidgetsBack.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        applyThemeUI()
        setupWidgetsList()
    }

    override fun onResume() {
        super.onResume()
        applyThemeUI()
        binding.rvWidgets.adapter?.notifyDataSetChanged()
    }

    private fun applyThemeUI() {
        val currentTheme = ThemeManager.getCurrentTheme(this)
        ThemeManager.applyThemeBackground(this, binding.widgetsRoot, currentTheme)

        val isDark = currentTheme.isDark
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark

        val textColor = if (isDark) Color.WHITE else Color.parseColor("#111827")
        val subTextColor = if (isDark) Color.parseColor("#E0FFFFFF") else Color.parseColor("#4B5563")

        binding.tvWidgetsTitle.setTextColor(textColor)
        binding.btnWidgetsBack.imageTintList = ColorStateList.valueOf(textColor)
        binding.tvBannerText.setTextColor(subTextColor)
        binding.ivBannerIcon.imageTintList = ColorStateList.valueOf(textColor)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun setupWidgetsList() {
        val widgetStyles = listOf(
            WidgetStyleItem(
                id = "mini",
                nameRes = R.string.widget_name_mini,
                sizeRes = R.string.widget_size_2x2,
                previewLayoutRes = R.layout.widget_preview_mini,
                widgetLayoutRes = R.layout.widget_style_mini,
                providerClass = MiniWidgetProvider::class.java
            ),
            WidgetStyleItem(
                id = "standard",
                nameRes = R.string.widget_name_standard,
                sizeRes = R.string.widget_size_4x2,
                previewLayoutRes = R.layout.widget_preview_standard,
                widgetLayoutRes = R.layout.widget_style_standard,
                providerClass = StandardWidgetProvider::class.java
            ),
            WidgetStyleItem(
                id = "card",
                nameRes = R.string.widget_name_card,
                sizeRes = R.string.widget_size_3x2,
                previewLayoutRes = R.layout.widget_preview_card,
                widgetLayoutRes = R.layout.widget_style_card,
                providerClass = CardWidgetProvider::class.java
            ),
            WidgetStyleItem(
                id = "practical",
                nameRes = R.string.widget_name_practical,
                sizeRes = R.string.widget_size_4x4,
                previewLayoutRes = R.layout.widget_preview_practical,
                widgetLayoutRes = R.layout.widget_style_practical,
                providerClass = PracticalWidgetProvider::class.java
            ),
            WidgetStyleItem(
                id = "stylish",
                nameRes = R.string.widget_name_stylish,
                sizeRes = R.string.widget_size_4x4,
                previewLayoutRes = R.layout.widget_preview_stylish,
                widgetLayoutRes = R.layout.widget_style_stylish,
                providerClass = StylishWidgetProvider::class.java
            ),
            WidgetStyleItem(
                id = "classic",
                nameRes = R.string.widget_name_classic,
                sizeRes = R.string.widget_size_4x1,
                previewLayoutRes = R.layout.widget_preview_classic,
                widgetLayoutRes = R.layout.widget_style_classic,
                providerClass = ClassicWidgetProvider::class.java
            ),
            WidgetStyleItem(
                id = "lite",
                nameRes = R.string.widget_name_lite,
                sizeRes = R.string.widget_size_4x1,
                previewLayoutRes = R.layout.widget_preview_lite,
                widgetLayoutRes = R.layout.widget_style_lite,
                providerClass = LiteWidgetProvider::class.java
            ),
            WidgetStyleItem(
                id = "vinyl",
                nameRes = R.string.widget_name_vinyl,
                sizeRes = R.string.widget_size_4x2,
                previewLayoutRes = R.layout.widget_preview_vinyl,
                widgetLayoutRes = R.layout.widget_style_vinyl,
                providerClass = VinylWidgetProvider::class.java
            ),
            WidgetStyleItem(
                id = "simple",
                nameRes = R.string.widget_name_simple,
                sizeRes = R.string.widget_size_2x1,
                previewLayoutRes = R.layout.widget_preview_simple,
                widgetLayoutRes = R.layout.widget_style_simple,
                providerClass = SimpleWidgetProvider::class.java
            ),
            WidgetStyleItem(
                id = "circular",
                nameRes = R.string.widget_name_circular,
                sizeRes = R.string.widget_size_4x2,
                previewLayoutRes = R.layout.widget_preview_circular,
                widgetLayoutRes = R.layout.widget_style_circular,
                providerClass = CircularWidgetProvider::class.java
            )
        )

        // Directly pin widget to home screen without any intermediate bottom sheet
        val adapter = WidgetsAdapter(widgetStyles) { selectedStyle ->
            pinWidget(selectedStyle)
        }

        binding.rvWidgets.layoutManager = LinearLayoutManager(this)
        binding.rvWidgets.adapter = adapter
    }

    private fun pinWidget(styleItem: WidgetStyleItem) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
            val myProvider = ComponentName(this, styleItem.providerClass)

            // Pass exact RemoteViews preview so the launcher sheet displays the widget UI
            val previewViews = MusicAppWidgetProvider.buildRemoteViews(
                context = this,
                layoutRes = styleItem.widgetLayoutRes,
                forPreview = true
            )
            val extras = Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
            }

            val callbackIntent = Intent(this, MainActivity::class.java)
            val successCallback = PendingIntent.getActivity(
                this,
                styleItem.id.hashCode(),
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            appWidgetManager.requestPinAppWidget(myProvider, extras, successCallback)
        } else {
            Toast.makeText(this, getString(R.string.widget_pin_dialog_msg), Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
