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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MusicService : Service() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
        var playlist: ArrayList<Music> = arrayListOf()
        var position: Int = 0
        var allSongs: List<Music> = emptyList()
        var musicService: MusicService? = null
        var song: Music? = null
    }

    private lateinit var receiver: BroadcastReceiver
    private var isServiceStarted = false

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
                    putExtra("Index", position)
                    putExtra("isPlaying", mediaPlayer?.isPlaying == true)
                    putExtra("currentMs", currentPosition)
                    putExtra("durationMs", duration)
                    putExtra("title", playlist.getOrNull(position)?.title)
                    putExtra("artist", playlist.getOrNull(position)?.artist)
                    putExtra("artUri", playlist.getOrNull(position)?.artUri)
                })
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
        registerReceiver()

        if (mediaPlayer == null) mediaPlayer = MediaPlayer()
        mediaPlayer?.setOnCompletionListener { handleCompletion() }

        mediaPlayer?.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)

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
                } else {
                    mediaPlayer?.start()
                    sendBroadcast(Intent("SHOW_MINI_PLAYER"))
                    sendBroadcast(Intent("UPDATE_UI"))
// <-- ADD HERE
                }
                notifyUIAndUpdateNotification()
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

                    sendBroadcast(Intent("SHOW_MINI_PLAYER"))
                    sendBroadcast(Intent("UPDATE_UI"))

                    startProgressUpdates()
                    notifyUIAndUpdateNotification()

                    updatePlayHistory(song!!)
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
        val track = playlist[index]

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

            // repeat flag comes from PlayerActivity prefs (optional safeguard)
            val isRepeat = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("isRepeat", false)
            mp.isLooping = isRepeat
            mp.start()

            sendBroadcast(Intent("SHOW_MINI_PLAYER"))


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
        }
        updatePlayHistory(playlist[index])
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun handleCompletion() {
        // honor repeat-one
        val isRepeat = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("isRepeat", false)

        if (isRepeat) {
            mediaPlayer?.seekTo(0)
            mediaPlayer?.start()
            sendBroadcast(Intent("SHOW_MINI_PLAYER"))
            sendBroadcast(Intent("UPDATE_UI"))

            broadcastUiUpdateAll()
            buildNotification(true)
            return
        }

        // next
        if (playlist.isNotEmpty()) {
            position = (position + 1) % playlist.size
            sendBroadcast(Intent("SHOW_MINI_PLAYER"))
            sendBroadcast(Intent("UPDATE_UI"))
// <-- ADD HERE
            playAt(position, startForegroundNow = false)
            Log.d("musicservice1", "songPlus $position")
            broadcastUiUpdateAll()
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
        val currentPosition = playlist.getOrNull(position)
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseAction = if (isPlaying) Constants.ACTION_PAUSE else Constants.ACTION_PLAY

        val playerIntent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("class", "Notification")
            putExtra("index", position)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, playerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prevPI = PendingIntent.getBroadcast(
            this, 0, Intent(Constants.ACTION_PREVIOUS), PendingIntent.FLAG_IMMUTABLE
        )
        val playPausePI = PendingIntent.getBroadcast(
            this, 1, Intent(playPauseAction), PendingIntent.FLAG_IMMUTABLE
        )
        val nextPI = PendingIntent.getBroadcast(
            this, 2, Intent(Constants.ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE
        )

        val artwork = BitmapFactory.decodeResource(resources, R.drawable.itunes)
        val artworkBitmap = getBitmapFromUri(currentPosition?.artUri)

        return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setContentTitle(currentPosition?.title ?: getString(R.string.app_name))
            .setContentText(currentPosition?.artist ?: "")
            .setSmallIcon(R.drawable.music_player_icon_splash_screen)
            .setLargeIcon(artworkBitmap)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.previous_icon, "Previous", prevPI)
            .addAction(playPauseIcon, "PlayPause", playPausePI)
            .addAction(R.drawable.next_icon, "Next", nextPI)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .build()

        // If we are already foreground, this updates it; otherwise it starts foreground.
        //   startForeground(Constants.NOTIFICATION_ID, notification)
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
                }

                when (intent?.action) {

                    Constants.ACTION_PLAY -> {

                        if (mediaPlayer?.isPlaying == false) {
                            mediaPlayer?.start()
                            sendBroadcast(Intent("SHOW_MINI_PLAYER"))
                            sendBroadcast(Intent("UPDATE_UI"))

                        } else if (mediaPlayer == null) {
                            playAt(position, false)
                            return
                        }

                        // Notification icon update
                        updateNotification(true)
                        broadcastUiUpdateAll()
                    }

                    Constants.ACTION_PAUSE -> {

                        if (mediaPlayer?.isPlaying == true) {
                            mediaPlayer!!.pause()
                        }

                        // Notification icon update
                        updateNotification(false)
                        broadcastUiUpdateAll()
                    }

                    Constants.ACTION_NEXT -> {
                        if (playlist.isNotEmpty()) {
                            position = (position + 1) % playlist.size
                            playAt(position, startForegroundNow = false)
                            sendBroadcast(Intent("SHOW_MINI_PLAYER"))
                            sendBroadcast(Intent("UPDATE_UI"))
                        }
                    }

                    Constants.ACTION_PREVIOUS -> {
                        if (playlist.isNotEmpty()) {
                            position = if (position == 0) playlist.lastIndex else position - 1
                            playAt(position, startForegroundNow = false)
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

        val song = playlist[position]

        val current = playlist.getOrNull(position)
        val isPlaying = mediaPlayer?.isPlaying == true
        val currentMs = mediaPlayer?.currentPosition ?: 0
        val durationMs = mediaPlayer?.duration ?: 0

        if (playlist.isEmpty() || position !in playlist.indices) return


        // Update titles, images, seekbar, etc.
        sendBroadcast(Intent("UPDATE_UI").apply {
            putExtra("Index", position)
            putExtra("title", song.title)
            putExtra("artist", song.artist)
            putExtra("path", song.path)

            putExtra("isPlaying", isPlaying)
            putExtra("currentMs", currentMs)
            putExtra("durationMs", durationMs)
            putExtra("artUri", current?.artUri)
        })
        // Update play/pause icon
        sendBroadcast(Intent("MUSIC_PLAYER_UI_UPDATE").apply {
            putExtra("action", "playPause")
        })
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun notifyUIAndUpdateNotification() {
        broadcastUiUpdateAll()
        buildNotification(mediaPlayer?.isPlaying == true)
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

