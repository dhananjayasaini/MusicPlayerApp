package com.dhananjaysaini.musicplayerapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.RecordingsAdapter
import com.dhananjaysaini.musicplayerapp.databinding.ActivityVoiceRecorderBinding
import com.dhananjaysaini.musicplayerapp.model.RecordingItem
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class VoiceRecorderActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private lateinit var binding: ActivityVoiceRecorderBinding
    private val recordings = mutableListOf<RecordingItem>()
    private lateinit var adapter: RecordingsAdapter

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null

    private enum class RecordState { IDLE, RECORDING, PAUSED }
    private var currentState = RecordState.IDLE

    private var recordStartTime = 0L
    private var recordElapsedTime = 0L

    private val handler = Handler(Looper.getMainLooper())

    // Player for listening to saved recordings
    private var playbackMediaPlayer: MediaPlayer? = null
    private var currentlyPlayingFilePath: String? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (currentState == RecordState.RECORDING) {
                val currentElapsed = recordElapsedTime + (System.currentTimeMillis() - recordStartTime)
                binding.tvRecordTimer.text = formatRecordTime(currentElapsed)
                handler.postDelayed(this, 300)
            }
        }
    }

    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            if (currentState == RecordState.RECORDING) {
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    binding.waveformVisualizer.addAmplitude(maxAmp)
                } catch (_: Exception) {}
                handler.postDelayed(this, 60)
            }
        }
    }

    private val playbackProgressRunnable = object : Runnable {
        override fun run() {
            val player = playbackMediaPlayer
            val path = currentlyPlayingFilePath
            if (player != null && player.isPlaying && path != null) {
                adapter.updateProgress(
                    filePath = path,
                    progressMs = player.currentPosition,
                    durationMs = player.duration
                )
                handler.postDelayed(this, 250)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivityVoiceRecorderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.recorderRoot) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val extraTop = (12 * resources.displayMetrics.density).toInt()
            binding.recorderTopBar.updatePadding(
                top = systemBars.top + extraTop,
                bottom = (8 * resources.displayMetrics.density).toInt()
            )
            binding.rvRecordings.updatePadding(bottom = systemBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.recorderRoot)

        ThemeManager.applyThemeToActivity(this)

        setupUI()
        setupRecyclerView()
        loadSavedRecordings()
    }

    private fun setupUI() {
        binding.btnRecorderBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnMainRecord.setOnClickListener {
            if (checkAudioPermission()) {
                handleMainRecordClick()
            }
        }

        binding.btnSaveRecording.setOnClickListener {
            stopAndSaveRecording()
        }

        binding.btnDiscardRecording.setOnClickListener {
            confirmDiscardRecording()
        }
    }

    private fun setupRecyclerView() {
        adapter = RecordingsAdapter(
            items = recordings,
            onPlayClick = { item, _ -> handlePlayRecording(item) },
            onDeleteClick = { item -> confirmDeleteRecording(item) },
            onRenameClick = { item -> showRenameDialog(item) },
            onShareClick = { item -> shareRecording(item) },
            onSeek = { item, progressMs ->
                if (currentlyPlayingFilePath == item.file.absolutePath) {
                    adapter.currentPlayProgressMs = progressMs
                    if (playbackMediaPlayer != null) {
                        playbackMediaPlayer?.seekTo(progressMs)
                    }
                }
            }
        )
        binding.rvRecordings.layoutManager = LinearLayoutManager(this)
        binding.rvRecordings.adapter = adapter
    }

    private fun handleMainRecordClick() {
        when (currentState) {
            RecordState.IDLE -> startRecording()
            RecordState.RECORDING -> pauseRecording()
            RecordState.PAUSED -> resumeRecording()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startRecording() {
        stopPlayback(keepOpen = true)

        val dir = getRecordingsDir()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        currentRecordingFile = File(dir, "REC_$timeStamp.m4a")

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentRecordingFile!!.absolutePath)
                prepare()
                start()
            }

            currentState = RecordState.RECORDING
            recordStartTime = System.currentTimeMillis()
            recordElapsedTime = 0L

            binding.btnMainRecord.setBackgroundResource(R.drawable.bg_record_btn_active)
            binding.btnMainRecord.setImageResource(R.drawable.ic_widget_pause)
            binding.tvRecordStatus.text = getString(R.string.recording_in_progress)
            binding.tvRecordStatus.setTextColor(Color.parseColor("#FF1744"))

            binding.btnDiscardRecording.visibility = View.VISIBLE
            binding.btnSaveRecording.visibility = View.VISIBLE

            handler.post(timerRunnable)
            handler.post(amplitudeRunnable)

        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_error_start_recorder, e.message ?: ""), Toast.LENGTH_SHORT).show()
            resetRecordingState()
        }
    }

    private fun pauseRecording() {
        if (currentState != RecordState.RECORDING) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
            }
            recordElapsedTime += System.currentTimeMillis() - recordStartTime
            currentState = RecordState.PAUSED

            binding.btnMainRecord.setBackgroundResource(R.drawable.bg_record_btn_idle)
            binding.btnMainRecord.setImageResource(R.drawable.ic_widget_play)
            binding.tvRecordStatus.text = getString(R.string.recording_paused)
            binding.tvRecordStatus.setTextColor(Color.parseColor("#00F5FF"))

            handler.removeCallbacks(timerRunnable)
            handler.removeCallbacks(amplitudeRunnable)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_error_pausing), Toast.LENGTH_SHORT).show()
        }
    }

    private fun resumeRecording() {
        if (currentState != RecordState.PAUSED) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
            }
            recordStartTime = System.currentTimeMillis()
            currentState = RecordState.RECORDING

            binding.btnMainRecord.setBackgroundResource(R.drawable.bg_record_btn_active)
            binding.btnMainRecord.setImageResource(R.drawable.ic_widget_pause)
            binding.tvRecordStatus.text = getString(R.string.recording_in_progress)
            binding.tvRecordStatus.setTextColor(Color.parseColor("#FF1744"))

            handler.post(timerRunnable)
            handler.post(amplitudeRunnable)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_error_resuming), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedRecordings()
    }

    private fun stopAndSaveRecording() {
        if (currentState == RecordState.IDLE) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            android.util.Log.e("VoiceRecorder", "stopAndSaveRecording error: ${e.message}")
        }

        mediaRecorder = null
        currentRecordingFile?.let { savedFile ->
            if (savedFile.exists() && savedFile.length() > 0L) {
                try {
                    android.media.MediaScannerConnection.scanFile(
                        this,
                        arrayOf(savedFile.absolutePath),
                        arrayOf("audio/mp4", "audio/m4a"),
                        null
                    )
                } catch (_: Exception) {}
            }
        }
        resetRecordingState()
        Toast.makeText(this, getString(R.string.recording_saved), Toast.LENGTH_SHORT).show()
        loadSavedRecordings()
    }

    private fun confirmDiscardRecording() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.discard_recording_title))
            .setMessage(getString(R.string.discard_recording_msg))
            .setPositiveButton(getString(R.string.btn_discard)) { _, _ ->
                discardRecording()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun discardRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        currentRecordingFile?.delete()
        resetRecordingState()
        Toast.makeText(this, getString(R.string.recording_discarded), Toast.LENGTH_SHORT).show()
    }

    private fun resetRecordingState() {
        currentState = RecordState.IDLE
        recordStartTime = 0L
        recordElapsedTime = 0L
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(amplitudeRunnable)

        binding.tvRecordTimer.text = "00:00:00"
        binding.tvRecordStatus.text = getString(R.string.ready_to_record)
        binding.tvRecordStatus.setTextColor(Color.parseColor("#A08EBC"))
        binding.btnMainRecord.setBackgroundResource(R.drawable.bg_record_btn_idle)
        binding.btnMainRecord.setImageResource(R.drawable.ic_neon_mic)
        binding.btnDiscardRecording.visibility = View.INVISIBLE
        binding.btnSaveRecording.visibility = View.INVISIBLE
        binding.waveformVisualizer.reset()
    }

    private fun loadSavedRecordings() {
        val dir = getRecordingsDir()
        val files = dir.listFiles { file ->
            file.isFile && file.length() > 0L && (file.extension.equals("m4a", true) ||
                    file.extension.equals("mp3", true) ||
                    file.extension.equals("wav", true) ||
                    file.extension.equals("aac", true))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        recordings.clear()
        for (file in files) {
            val duration = getAudioFileDuration(file)
            recordings.add(
                RecordingItem(
                    file = file,
                    name = file.name,
                    durationMs = duration,
                    dateMs = file.lastModified(),
                    sizeBytes = file.length()
                )
            )
        }

        adapter.updateList(recordings)
        binding.tvRecordingsCountBadge.text = getString(R.string.recordings_count_badge, recordings.size)

        if (recordings.isEmpty()) {
            binding.layoutEmptyRecordings.visibility = View.VISIBLE
            binding.rvRecordings.visibility = View.GONE
        } else {
            binding.layoutEmptyRecordings.visibility = View.GONE
            binding.rvRecordings.visibility = View.VISIBLE
        }
    }

    private fun handlePlayRecording(item: RecordingItem) {
        val path = item.file.absolutePath

        if (currentlyPlayingFilePath == path) {
            val player = playbackMediaPlayer
            if (player != null) {
                if (player.isPlaying) {
                    player.pause()
                    val pos = player.currentPosition
                    val dur = player.duration
                    adapter.setPlaybackState(path, pos, dur, false)
                    handler.removeCallbacks(playbackProgressRunnable)
                } else {
                    player.start()
                    val pos = player.currentPosition
                    val dur = player.duration
                    adapter.setPlaybackState(path, pos, dur, true)
                    handler.post(playbackProgressRunnable)
                }
            } else {
                startPlaybackForFile(item, startPositionMs = adapter.currentPlayProgressMs)
            }
            return
        }

        stopPlayback(keepOpen = false)
        startPlaybackForFile(item, startPositionMs = 0)
    }

    private fun startPlaybackForFile(item: RecordingItem, startPositionMs: Int) {
        val path = item.file.absolutePath
        currentlyPlayingFilePath = path

        try {
            playbackMediaPlayer = MediaPlayer().apply {
                setDataSource(this@VoiceRecorderActivity, Uri.fromFile(item.file))
                prepare()
                if (startPositionMs in 1 until duration) {
                    seekTo(startPositionMs)
                }
                setOnCompletionListener {
                    adapter.setPlaybackState(
                        filePath = path,
                        progressMs = 0,
                        durationMs = duration,
                        playing = false
                    )
                    stopPlayback(keepOpen = true)
                }
                start()
            }
            val cur = playbackMediaPlayer?.currentPosition ?: startPositionMs
            val dur = playbackMediaPlayer?.duration ?: item.durationMs.toInt()
            adapter.setPlaybackState(path, cur, dur, true)
            handler.post(playbackProgressRunnable)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_cannot_play_recording, e.message ?: ""), Toast.LENGTH_SHORT).show()
            stopPlayback(keepOpen = false)
        }
    }

    private fun stopPlayback(keepOpen: Boolean = false) {
        handler.removeCallbacks(playbackProgressRunnable)
        try {
            playbackMediaPlayer?.stop()
            playbackMediaPlayer?.release()
        } catch (_: Exception) {}
        playbackMediaPlayer = null

        if (!keepOpen) {
            currentlyPlayingFilePath = null
            adapter.clearPlayback()
        } else {
            currentlyPlayingFilePath?.let { path ->
                adapter.setPlaybackState(
                    filePath = path,
                    progressMs = adapter.currentPlayProgressMs,
                    durationMs = adapter.currentPlayDurationMs,
                    playing = false
                )
            }
        }
    }

    private fun confirmDeleteRecording(item: RecordingItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_recording_title))
            .setMessage(getString(R.string.delete_recording_prompt, item.name))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                if (currentlyPlayingFilePath == item.file.absolutePath) {
                    stopPlayback(keepOpen = false)
                }
                item.file.delete()
                loadSavedRecordings()
                Toast.makeText(this, getString(R.string.recording_discarded), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun showRenameDialog(item: RecordingItem) {
        val input = EditText(this).apply {
            setText(item.file.nameWithoutExtension)
            selectAll()
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rename_recording_title))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val ext = item.file.extension
                    val newFile = File(item.file.parentFile, "$newName.$ext")
                    val wasPlaying = currentlyPlayingFilePath == item.file.absolutePath
                    if (item.file.renameTo(newFile)) {
                        if (wasPlaying) {
                            currentlyPlayingFilePath = newFile.absolutePath
                            adapter.updatePlayingFilePath(newFile.absolutePath)
                        }
                        loadSavedRecordings()
                        Toast.makeText(this, getString(R.string.toast_renamed_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, getString(R.string.toast_rename_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun shareRecording(item: RecordingItem) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                item.file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_audio_title)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_share_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRecordingsDir(): File {
        val baseDir = getExternalFilesDir(Environment.DIRECTORY_RECORDINGS) ?: filesDir
        val recordingsDir = File(baseDir, "MusicPlayerRecordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return recordingsDir
    }

    private fun getAudioFileDuration(file: File): Long {
        return try {
            val mmr = android.media.MediaMetadataRetriever()
            mmr.setDataSource(file.absolutePath)
            val durStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            mmr.release()
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun formatRecordTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun checkAudioPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQ_RECORD
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_RECORD) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.mic_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        if (currentState != RecordState.IDLE) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
        }
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val PERMISSION_REQ_RECORD = 1201
    }
}
