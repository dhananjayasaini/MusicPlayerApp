package com.dhananjaysaini.musicplayerapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.ViewPagerAdapter
import com.dhananjaysaini.musicplayerapp.databinding.ActivityMainBinding
import com.dhananjaysaini.musicplayerapp.fragments.MiniPlayerFragment
import com.dhananjaysaini.musicplayerapp.widget.MusicAppWidgetProvider
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.model.FavouriteEntity
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.DeleteManager
import com.dhananjaysaini.musicplayerapp.utils.EqualizerManager
import com.dhananjaysaini.musicplayerapp.utils.FavouriteManager
import com.dhananjaysaini.musicplayerapp.utils.PlaylistManager
import com.dhananjaysaini.musicplayerapp.utils.SleepTimerBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.VolumeBoosterBottomSheet
import com.dhananjaysaini.musicplayerapp.activities.VoiceRecorderActivity
import com.dhananjaysaini.musicplayerapp.activities.FreeRingtonesActivity
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.utils.ThemeSelectionBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.VoiceControlManager
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var toolbar: Toolbar
    private lateinit var voiceControl: VoiceControlManager
    private val mainViewModel: MainViewModel by viewModels()

    companion object {
        var musicListMA = ArrayList<Music>()

        private const val STORAGE_PERMISSION_CODE = 500
        private const val NOTIFICATION_PERMISSION_CODE = 501
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        applySavedTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.appBarLayout.updatePadding(top = systemBars.top)
            binding.bottomNavigation.updatePadding(bottom = systemBars.bottom)
            insets
        }

        toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        ThemeManager.applyThemeToActivity(this)

        FavouriteManager.init(applicationContext)
        PlaylistManager.init(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            val dao = MusicDatabase.getDatabase(applicationContext).FavouriteDao()
            val roomFavIds = dao.getAllFavourites().map { it.songId }.toSet()
            for (favSong in FavouriteManager.getFavourites()) {
                if (!roomFavIds.contains(favSong.id)) {
                    dao.addFavourite(FavouriteEntity(favSong.id))
                }
            }
        }

        setupNavigation()
        setupDrawerMenu()
        setupMiniPlayer()
        observeViewModel()
        requestPermission()
