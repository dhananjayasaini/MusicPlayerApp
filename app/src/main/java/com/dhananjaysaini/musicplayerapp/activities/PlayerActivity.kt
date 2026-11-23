package com.dhananjaysaini.musicplayerapp.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.ActivityPlayerBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.formatDuration
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper
import com.dhananjaysaini.musicplayerapp.utils.EqualizerManager
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.SleepTimerBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.utils.ThemeSelectionBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.WaveformSeekBar
import com.dhananjaysaini.musicplayerapp.viewmodel.FavouriteViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.text.*

open class PlayerActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private lateinit var binding: ActivityPlayerBinding
    private val playerViewModel: PlayerViewModel by viewModels()
    private lateinit var musicService: MusicService
    private lateinit var equalizerManager: EqualizerManager

    private var timerReceiver: BroadcastReceiver? = null
    private var currentRemainingTime: Long = 0L
    private var lastSongId: String? = null
    private var currentBackgroundGradient: Drawable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
     //   setTheme(R.style.Theme_MusicPlayerApp)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        musicService = MusicService()

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.playerRoot) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.topLayerPA.updatePadding(top = systemBars.top)
            binding.playerControls.updatePadding(bottom = systemBars.bottom)
            insets
        }

        if (MusicService.playlist.isEmpty()) {
            finish()
            return
        }

        binding.themeBtnPA.setOnClickListener{
            startActivity(Intent(this, SkinThemeActivity::class.java))
        }

        observeViewModel()
        setupClicks()
        setToEqualizer()
        sleepTimerBottomSheet()

        binding.shareBtnPA.setOnClickListener {
            shareAudioFile()
        }

        sendBroadcast(Intent("REQUEST_UI_UPDATE"))

        BackgroundPlaybackPermissionHelper.checkAndRequestOnFirstAudioPlay(this)
    }

    private fun observeViewModel() {
        playerViewModel.uiState.observe(this) { state ->

            val song = state.currentSong ?: return@observe

            binding.songNamePA.text = song.title
            binding.songAlbumPA.text = song.album
            binding.startTimePA.text = formatDuration(state.positionMs.toLong())
            binding.endTimePA.text = formatDuration(state.durationMs.toLong())

            if (lastSongId != song.id) {
                lastSongId = song.id
                loadAndApplySongArtBackground(song)

                val sessionId = musicService.getAudioSessionId()
                if (sessionId > 0) {
                    binding.waveformSeekBarPA.attachVisualizer(sessionId)
                }
            }

            binding.waveformSeekBarPA.durationMs = state.durationMs.toLong()
            binding.waveformSeekBarPA.positionMs = state.positionMs.toLong()
            binding.waveformSeekBarPA.progress = state.positionMs
            binding.waveformSeekBarPA.max = state.durationMs

            binding.playPauseBtnPA.setImageResource(
                if (state.isPlaying) R.drawable.ic_neon_player_pause
                else R.drawable.ic_neon_player_play
            )

            binding.favouriteBtnPA.setImageResource(
                if (state.isFavourite) R.drawable.ic_neon_player_fav_filled
                else R.drawable.ic_neon_player_fav_empty
            )

            binding.repeatBtnPA.setImageResource(
                if (state.isRepeat) R.drawable.ic_neon_player_repeat_one
                else R.drawable.ic_neon_player_repeat
            )
        }
    }

    private fun loadAndApplySongArtBackground(song: Music) {
        CoroutineScope(Dispatchers.IO).launch {
            var bitmap: Bitmap? = null

            // 1. Try MediaMetadataRetriever from local file path (reliable for ID3 embedded album art on all Android versions)
            try {
                if (!song.path.isNullOrEmpty() && File(song.path).exists()) {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(song.path)
                    val artBytes = retriever.embeddedPicture
                    retriever.release()
                    if (artBytes != null && artBytes.isNotEmpty()) {
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 1
                        }
                        bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, options)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Try artUri via ContentResolver if retriever didn't yield artwork
            if (bitmap == null && !song.artUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(song.artUri)
                    contentResolver.openInputStream(uri)?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val finalBitmap = bitmap
            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (finalBitmap != null) {
                    Glide.with(this@PlayerActivity)
                        .load(finalBitmap)
                        .circleCrop()
                        .placeholder(R.drawable.itunes)
                        .into(binding.songImgPA)
                    applyDynamicPalette(finalBitmap, song)
                } else {
                    Glide.with(this@PlayerActivity)
                        .load(R.drawable.itunes)
                        .circleCrop()
                        .into(binding.songImgPA)
                    applyDynamicColorForNoArt(song)
                }
            }
        }
    }

    private fun applyDynamicPalette(bitmap: Bitmap, song: Music) {
        Palette.from(bitmap).generate { palette ->
            if (isFinishing || isDestroyed) return@generate

            val dominant = palette?.getDominantColor(Color.parseColor("#1C1929"))
                ?: Color.parseColor("#1C1929")
            val vibrant = palette?.getVibrantColor(Color.parseColor("#FFD700"))
                ?: palette?.getLightVibrantColor(Color.parseColor("#FFD700"))
                ?: palette?.getMutedColor(Color.parseColor("#FFD700"))
                ?: Color.parseColor("#FFD700")

            // Ambient glow gradient (smooth 3-stop vertical gradient from dominant down to deep dark)
            val topColor = ColorUtils.blendARGB(dominant, Color.BLACK, 0.20f)
            val midColor = ColorUtils.blendARGB(dominant, Color.BLACK, 0.65f)
            val bottomColor = Color.parseColor("#08080C")
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(topColor, midColor, bottomColor)
            )
            currentBackgroundGradient = gradient
            binding.playerRoot.background = gradient
            window.decorView.background = gradient

            applyControlsTint(vibrant)
        }
    }

    private fun applyDynamicColorForNoArt(song: Music) {
        val hues = intArrayOf(
            Color.parseColor("#6A11CB"), // Royal violet
            Color.parseColor("#2575FC"), // Electric blue
            Color.parseColor("#FF416C"), // Passion rose
            Color.parseColor("#8E2DE2"), // Deep purple
            Color.parseColor("#00B4DB"), // Ocean cyan
            Color.parseColor("#F7971E"), // Sunset orange
            Color.parseColor("#11998E"), // Emerald green
            Color.parseColor("#E52D27"), // Crimson red
            Color.parseColor("#B224EF"), // Magenta
            Color.parseColor("#1A2980")  // Midnight navy
        )
        val hash = abs((song.title + song.artist).hashCode())
        val baseColor = hues[hash % hues.size]
        val topColor = ColorUtils.blendARGB(baseColor, Color.BLACK, 0.25f)
        val midColor = ColorUtils.blendARGB(baseColor, Color.BLACK, 0.70f)
        val bottomColor = Color.parseColor("#08080C")
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topColor, midColor, bottomColor)
        )
        currentBackgroundGradient = gradient
        binding.playerRoot.background = gradient
        window.decorView.background = gradient

        applyControlsTint(baseColor)
    }

    private fun applyControlsTint(vibrant: Int) {
        // Waveform Seekbar Tinting
        binding.waveformSeekBarPA.playedColor = vibrant
        binding.waveformSeekBarPA.unplayedColor = ColorUtils.setAlphaComponent(vibrant, 65)

        // Keep native multi-color neon gradients on all player screen icons
        binding.playPauseBtnPA.imageTintList = null
        binding.prevBtnPA.imageTintList = null
        binding.nextBtnPA.imageTintList = null
        binding.favouriteBtnPA.imageTintList = null
        binding.repeatBtnPA.imageTintList = null
        binding.btnEqualizer.imageTintList = null
        binding.btnSleepTimer.imageTintList = null
        binding.shareBtnPA.imageTintList = null
        binding.backBtnPA.imageTintList = null
        binding.themeBtnPA.imageTintList = null
        binding.btnRewind10.imageTintList = null
        binding.btnForward10.imageTintList = null

        binding.songNamePA.setTextColor(Color.WHITE)
        binding.songAlbumPA.setTextColor(ColorUtils.blendARGB(vibrant, Color.WHITE, 0.45f))
        binding.startTimePA.setTextColor(Color.WHITE)
        binding.endTimePA.setTextColor(Color.WHITE)
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
            val song = MusicService.playlist
                .getOrNull(MusicService.position)
                ?: return@setOnClickListener

            val isNowFav = playerViewModel.toggleFavourite()

            ViewModelProvider(this)[FavouriteViewModel::class.java]
                .setFavourite(song.id, isNowFav)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                MusicService.musicService?.updateNotification(MusicService.mediaPlayer?.isPlaying == true)
            }
        }

        binding.repeatBtnPA.setOnClickListener {
            playerViewModel.toggleRepeat()
        }

        binding.btnRewind10.setOnClickListener {
            val current = (MusicService.mediaPlayer?.currentPosition ?: playerViewModel.uiState.value?.positionMs ?: 0)
            val target = (current - 10000).coerceAtLeast(0)
            playerViewModel.seekTo(target)
            binding.waveformSeekBarPA.positionMs = target.toLong()
            binding.waveformSeekBarPA.progress = target
            binding.startTimePA.text = formatDuration(target.toLong())
        }

        binding.btnForward10.setOnClickListener {
            val current = (MusicService.mediaPlayer?.currentPosition ?: playerViewModel.uiState.value?.positionMs ?: 0)
            val duration = (MusicService.mediaPlayer?.duration ?: playerViewModel.uiState.value?.durationMs ?: 0)
            val target = (current + 10000).coerceAtMost(duration)
            playerViewModel.seekTo(target)
            binding.waveformSeekBarPA.positionMs = target.toLong()
            binding.waveformSeekBarPA.progress = target
            binding.startTimePA.text = formatDuration(target.toLong())
        }

        binding.waveformSeekBarPA.onSeekListener = object : WaveformSeekBar.OnWaveformSeekListener {
            override fun onProgressChanged(
                waveformSeekBar: WaveformSeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    playerViewModel.seekTo(progress)
                    waveformSeekBar.positionMs = progress.toLong()
                    binding.startTimePA.text = formatDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(waveformSeekBar: WaveformSeekBar) {}
            override fun onStopTrackingTouch(waveformSeekBar: WaveformSeekBar) {}
        }

        binding.backBtnPA.setOnClickListener { finish() }
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
        playerViewModel.updateUiState()

        val settingsPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (settingsPrefs.getBoolean("keep_screen_on", false)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        currentBackgroundGradient?.let {
            binding.playerRoot.background = it
            window.decorView.background = it
        } ?: run {
            val song = playerViewModel.uiState.value?.currentSong
            if (song != null) {
                loadAndApplySongArtBackground(song)
            }
        }

        timerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                if (intent?.action == Constants.ACTION_TIMER_UPDATE) {

                    currentRemainingTime =
                        intent.getLongExtra(Constants.EXTRA_REMAINING_TIME, 0L)

                    if (currentRemainingTime > 0) {
                        binding.txtTimerStatus.visibility = View.VISIBLE
                        binding.txtTimerStatus.text = formatTime(currentRemainingTime)
                    } else {
                        binding.playPauseBtnPA.setImageResource(R.drawable.ic_neon_player_play)
                        binding.txtTimerStatus.visibility = View.GONE
                        binding.txtTimerStatus.text = getString(R.string.timer_off)
                    }
                } else if (intent?.action == Constants.ACTION_MUSIC_STOPPED_BY_TIMER) {
                    binding.playPauseBtnPA.setImageResource(R.drawable.ic_neon_player_play)

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
        if (::binding.isInitialized) {
            binding.waveformSeekBarPA.releaseVisualizer()
        }
        if (::equalizerManager.isInitialized) {
            equalizerManager.release()
        }
    }

    override fun onPause() {
        super.onPause()
        timerReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {
                // Receiver already unregistered or never registered
            }
        }
        timerReceiver = null
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
                Toast.makeText(this, getString(R.string.toast_file_not_found), Toast.LENGTH_SHORT).show()
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
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_audio_title)))
        }
        catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_error_sharing), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BackgroundPlaybackPermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val serviceIntent = Intent(this, MusicService::class.java).apply {
                    action = Constants.ACTION_REFRESH_NOTIFICATION
                }
                startService(serviceIntent)
            }
        }
    }

}

