package com.dhananjaysaini.musicplayerapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import androidx.core.view.WindowInsetsControllerCompat
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.ViewPagerAdapter
import com.dhananjaysaini.musicplayerapp.databinding.ActivityMainBinding
import com.dhananjaysaini.musicplayerapp.fragments.MiniPlayerFragment
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.model.FavouriteEntity
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.DeleteManager
import com.dhananjaysaini.musicplayerapp.utils.FavouriteManager
import com.dhananjaysaini.musicplayerapp.utils.PlaylistManager
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.utils.ThemeSelectionBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.VoiceControlManager
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
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

//        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true  // dark icons

        toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
//      supportActionBar?.title = ""
        toolbar.title = ""
//      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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

        setupViewPager()
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

    // -------------------- ViewPager --------------------

    /*
        private fun setupViewPager() {
            binding.viewPager.adapter = ViewPagerAdapter(this)
            binding.viewPager.offscreenPageLimit = 7

            binding.viewPager.setCurrentItem(1, false)

            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> "Home"
                    1 -> "Songs"
                    2 -> "Favourite"
                    3 -> "Playlists"
                    4 -> "Artist"
                    5 -> "Albums"
                    6 -> "Folder"
                    else -> "Songs"
                }
            }.attach()
        }
    */

    private fun setupViewPager() {

        binding.viewPager.adapter = ViewPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 2
        val defaultPosition = 1
        binding.viewPager.setCurrentItem(defaultPosition, false)
        binding.tabLayout.getTabAt(defaultPosition)?.select()

        //  binding.viewPager.setCurrentItem(1, false)

        val titles = listOf("Home", "Songs", "Favourite", "Playlists", "Artist", "Albums", "Folder")

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->

//            tab.text = when (position) {
//                0 -> "Home"
//                1 -> "Songs"
//                2 -> "Favourite"
//                3 -> "Playlists"
//                4 -> "Artist"
//                5 -> "Albums"
//                6 -> "Folder"
//                else -> "Songs"
//            }

            val view = layoutInflater.inflate(R.layout.item_viewpager_tab, null)
            val text = view.findViewById<TextView>(R.id.tabText)
            text.text = titles[position]

            tab.customView = view

        }.attach()

        // 🔥 Handle selection style
        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                styleTab(tab, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                styleTab(tab, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 🔥 Manually trigger default selected style
        binding.tabLayout.post {
            val tab = binding.tabLayout.getTabAt(defaultPosition)
            if (tab != null) {
                styleTab(tab, true)
            }
        }
    }

    private fun styleTab(tab: TabLayout.Tab, isSelected: Boolean) {

        val text = tab.customView?.findViewById<TextView>(R.id.tabText)

        text?.apply {
            if (isSelected) {
                textSize = 20f
                setTypeface(null, Typeface.BOLD)

                animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(200)
                    .start()

            } else {
                textSize = 16f
                setTypeface(null, Typeface.NORMAL)

                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
        }
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

        ContextCompat.registerReceiver(
            this,
            showMiniReceiver,
            IntentFilter("SHOW_MINI_PLAYER"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private val showMiniReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.miniPlayerContainer.visibility = View.VISIBLE
        }
    }

    // -------------------- Drawer --------------------

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_theme -> {
                //   toggleTheme()
                themeClick()
                return true
            }
        }
        return super.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

    private fun setupDrawerMenu() {
        val drawer = binding.drawerBtn
        toggle = ActionBarDrawerToggle(this, drawer, binding.toolbar, R.string.open, R.string.close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()


        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navFeedback -> sendFeedback(this)
                R.id.navSetting -> toast("Setting")
                R.id.navAbout -> toast("About")
                R.id.navExit -> exitProcess(1)
            }
            true
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
        return true
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

        ThemeManager.saveTheme(this, 0)

        val themeSheet = ThemeSelectionBottomSheet()
        themeSheet.show(supportFragmentManager, "ThemeSheet")

    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(this)
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

}