//        voiceControllerMa()
        mainSongsVM()

    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDark = sharedPref.getBoolean("dark_mode", false)

        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

    }

    // -------------------- Navigation & ViewPager --------------------

    private fun setupNavigation() {
        binding.viewPager.adapter = ViewPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.isUserInputEnabled = false

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.viewPager.setCurrentItem(0, false)
                    true
                }
                R.id.nav_favourite -> {
                    binding.viewPager.setCurrentItem(1, false)
                    true
                }
                R.id.nav_search -> {
                    binding.viewPager.setCurrentItem(2, false)
                    true
                }
                R.id.nav_settings -> {
                    binding.viewPager.setCurrentItem(3, false)
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    // -------------------- ViewModel --------------------

    private fun observeViewModel() {
        mainViewModel.error.observe(this) {

            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    // -------------------- Mini Player --------------------

    private fun setupMiniPlayer() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mini_player_container, MiniPlayerFragment())
            .commit()

        val filter = IntentFilter().apply {
            addAction("SHOW_MINI_PLAYER")
            addAction("UPDATE_UI")
        }
        ContextCompat.registerReceiver(
            this,
            showMiniReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val themeFilter = IntentFilter(ThemeManager.ACTION_THEME_CHANGED)
        ContextCompat.registerReceiver(
            this,
            themeReceiver,
            themeFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private val showMiniReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.miniPlayerContainer.visibility = View.VISIBLE
        }
    }

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            ThemeManager.applyThemeToActivity(this@MainActivity)
            updateToolbarColors()
        }
    }

    // -------------------- Drawer --------------------

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_theme -> {
                themeClick()
                return true
            }
            R.id.action_search -> {
                binding.bottomNavigation.selectedItemId = R.id.nav_search
                return true
            }
            R.id.action_settings -> {
                binding.bottomNavigation.selectedItemId = R.id.nav_settings
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupDrawerMenu() {
        val drawer = binding.drawerBtn
        toggle = ActionBarDrawerToggle(this, drawer, binding.toolbar, R.string.open, R.string.close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        updateToolbarColors()

        binding.btnViewRecap.setOnClickListener {
            toast("Music Recap coming soon")
        }

        val customDrawer = binding.customDrawer

        // Top Music Recap Card
        customDrawer.cardMusicRecap.setOnClickListener {
            drawer.closeDrawers()
            toast("Music Recap coming soon")
        }

        // 1. Library -> switch to Home / Library tab
        customDrawer.navItemLibrary.setOnClickListener {
            drawer.closeDrawers()
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }

        // 2. Settings -> switch to Settings tab
        customDrawer.navItemSettings.setOnClickListener {
            drawer.closeDrawers()
            binding.bottomNavigation.selectedItemId = R.id.nav_settings
        }

        // 3. Equalizer -> Open Equalizer
        customDrawer.navItemEqualizer.setOnClickListener {
            drawer.closeDrawers()
            val sessionId = MusicService.mediaPlayer?.audioSessionId ?: -1
            if (sessionId != -1) {
                EqualizerManager(this).showEqualizer(sessionId)
            } else {
                toast("Play a song first to open the Equalizer!")
            }
        }

        // 4. Sleep timer -> Open SleepTimerBottomSheet
        customDrawer.navItemSleepTimer.setOnClickListener {
            drawer.closeDrawers()
            SleepTimerBottomSheet().show(supportFragmentManager, "SleepTimerBottomSheet")
        }

        // 5. Skin theme -> Open SkinThemeActivity
        customDrawer.navItemSkinTheme.setOnClickListener {
            drawer.closeDrawers()
            startActivity(Intent(this, SkinThemeActivity::class.java))
        }

        // 6. Widgets
        customDrawer.navItemWidgets.setOnClickListener {
            drawer.closeDrawers()
            pinMusicWidget()
        }

        // 7. Remove ads
        customDrawer.navItemRemoveAds.setOnClickListener {
            drawer.closeDrawers()
            toast("Remove ads coming soon")
        }

        // 8. Volume booster [AD]
        customDrawer.navItemVolumeBooster.setOnClickListener {
            drawer.closeDrawers()
            VolumeBoosterBottomSheet(this).show()
        }

        // 9. Voice recorder [AD]
        customDrawer.navItemVoiceRecorder.setOnClickListener {
            drawer.closeDrawers()
            startActivity(Intent(this, VoiceRecorderActivity::class.java))
        }

        // 10. Free ringtones [AD]
        customDrawer.navItemFreeRingtones.setOnClickListener {
            drawer.closeDrawers()
            startActivity(Intent(this, FreeRingtonesActivity::class.java))
        }
    }

//    companion object {
//        private const val STORAGE_PERMISSION_CODE = 500
//        private const val NOTIFICATION_PERMISSION_CODE = 501
//    }

    private fun requestPermission() {
        if (hasStoragePermission()) {
            mainViewModel.loadMusic(this)
            requestNotificationPermission()
        } else {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION_CODE)
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun hasPermission(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mainViewModel.loadMusic(this)
                    requestNotificationPermission()
                }
            }
            NOTIFICATION_PERMISSION_CODE -> {
                // Notifications allowed or denied; playback works regardless
            }
        }
    }

    private fun checkAudioPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    // -------------------- Voice --------------------

    private fun voiceControllerMa() {
        voiceControl = VoiceControlManager(this)
//        binding.voiceControlMa.btnMic.setOnClickListener {
//            voiceControl.startListening()
//        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun mainSongsVM() {

        mainViewModel.musicListLiveData.observe(this) { songs ->
            MusicService.allSongs = songs
        }


    }

    private fun sendFeedback(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // only email apps will open
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@musicplayer.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Feedback - Music Player App")
            putExtra(
                Intent.EXTRA_TEXT,
                "Write your feedback here...\n\n" +
                        "---- Device Info ----\n" +
                        "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                        "Android Version: ${Build.VERSION.RELEASE}\n" +
                        "App Version: ${getAppVersion(context)}\n"
            )
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Send Feedback"))
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val versionInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        updateToolbarColors()
        return true
    }

    private fun updateToolbarColors() {
        val isDark = ThemeManager.isDarkTheme(this)
        val iconColor = if (isDark) Color.WHITE else Color.parseColor("#111827")

        if (::toggle.isInitialized) {
            toggle.drawerArrowDrawable.color = iconColor
        }

        binding.toolbar.menu?.let { menu ->
            menu.findItem(R.id.action_theme)?.icon?.setTintList(null)
            menu.findItem(R.id.action_search)?.icon?.setTintList(null)
            menu.findItem(R.id.action_settings)?.icon?.setTintList(null)
        }
    }

    private fun toggleTheme() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDark = sharedPref.getBoolean("dark_mode", false)

        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            //    sharedPref.edit().putBoolean("dark_mode", false).apply()

        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            //      sharedPref.edit().putBoolean("dark_mode", true).apply()

        }

        //   sharedPref.edit().putBoolean("dark_mode", !isDark).apply()
        recreate()
    }

    private fun themeClick() {
        startActivity(Intent(this, SkinThemeActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(this)
        updateToolbarColors()
        if (MusicService.mediaPlayer != null || MusicService.playlist.isNotEmpty()) {
            binding.miniPlayerContainer.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(showMiniReceiver)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(themeReceiver)
        } catch (_: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DeleteManager.DELETE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Song deleted", Toast.LENGTH_SHORT).show()
            mainViewModel.loadMusic(this, true)
        }
    }

    private fun pinMusicWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val myProvider = ComponentName(this, MusicAppWidgetProvider::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
            val pinnedWidgetCallbackIntent = Intent(this, MainActivity::class.java)
            val successCallback = PendingIntent.getActivity(
                this, 0, pinnedWidgetCallbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
        } else {
            showWidgetGuideDialog()
        }
    }

    private fun showWidgetGuideDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.widget_pin_dialog_title)
            .setMessage(R.string.widget_pin_dialog_msg)
            .setPositiveButton("Got it", null)
            .show()
    }

}
