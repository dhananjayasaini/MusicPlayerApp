package com.dhananjaysaini.musicplayerapp.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.ActivityPlayerBinding
import com.dhananjaysaini.musicplayerapp.utils.ThemeSelectionBottomSheet
import com.dhananjaysaini.musicplayerapp.model.formatDuration
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.EqualizerManager
import com.dhananjaysaini.musicplayerapp.utils.SleepTimerBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.utils.VoiceControlManager
import com.dhananjaysaini.musicplayerapp.viewmodel.FavouriteViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.PlayerViewModel
import java.io.File
import kotlin.text.*

open class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val playerViewModel: PlayerViewModel by viewModels()
    private lateinit var voiceControl: VoiceControlManager
    private lateinit var musicService: MusicService
    private lateinit var equalizerManager: EqualizerManager

    private var timerReceiver: BroadcastReceiver? = null
    private var currentRemainingTime: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
     //   setTheme(R.style.Theme_MusicPlayerApp)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true  // dark icons

        musicService = MusicService()

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (MusicService.playlist.isEmpty()) {
            finish()
            return
        }

        ThemeManager.applyThemeToActivity(this)

        binding.themeBtnPA.setOnClickListener{
            ThemeManager.saveTheme(this, 0)

            val themeSheet = ThemeSelectionBottomSheet()
            themeSheet.show(supportFragmentManager, "ThemeSheet")
        }

        observeViewModel()
        setupClicks()
        checkAudioPermission()
        setToEqualizer()
        voiceControllerPa()
        sleepTimerBottomSheet()

        binding.shareBtnPA.setOnClickListener {
            shareAudioFile()
        }

        sendBroadcast(Intent("REQUEST_UI_UPDATE"))
    }

    private fun observeViewModel() {
        playerViewModel.uiState.observe(this) { state ->

            val song = state.currentSong ?: return@observe

            binding.songNamePA.text = song.title
            binding.songAlbumPA.text = song.album
            binding.startTimePA.text = formatDuration(state.positionMs.toLong())
            binding.endTimePA.text = formatDuration(state.durationMs.toLong())

            Glide.with(this)
                .load(song.artUri)
                .apply(RequestOptions().placeholder(R.drawable.itunes))
                .into(binding.songImgPA)

            binding.seekBarPA.progress = state.positionMs
            binding.seekBarPA.max = state.durationMs

            binding.playPauseBtnPA.setImageResource(
                if (state.isPlaying) R.drawable.ic_pause
                else R.drawable.ic_play
            )

            binding.favouriteBtnPA.setImageResource(
                if (state.isFavourite) R.drawable.favourite_filled_icon
                else R.drawable.favourite_empty_icon
            )

            binding.repeatBtnPA.setImageResource(
                if (state.isRepeat) R.drawable.repeat_one_icon
                else R.drawable.repeat_icon
            )
        }
    }

    private fun setupClicks() {

        binding.playPauseBtnPA.setOnClickListener {
            playerViewModel.playPause()
        }

        binding.nextBtnPA.setOnClickListener {
            playerViewModel.nextSong()
        }

        binding.prevBtnPA.setOnClickListener {
            playerViewModel.previousSong()
        }

        binding.favouriteBtnPA.setOnClickListener {
            playerViewModel.toggleFavourite()

            val song = MusicService.playlist
                .getOrNull(MusicService.position)
                ?: return@setOnClickListener

            // 🔥 SAME playerViewModel jo Fragment use kar raha hai
            ViewModelProvider(this)[FavouriteViewModel::class.java]
                .toggle(song.id)
        }

        binding.repeatBtnPA.setOnClickListener {
            playerViewModel.toggleRepeat()
        }

        binding.seekBarPA.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                    if (f) playerViewModel.seekTo(p)
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            }
        )

        binding.backBtnPA.setOnClickListener { finish() }
    }

    private fun voiceControllerPa() {
        voiceControl = VoiceControlManager(this)
        binding.voiceControlPa.btnMic.setOnClickListener {
            voiceControl.startListening()
        }
    }

    private fun checkAudioPermission() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    private fun setToEqualizer() {
        equalizerManager = EqualizerManager(this)

        binding.btnEqualizer.setOnClickListener {
            val sessionId = musicService.getAudioSessionId()
            equalizerManager.showEqualizer(sessionId)
        }
    }

    private fun sleepTimerBottomSheet() {
        binding.btnSleepTimer.setOnClickListener {

            val bottomSheet = SleepTimerBottomSheet()
            bottomSheet.show(supportFragmentManager, "SleepTimerBottomSheet")
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(this)

        timerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                if (intent?.action == Constants.ACTION_TIMER_UPDATE) {

                    currentRemainingTime =
                        intent.getLongExtra(Constants.EXTRA_REMAINING_TIME, 0L)

                    if (currentRemainingTime > 0) {
                        binding.txtTimerStatus.visibility = View.VISIBLE
                        binding.txtTimerStatus.text = formatTime(currentRemainingTime)
                    } else {
                        binding.playPauseBtnPA.setImageResource(R.drawable.ic_play)
                        binding.txtTimerStatus.visibility = View.GONE
                        binding.txtTimerStatus.text = "Timer: Off"
                    }
                } else if (intent?.action == Constants.ACTION_MUSIC_STOPPED_BY_TIMER) {
                    binding.playPauseBtnPA.setImageResource(R.drawable.ic_play)

                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_TIMER_UPDATE)
            addAction(Constants.ACTION_MUSIC_STOPPED_BY_TIMER)
        }
        ContextCompat.registerReceiver(
            this,
            timerReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        equalizerManager.release()
    }

    override fun onPause() {
        super.onPause()
        timerReceiver?.let { unregisterReceiver(it) }
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun shareAudioFile() {

        try {
            val audio = MusicService.playlist[MusicService.position]
            val file = File(audio.path)

            if (!file.exists()) {
                Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider", file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_SUBJECT, "Listen to this audio!")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this audio:\nTitle: ${audio.title}\nArtist: ${audio.artist}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share audio file via"))
        }
        catch (e: Exception) {
            Toast.makeText(this, "Error sharing file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

}

