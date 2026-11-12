package com.dhananjaysaini.musicplayerapp.activities

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.RingtoneAdapter
import com.dhananjaysaini.musicplayerapp.adapter.RingtoneModel
import com.dhananjaysaini.musicplayerapp.databinding.ActivityFreeRingtonesBinding
import com.dhananjaysaini.musicplayerapp.databinding.LayoutSetRingtoneDialogBinding
import com.dhananjaysaini.musicplayerapp.repository.MusicRepository
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.MelodicToneGenerator
import com.dhananjaysaini.musicplayerapp.utils.RingtoneHelper
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FreeRingtonesActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private lateinit var binding: ActivityFreeRingtonesBinding

    private val curatedRingtones = mutableListOf<RingtoneModel>()
    private val deviceSongs = mutableListOf<RingtoneModel>()
    private val displayedList = mutableListOf<RingtoneModel>()

    private lateinit var adapter: RingtoneAdapter
    private var isCuratedTabSelected = true

    private var previewMediaPlayer: MediaPlayer? = null
    private var currentPlayingItem: RingtoneModel? = null
    private var currentPlayingPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivityFreeRingtonesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.ringtonesRoot) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.ringtonesTopBar.updatePadding(top = systemBars.top)
            binding.rvRingtonesList.updatePadding(bottom = systemBars.bottom + 80)
            insets
        }

        ThemeManager.applyThemeToActivity(this)

        setupUI()
        setupRecyclerView()
        loadData()
    }

    private fun setupUI() {
        binding.btnRingtonesBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Toggle search bar
        binding.btnToggleSearch.setOnClickListener {
            if (binding.layoutSearchBar.visibility == View.VISIBLE) {
                binding.layoutSearchBar.visibility = View.GONE
                binding.etSearchRingtone.setText("")
            } else {
                binding.layoutSearchBar.visibility = View.VISIBLE
                binding.etSearchRingtone.requestFocus()
            }
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchRingtone.setText("")
        }

        binding.etSearchRingtone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Tabs
        binding.tabCuratedRingtones.setOnClickListener {
            if (!isCuratedTabSelected) {
                isCuratedTabSelected = true
                updateTabStyles()
                stopPreview()
                filterList(binding.etSearchRingtone.text.toString())
            }
        }

        binding.tabDeviceSongs.setOnClickListener {
            if (isCuratedTabSelected) {
                isCuratedTabSelected = false
                updateTabStyles()
                stopPreview()
                filterList(binding.etSearchRingtone.text.toString())
            }
        }

        // Bottom preview controls
        binding.btnBottomPlayPause.setOnClickListener {
            val player = previewMediaPlayer
            if (player != null) {
                if (player.isPlaying) {
                    player.pause()
                    binding.btnBottomPlayPause.setImageResource(R.drawable.ic_widget_play)
                    adapter.setPlaybackState(currentPlayingPosition, false)
                } else {
                    player.start()
                    binding.btnBottomPlayPause.setImageResource(R.drawable.ic_widget_pause)
                    adapter.setPlaybackState(currentPlayingPosition, true)
                }
            }
        }

        binding.btnBottomSetRingtone.setOnClickListener {
            currentPlayingItem?.let { showSetRingtoneDialog(it) }
        }
    }

    private fun updateTabStyles() {
        if (isCuratedTabSelected) {
            binding.tabCuratedRingtones.setBackgroundResource(R.drawable.bg_btn_apply_theme)
            binding.tabCuratedRingtones.setTextColor(Color.WHITE)
            binding.tabDeviceSongs.setBackgroundResource(R.drawable.bg_booster_card)
            binding.tabDeviceSongs.setTextColor(Color.parseColor("#A08EBC"))
        } else {
            binding.tabDeviceSongs.setBackgroundResource(R.drawable.bg_btn_apply_theme)
            binding.tabDeviceSongs.setTextColor(Color.WHITE)
            binding.tabCuratedRingtones.setBackgroundResource(R.drawable.bg_booster_card)
            binding.tabCuratedRingtones.setTextColor(Color.parseColor("#A08EBC"))
        }
    }

    private fun setupRecyclerView() {
        adapter = RingtoneAdapter(
            items = displayedList,
            onPreviewClick = { item, position -> handlePreviewClick(item, position) },
            onSetClick = { item -> showSetRingtoneDialog(item) }
        )
        binding.rvRingtonesList.layoutManager = LinearLayoutManager(this)
        binding.rvRingtonesList.adapter = adapter
    }

    private fun loadData() {
        lifecycleScope.launch {
            // 1. Generate & load curated tones
            val curatedFiles = withContext(Dispatchers.IO) {
                MelodicToneGenerator.ensurePresetsGenerated(this@FreeRingtonesActivity)
            }

            curatedRingtones.clear()
            for ((index, file) in curatedFiles.withIndex()) {
                val preset = MelodicToneGenerator.PRESETS.getOrNull(index)
                curatedRingtones.add(
                    RingtoneModel(
                        id = preset?.id ?: file.nameWithoutExtension,
                        title = preset?.title ?: file.nameWithoutExtension,
                        tag = preset?.tag ?: "Tone",
                        file = file,
                        durationMs = 15000L,
                        isFromDevice = false
                    )
                )
            }

            // 2. Load device songs in background
            val musicRepo = MusicRepository(this@FreeRingtonesActivity)
            val songs = withContext(Dispatchers.IO) {
                try {
                    musicRepo.fetchAllSongs()
                } catch (_: Exception) {
                    emptyList()
                }
            }

            deviceSongs.clear()
            for (song in songs) {
                val songFile = File(song.path)
                if (songFile.exists()) {
                    deviceSongs.add(
                        RingtoneModel(
                            id = song.id,
                            title = song.title,
                            tag = song.artist,
                            file = songFile,
                            durationMs = song.duration,
                            isFromDevice = true
                        )
                    )
                }
            }

            filterList(binding.etSearchRingtone.text.toString())
        }
    }

    private fun filterList(query: String) {
        val source = if (isCuratedTabSelected) curatedRingtones else deviceSongs
        val filtered = if (query.isBlank()) {
            source
        } else {
            source.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.tag.contains(query, ignoreCase = true)
            }
        }
        displayedList.clear()
        displayedList.addAll(filtered)
        adapter.updateList(displayedList)
    }

    private fun handlePreviewClick(item: RingtoneModel, position: Int) {
        if (currentPlayingPosition == position && previewMediaPlayer != null) {
            if (previewMediaPlayer?.isPlaying == true) {
                previewMediaPlayer?.pause()
                adapter.setPlaybackState(position, false)
                binding.btnBottomPlayPause.setImageResource(R.drawable.ic_widget_play)
            } else {
                previewMediaPlayer?.start()
                adapter.setPlaybackState(position, true)
                binding.btnBottomPlayPause.setImageResource(R.drawable.ic_widget_pause)
            }
            return
        }

        stopPreview()
        currentPlayingPosition = position
        currentPlayingItem = item

        try {
            previewMediaPlayer = MediaPlayer().apply {
                setDataSource(this@FreeRingtonesActivity, Uri.fromFile(item.file))
                isLooping = true
                prepare()
                start()
            }
            adapter.setPlaybackState(position, true)

            // Show bottom preview bar
            binding.layoutBottomPreview.visibility = View.VISIBLE
            binding.tvBottomPreviewTitle.text = item.title
            binding.btnBottomPlayPause.setImageResource(R.drawable.ic_widget_pause)

        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_failed_preview, e.message ?: ""), Toast.LENGTH_SHORT).show()
            stopPreview()
        }
    }

    private fun stopPreview() {
        try {
            previewMediaPlayer?.stop()
            previewMediaPlayer?.release()
        } catch (_: Exception) {}
        previewMediaPlayer = null
        val oldPos = currentPlayingPosition
        currentPlayingPosition = -1
        currentPlayingItem = null

        if (oldPos != -1) {
            adapter.setPlaybackState(oldPos, false)
        }
        binding.layoutBottomPreview.visibility = View.GONE
    }

    private fun showSetRingtoneDialog(item: RingtoneModel) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = LayoutSetRingtoneDialogBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvDialogRingtoneTitle.text = item.title

        dialogBinding.optPhoneRingtone.setOnClickListener {
            dialog.dismiss()
            applyRingtoneWithPermission(item, RingtoneManager.TYPE_RINGTONE)
        }

        dialogBinding.optNotificationTone.setOnClickListener {
            dialog.dismiss()
            applyRingtoneWithPermission(item, RingtoneManager.TYPE_NOTIFICATION)
        }

        dialogBinding.optAlarmTone.setOnClickListener {
            dialog.dismiss()
            applyRingtoneWithPermission(item, RingtoneManager.TYPE_ALARM)
        }

        dialog.show()
    }

    private fun applyRingtoneWithPermission(item: RingtoneModel, type: Int) {
        if (!RingtoneHelper.canModifySettings(this)) {
            showPermissionExplanationDialog()
            return
        }
        RingtoneHelper.applyRingtone(this, item.file, item.title, type)
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_ringtone_msg)
            .setPositiveButton(R.string.btn_allow) { _, _ ->
                RingtoneHelper.openSettingsPermission(this)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPreview()
    }
}
