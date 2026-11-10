package com.dhananjaysaini.musicplayerapp.service

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.content.pm.ServiceInfo
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.repository.PlayHistoryRepository
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.widget.RemoteViews
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.dhananjaysaini.musicplayerapp.model.formatDuration
import com.dhananjaysaini.musicplayerapp.utils.NotificationWaveformHelper
import com.dhananjaysaini.musicplayerapp.utils.VolumeBoosterManager
import com.dhananjaysaini.musicplayerapp.widget.MusicAppWidgetProvider
import java.io.IOException

class MusicService : Service() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
        var playlist: ArrayList<Music> = arrayListOf()
        var position: Int = 0
        var allSongs: List<Music> = emptyList()
        var musicService: MusicService? = null
        var song: Music? = null
        val currentSongLiveData = MutableLiveData<Music?>()
        val isPlayingLiveData = MutableLiveData<Boolean>()
    }

    private lateinit var receiver: BroadcastReceiver
    private var isServiceStarted = false
    private var lastNotifProgressUpdateTime = 0L
    private var mediaSession: MediaSessionCompat? = null

    private var sleepTimer: CountDownTimer? = null
    var remainingTime: Long = 0L
    var isTimerRunning: Boolean = false


    private val uiHandler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            try {
                val currentPosition = mediaPlayer?.currentPosition ?: 0
                val duration = mediaPlayer?.duration ?: 0

                // Broadcast current position regularly
                sendBroadcast(Intent("UPDATE_UI").apply {
                    setPackage(packageName)
                    putExtra("Index", position)
                    putExtra("isPlaying", mediaPlayer?.isPlaying == true)
                    putExtra("currentMs", currentPosition)
                    putExtra("durationMs", duration)
                    putExtra("title", playlist.getOrNull(position)?.title)
                    putExtra("artist", playlist.getOrNull(position)?.artist)
                    putExtra("artUri", playlist.getOrNull(position)?.artUri)
                })

                // Periodic notification waveform update (every ~1000ms while playing)
                val now = System.currentTimeMillis()
                if (now - lastNotifProgressUpdateTime >= 1000L && mediaPlayer?.isPlaying == true) {
                    lastNotifProgressUpdateTime = now
                    updateNotification(true)
                }
            } catch (e: Exception) { /* ignore*/
            }
            // schedule next update only if still playing
            if (mediaPlayer?.isPlaying == true) {
                uiHandler.postDelayed(this, 500) // update every 500ms
            }
        }
    }

    private fun startProgressUpdates() {
        uiHandler.removeCallbacks(progressRunnable)
        uiHandler.post(progressRunnable)
    }

    private fun stopProgressUpdates() {
        uiHandler.removeCallbacks(progressRunnable)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        super.onCreate()

        musicService = this

        createNotificationChannel()
        initMediaSession()
        registerReceiver()

        if (mediaPlayer == null) mediaPlayer = MediaPlayer()
        mediaPlayer?.setOnCompletionListener { handleCompletion() }

        mediaPlayer?.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)

    }

    private fun initMediaSession() {
        try {
            mediaSession = MediaSessionCompat(this, "MusicPlayerServiceSession").apply {
                setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        val intent = Intent(this@MusicService, MusicService::class.java).apply {
                            action = Constants.ACTION_PLAY
                        }
                        startService(intent)
                    }

                    override fun onPause() {
                        val intent = Intent(this@MusicService, MusicService::class.java).apply {
                            action = Constants.ACTION_PAUSE
                        }
                        startService(intent)
                    }

                    override fun onSkipToNext() {
                        val intent = Intent(this@MusicService, MusicService::class.java).apply {
                            action = Constants.ACTION_NEXT
                        }
                        startService(intent)
                    }

                    override fun onSkipToPrevious() {
                        val intent = Intent(this@MusicService, MusicService::class.java).apply {
                            action = Constants.ACTION_PREVIOUS
                        }
                        startService(intent)
                    }

                    override fun onSeekTo(pos: Long) {
                        val intent = Intent(this@MusicService, MusicService::class.java).apply {
                            action = Constants.ACTION_SEEK_TO
                            putExtra("seekToMs", pos.toInt())
                        }
                        startService(intent)
                    }

                    override fun onStop() {
                        val intent = Intent(this@MusicService, MusicService::class.java).apply {
                            action = Constants.ACTION_CLOSE_NOTIFICATION
                        }
                        startService(intent)
                    }
                })
                isActive = true
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error initializing MediaSession: ${e.message}")
        }
    }

    private fun updateMediaSessionState(
        isPlaying: Boolean,
        currentMs: Long,
        durationMs: Long,
        song: Music?,
        artwork: Bitmap?
    ) {
        val session = mediaSession ?: return
        try {
            val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            val playbackState = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, currentMs, 1.0f)
                .build()
            session.setPlaybackState(playbackState)

            if (song != null) {
                val metadataBuilder = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
                if (artwork != null) {
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                }
                session.setMetadata(metadataBuilder.build())
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error updating MediaSession: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

//        if (!isServiceStarted) {
//            startForeground(Constants.NOTIFICATION_ID, buildNotification(false))
//            isServiceStarted = true
//        }

        song = Music(
            path = "",
            title = "",
            album = "",
            duration = 0L,
            artUri = null.toString(),
            artist = "",
            id = "",
            date = 0L
        )

        if (playlist.isEmpty()) {
            return START_NOT_STICKY
        }


        when (intent?.action) {
            Constants.ACTION_TOGGLE_PLAY -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    notifyUIAndUpdateNotification()
                } else {
                    if (song == null || song?.path.isNullOrEmpty()) {
                        playAt(position, startForegroundNow = false)
                    } else {
                        try {
                            mediaPlayer?.start()
                            sendBroadcast(Intent("SHOW_MINI_PLAYER").setPackage(packageName))
                            notifyUIAndUpdateNotification()
                        } catch (e: Exception) {
                            playAt(position, startForegroundNow = false)
                        }
                    }
                }
            }
        }
        createNotificationChannel()

        when (intent?.action) {
            // New list coming from Activity
            Constants.ACTION_PLAY_NEW_LIST -> {

                @Suppress("UNCHECKED_CAST")
                val list = intent.getSerializableExtra("musicList") as? ArrayList<Music>
                    ?: playlist // fallback
                val pos = intent.getIntExtra("songPosition", 0)
                if (list.isNotEmpty()) {
                    playlist = list
                    position = pos.coerceIn(0, playlist.lastIndex)
                    playAt(position, startForegroundNow = true)
                    // sendBroadcast(Intent("REQUEST_UI_UPDATE"))

                } else {
                    Log.d("MusicService", "Received empty playlist in ACTION_PLAY_NEW_LIST")
                }
            }

            Constants.ACTION_PLAY -> {

                if (mediaPlayer == null) {

                    playAt(position, false)   // fresh start
                }
                else if (mediaPlayer?.isPlaying != true) {
                    mediaPlayer?.start()

                    sendBroadcast(Intent("SHOW_MINI_PLAYER").setPackage(packageName))
                    startProgressUpdates()
                    notifyUIAndUpdateNotification()

                    val currentSong = playlist.getOrNull(position)
                    if (currentSong != null) {
                        updatePlayHistory(currentSong)
                    }
                }
            }

            Constants.ACTION_PLAY_BOTTOM_SHEET -> {

                val clickedSong = intent.getSerializableExtra("song") as? Music

                if (clickedSong != null) {

                    val index = playlist.indexOfFirst { it.id == clickedSong.id }

                    if (index != -1) {

                        position = index

                    } else {

                        playlist.clear()
                        playlist.add(clickedSong)
                        position = 0
                    }
                }

                // 🔥 ALWAYS playAt (no resume logic here)
                playAt(position, false)
            }

            Constants.ACTION_PAUSE -> {

                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()

                    broadcastUiUpdateAll()
                    updateNotification(false)

                    stopProgressUpdates()
                    notifyUIAndUpdateNotification()
                }
            }

            Constants.ACTION_NEXT -> {

                if (playlist.isNotEmpty()) {
                    position = (position + 1) % playlist.size
                    playAt(position, startForegroundNow = false)
                }
            }

            Constants.ACTION_PREVIOUS -> {

                if (playlist.isNotEmpty()) {
                    position = if (position == 0) playlist.lastIndex else position - 1
                    playAt(position, startForegroundNow = false)
                }
            }

            // Jump to a specific index (used when user taps a song)
            Constants.ACTION_PLAY_AT -> {
                val target = intent.getIntExtra("songPosition", position)
                if (playlist.isNotEmpty()) {
                    position = target.coerceIn(0, playlist.lastIndex)
                    playAt(position, startForegroundNow = false)
                }
            }

            // Seek bar change from Activity
            Constants.ACTION_SEEK_TO -> {
                val ms = intent.getIntExtra("seekToMs", 0)
                if (ms >= 0) {
                    mediaPlayer?.seekTo(ms)
                    notifyUIAndUpdateNotification() // update time/icon if needed
                }
            }

            Constants.ACTION_REWIND_10 -> {
                val current = mediaPlayer?.currentPosition ?: 0
                val target = (current - 10000).coerceAtLeast(0)
                mediaPlayer?.seekTo(target)
                notifyUIAndUpdateNotification()
            }

            Constants.ACTION_FORWARD_10 -> {
                val current = mediaPlayer?.currentPosition ?: 0
                val total = mediaPlayer?.duration ?: 0
                val target = (current + 10000).coerceAtMost(total)
                mediaPlayer?.seekTo(target)
                notifyUIAndUpdateNotification()
            }

            Constants.ACTION_CLOSE_NOTIFICATION -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
                stopProgressUpdates()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(Constants.NOTIFICATION_ID)
                isServiceStarted = false
                broadcastUiUpdateAll()
            }

            // Optional: refresh notification state externally
            Constants.ACTION_REFRESH_NOTIFICATION -> {
                buildNotification(mediaPlayer?.isPlaying == true)
            }

            Constants.ACTION_PLAY_BOTTOM_SHEET -> {

                val index = intent.getIntExtra(Constants.EXTRA_INDEX, position)

                playAt(index, true)
            }

            Constants.ACTION_PLAY_NEXT -> {

                val song = intent.getSerializableExtra(Constants.EXTRA_SONG) as Music

                addToPlayNext(song)
            }

            Constants.ACTION_ADD_TO_QUEUE -> {

                val song = intent.getSerializableExtra(Constants.EXTRA_SONG) as Music

                addToQueue(song)
            }
        }

        when (intent?.action) {

            Constants.ACTION_START_TIMER -> {
                val time = intent.getLongExtra(Constants.EXTRA_TIMER_TIME, 0L)
                if (time > 0) startSleepTimer(time)
            }

            Constants.ACTION_CANCEL_TIMER -> {
                cancelSleepTimer()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
        } catch (_: Exception) {}
        try {
            val prefs = getSharedPreferences("widget_playback_state", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isPlaying", false).apply()
            MusicAppWidgetProvider.updateAllWidgets(this)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(receiver)
        } catch (_: Exception) { /* ignore */
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------ Core playback ------------------

    private fun startForegroundServiceCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                Constants.NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                }
            )
        } else {
            startForeground(Constants.NOTIFICATION_ID, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun playAt(index: Int, startForegroundNow: Boolean) {
        if (playlist.isEmpty()) return
        position = index.coerceIn(0, playlist.lastIndex)
        val track = playlist[position]
        song = track

        currentSongLiveData.postValue(track)
        isPlayingLiveData.postValue(true)

        if (!isServiceStarted) {
            startForegroundServiceCompat(buildNotification(true))
            isServiceStarted = true
        } else {
            updateNotification(true)
        }

        try {
            val mp = mediaPlayer ?: MediaPlayer().also {
                mediaPlayer = it
            }

            mp.setOnCompletionListener(null)   // ⭐ FIX
            mp.reset()
            mp.setDataSource(applicationContext, Uri.parse(track.path))
            mp.prepare()
            mp.setOnCompletionListener { handleCompletion() }
            mp.start()
            VolumeBoosterManager.init(this)
            VolumeBoosterManager.attachSession(mp.audioSessionId)

            // repeat flag comes from PlayerActivity prefs (optional safeguard)
            val isRepeat = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("isRepeat", false)
            mp.isLooping = isRepeat
            mp.start()

            sendBroadcast(Intent("SHOW_MINI_PLAYER").setPackage(packageName))

            if (!isServiceStarted) {
                startForegroundServiceCompat(buildNotification(true))
                isServiceStarted = true
            } else {
                updateNotification(true)
            }

            broadcastUiUpdateAll()
            startProgressUpdates()

        } catch (e: Exception) {
            Log.e("MusicService", "playAt error: ${e.message}", e)
            broadcastUiUpdateAll()
        }
        updatePlayHistory(playlist[position])
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun handleCompletion() {
        // honor repeat-one
        val isRepeat = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("isRepeat", false)

        if (isRepeat) {
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
            sendBroadcast(Intent("SHOW_MINI_PLAYER").setPackage(packageName))
            notifyUIAndUpdateNotification()
            return
        }

        // next
        if (playlist.isNotEmpty()) {
            position = (position + 1) % playlist.size
            sendBroadcast(Intent("SHOW_MINI_PLAYER").setPackage(packageName))
            playAt(position, startForegroundNow = false)
            Log.d("musicservice1", "songPlus $position")
        }
    }


    // ------------------ Notification ------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Channel for music playback" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun buildNotification(isPlaying: Boolean): Notification {
        val currentSong = playlist.getOrNull(position)
        val titleText = currentSong?.title ?: getString(R.string.app_name)
        val artistText = currentSong?.artist ?: "Unknown Artist"
        val currentMs = mediaPlayer?.currentPosition ?: 0
        val durationMs = (mediaPlayer?.duration ?: currentSong?.duration?.toInt() ?: 0).coerceAtLeast(1)
        val progressFraction = (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

        val playerIntent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("class", "Notification")
            putExtra("index", position)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val contentPI = PendingIntent.getActivity(
            this, 200, playerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Explicit Service PendingIntents for Notification Buttons
        val prevPI = PendingIntent.getService(
            this, 201,
            Intent(this, MusicService::class.java).apply { action = Constants.ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPausePI = PendingIntent.getService(
            this, 202,
            Intent(this, MusicService::class.java).apply { action = Constants.ACTION_TOGGLE_PLAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextPI = PendingIntent.getService(
            this, 203,
            Intent(this, MusicService::class.java).apply { action = Constants.ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rewindPI = PendingIntent.getService(
            this, 204,
            Intent(this, MusicService::class.java).apply { action = Constants.ACTION_REWIND_10 },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val forwardPI = PendingIntent.getService(
            this, 205,
            Intent(this, MusicService::class.java).apply { action = Constants.ACTION_FORWARD_10 },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val closePI = PendingIntent.getService(
            this, 206,
            Intent(this, MusicService::class.java).apply { action = Constants.ACTION_CLOSE_NOTIFICATION },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val artworkBitmap = getNotificationArtwork(currentSong?.artUri)

        // Dynamic Waveform
        val waveformBitmap = NotificationWaveformHelper.generateWaveformBitmap(
            width = 340,
            height = 36,
            progressFraction = progressFraction
        )

        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play

        // 1. Collapsed View
        val smallViews = RemoteViews(packageName, R.layout.layout_notification_small).apply {
            setTextViewText(R.id.notif_small_title, titleText)
            setTextViewText(R.id.notif_small_artist, artistText)
            setImageViewResource(R.id.notif_small_play_pause, playPauseIcon)
            if (artworkBitmap != null) {
                setImageViewBitmap(R.id.notif_small_art, artworkBitmap)
            } else {
                setImageViewResource(R.id.notif_small_art, R.drawable.music_player_icon_splash_screen)
            }
            setOnClickPendingIntent(R.id.notif_small_prev, prevPI)
            setOnClickPendingIntent(R.id.notif_small_play_pause, playPausePI)
            setOnClickPendingIntent(R.id.notif_small_next, nextPI)
            setOnClickPendingIntent(R.id.notif_small_root, contentPI)
            setOnClickPendingIntent(R.id.notif_small_art, contentPI)
        }

        // 2. Expanded View
        val bigViews = RemoteViews(packageName, R.layout.layout_notification_big).apply {
            setTextViewText(R.id.notif_big_title, titleText)
            setTextViewText(R.id.notif_big_artist, artistText)
            setTextViewText(R.id.notif_time_current, formatDuration(currentMs.toLong()))
            setTextViewText(R.id.notif_time_total, formatDuration(durationMs.toLong()))
            setImageViewResource(R.id.notif_big_play_pause, playPauseIcon)
            setImageViewBitmap(R.id.notif_waveform, waveformBitmap)
            if (artworkBitmap != null) {
                setImageViewBitmap(R.id.notif_big_art, artworkBitmap)
            } else {
                setImageViewResource(R.id.notif_big_art, R.drawable.music_player_icon_splash_screen)
            }
            setOnClickPendingIntent(R.id.notif_big_prev, prevPI)
            setOnClickPendingIntent(R.id.notif_big_play_pause, playPausePI)
            setOnClickPendingIntent(R.id.notif_big_next, nextPI)
            setOnClickPendingIntent(R.id.notif_btn_rewind, rewindPI)
            setOnClickPendingIntent(R.id.notif_btn_forward, forwardPI)
            setOnClickPendingIntent(R.id.notif_big_close, closePI)
            setOnClickPendingIntent(R.id.notif_big_art, contentPI)
            setOnClickPendingIntent(R.id.notif_waveform, contentPI)
            setOnClickPendingIntent(R.id.notif_big_root, contentPI)
        }

        val builder = NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setSmallIcon(R.drawable.music_player_icon_splash_screen)
            .setContentTitle(titleText)
            .setContentText(artistText)
            .setLargeIcon(artworkBitmap)
            .setCustomContentView(smallViews)
            .setCustomBigContentView(bigViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPI)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)

        // Sync with MediaSession
        mediaSession?.let {
            updateMediaSessionState(isPlaying, currentMs.toLong(), durationMs.toLong(), currentSong, artworkBitmap)
        }

        return builder.build()
    }

    private fun getNotificationArtwork(uriString: String?): Bitmap? {
        if (uriString.isNullOrEmpty() || uriString == "null") {
            return decodeSampledBitmapFromResource(resources, R.drawable.music_player_icon_splash_screen, 150, 150)?.let {
                getRoundedBitmap(it, 20f)
            }
        }
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val targetSize = 150
            var inSampleSize = 1
            while (options.outWidth / (inSampleSize * 2) >= targetSize &&
                options.outHeight / (inSampleSize * 2) >= targetSize
            ) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val stream2 = contentResolver.openInputStream(uri) ?: return null
            val raw = BitmapFactory.decodeStream(stream2, null, decodeOptions)
            stream2.close()

            raw?.let {
                val scaled = Bitmap.createScaledBitmap(it, targetSize, targetSize, true)
                getRoundedBitmap(scaled, 20f)
            }
        } catch (e: Exception) {
            decodeSampledBitmapFromResource(resources, R.drawable.music_player_icon_splash_screen, 150, 150)?.let {
                getRoundedBitmap(it, 20f)
            }
        }
    }

    private fun decodeSampledBitmapFromResource(res: android.content.res.Resources, resId: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(res, resId, options)
        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        val decoded = BitmapFactory.decodeResource(res, resId, options) ?: return null
        return Bitmap.createScaledBitmap(decoded, reqWidth, reqHeight, true)
    }

    private fun getRoundedBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateNotification(isPlaying: Boolean) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(Constants.NOTIFICATION_ID, buildNotification(isPlaying))
    }


    // ------------------ Receiver (buttons from notification) ------------------

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver() {
        receiver = object : BroadcastReceiver() {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onReceive(context: Context?, intent: Intent?) {

                if (intent?.action == "REQUEST_UI_UPDATE") {
                    broadcastUiUpdateAll()    // send fresh UI data
                    return
                }

                when (intent?.action) {

                    Constants.ACTION_PLAY -> {

                        if (mediaPlayer?.isPlaying == false) {
                            mediaPlayer?.start()
                            sendBroadcast(Intent("SHOW_MINI_PLAYER"))
                            notifyUIAndUpdateNotification()

                        } else if (mediaPlayer == null) {
                            playAt(position, false)
                            return
                        }
                    }

                    Constants.ACTION_PAUSE -> {

                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer!!.pause()
                            notifyUIAndUpdateNotification()
                        }
                    }

                    Constants.ACTION_NEXT -> {
                        if (playlist.isNotEmpty()) {
                            position = (position + 1) % playlist.size
                            playAt(position, startForegroundNow = false)
                            sendBroadcast(Intent("SHOW_MINI_PLAYER").setPackage(packageName))
                        }
                    }

                    Constants.ACTION_PREVIOUS -> {
                        if (playlist.isNotEmpty()) {
                            position = if (position == 0) playlist.lastIndex else position - 1
                            playAt(position, startForegroundNow = false)
                            sendBroadcast(Intent("SHOW_MINI_PLAYER").setPackage(packageName))
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_PLAY)
            addAction(Constants.ACTION_PAUSE)
            addAction(Constants.ACTION_NEXT)
            addAction(Constants.ACTION_PREVIOUS)
            addAction("REQUEST_UI_UPDATE")
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // ------------------ UI broadcast helpers ------------------

    private fun broadcastUiUpdateAll() {
        if (playlist.isEmpty() || position !in playlist.indices) return

        val song = playlist[position]
        val isPlaying = mediaPlayer?.isPlaying == true
        val currentMs = mediaPlayer?.currentPosition ?: 0
        val durationMs = mediaPlayer?.duration ?: 0

        currentSongLiveData.postValue(song)
        isPlayingLiveData.postValue(isPlaying)

        // Update titles, images, seekbar, etc.
        sendBroadcast(Intent("UPDATE_UI").apply {
            setPackage(packageName)
            putExtra("Index", position)
            putExtra("title", song.title)
            putExtra("artist", song.artist)
            putExtra("path", song.path)
            putExtra("isPlaying", isPlaying)
            putExtra("currentMs", currentMs)
            putExtra("durationMs", durationMs)
            putExtra("artUri", song.artUri)
        })
        // Update play/pause icon
        sendBroadcast(Intent("MUSIC_PLAYER_UI_UPDATE").apply {
            setPackage(packageName)
            putExtra("action", "playPause")
            putExtra("isPlaying", isPlaying)
        })

        // Persist state & update widget
        try {
            val prefs = getSharedPreferences("widget_playback_state", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("title", song.title)
                .putString("artist", song.artist)
                .putString("artUri", song.artUri)
                .putBoolean("isPlaying", isPlaying)
                .apply()
            MusicAppWidgetProvider.updateAllWidgets(this)
        } catch (_: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun notifyUIAndUpdateNotification() {
        broadcastUiUpdateAll()
        updateNotification(mediaPlayer?.isPlaying == true)
    }

    private fun getBitmapFromUri(uri: String?): Bitmap? {
        return try {
            uri?.let {
                val resolver = applicationContext.contentResolver
                MediaStore.Images.Media.getBitmap(resolver, it.toUri())
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getAudioSessionId(): Int {
        return mediaPlayer?.audioSessionId ?: -1
    }

    fun applyReverbEffect(reverbId: Int) {
        mediaPlayer?.attachAuxEffect(reverbId)
        mediaPlayer?.setAuxEffectSendLevel(1.0f)
    }

    fun startSleepTimer(timeInMillis: Long) {

        sleepTimer?.cancel()
        remainingTime = timeInMillis

        sleepTimer = object : CountDownTimer(timeInMillis, 1000) {

            override fun onTick(ms: Long) {
                remainingTime = ms
                sendTimerUpdateBroadcast(ms)
            }

            override fun onFinish() {
                remainingTime = 0L
                sendTimerUpdateBroadcast(0L)
                stopMusicBySleepTimer()
            }

        }.start()
    }

    fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        remainingTime = 0L
        sendTimerUpdateBroadcast(0L)
    }

    private fun stopMusicBySleepTimer() {

        try {
            mediaPlayer?.pause()

            stopForeground(false)
            sendTimerUpdateBroadcast(0)
            sendBroadcast(Intent(Constants.ACTION_MUSIC_STOPPED_BY_TIMER))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendTimerUpdateBroadcast(time: Long) {
        val intent = Intent(Constants.ACTION_TIMER_UPDATE)
        intent.putExtra(Constants.EXTRA_REMAINING_TIME, time)
        sendBroadcast(intent)
    }

    fun updatePlayHistory(song: Music) {

        CoroutineScope(Dispatchers.IO).launch {

            val dao = MusicDatabase
                .getDatabase(applicationContext)
                .playHistoryDao()

            val repo = PlayHistoryRepository(dao)

            repo.updatePlay(song.id)
        }
    }

    private fun addToPlayNext(song: Music) {

        if (playlist.isEmpty()) return

        playlist.removeAll { it.id == song.id }

        val nextIndex = (position + 1).coerceAtMost(playlist.size)

        playlist.add(nextIndex, song)

        Toast.makeText(this, "it will play next", Toast.LENGTH_SHORT).show()
    }

    private fun addToQueue(song: Music) {

        // 🔥 avoid duplicates
        if (playlist.any { it.id == song.id }) {

            Toast.makeText(this, "Already in queue", Toast.LENGTH_SHORT).show()
            return
        }
        playlist.add(song)

        Toast.makeText(this, "Added to queue", Toast.LENGTH_SHORT).show()
    }

}

