package com.dhananjaysaini.musicplayerapp.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentMiniPlayerBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import kotlin.math.abs

class MiniPlayerFragment : Fragment(R.layout.fragment_mini_player) {

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var gestureDetector: GestureDetector

    companion object {
        private const val SWIPE_THRESHOLD = 80
        private const val SWIPE_VELOCITY_THRESHOLD = 80
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMiniPlayerBinding.bind(view)

        setupGestures()
        setupClicks()
        observeMusicService()
        updateMiniPlayerFromService()
    }

    override fun onResume() {
        super.onResume()
        updateMiniPlayerFromService()
    }

    private fun observeMusicService() {
        MusicService.currentSongLiveData.observe(viewLifecycleOwner) { song ->
            if (song != null) {
                updateMiniPlayerView(song, MusicService.mediaPlayer?.isPlaying == true)
            }
        }
        MusicService.isPlayingLiveData.observe(viewLifecycleOwner) { isPlaying ->
            binding.miniPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val listener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                openPlayerActivity()
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                return when {
                    // Swipe UP -> Open Full Player
                    diffY < -SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD -> {
                        openPlayerActivity()
                        true
                    }
                    // Swipe LEFT -> Next Song
                    diffX < -SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD -> {
                        playNextSong()
                        true
                    }
                    // Swipe RIGHT -> Previous Song
                    diffX > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD -> {
                        playPrevSong()
                        true
                    }
                    else -> false
                }
            }
        }

        gestureDetector = GestureDetector(requireContext(), listener)

        binding.root.setOnTouchListener { _, event ->
            // Let control buttons handle their own touches
            val controlsRect = Rect()
            binding.miniControlsLayout.getGlobalVisibleRect(controlsRect)
            if (controlsRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                return@setOnTouchListener false
            }
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.miniInfoLayout.setOnClickListener { openPlayerActivity() }
        binding.miniArtCard.setOnClickListener { openPlayerActivity() }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction("UPDATE_UI")
            addAction("MUSIC_PLAYER_UI_UPDATE")
        }
        ContextCompat.registerReceiver(
            requireContext(),
            updateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        requireContext().sendBroadcast(
            Intent("REQUEST_UI_UPDATE").setPackage(requireContext().packageName)
        )
    }

    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(updateReceiver)
        } catch (_: Exception) { }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (_binding == null) return
            val currentSong = MusicService.playlist.getOrNull(MusicService.position)
            val isPlaying = intent?.getBooleanExtra("isPlaying", MusicService.mediaPlayer?.isPlaying == true)
                ?: (MusicService.mediaPlayer?.isPlaying == true)

            val title = intent?.getStringExtra("title") ?: currentSong?.title
            val artist = intent?.getStringExtra("artist") ?: currentSong?.artist
            val artUri = intent?.getStringExtra("artUri") ?: currentSong?.artUri

            if (!title.isNullOrEmpty()) {
                binding.miniTitle.text = title
            }
            if (!artist.isNullOrEmpty()) {
                binding.miniArtist.text = artist
            }

            binding.miniPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause
                else R.drawable.ic_play
            )

            if (!artUri.isNullOrEmpty() && artUri != "null") {
                Glide.with(this@MiniPlayerFragment)
                    .load(artUri)
                    .placeholder(R.drawable.ic_music)
                    .error(R.drawable.ic_music)
                    .into(binding.miniArt)
            } else {
                binding.miniArt.setImageResource(R.drawable.ic_music)
            }
        }
    }

    private fun updateMiniPlayerView(song: Music?, isPlaying: Boolean) {
        if (_binding == null || song == null) return
        binding.miniTitle.text = song.title.ifEmpty { "Song Title" }
        binding.miniArtist.text = song.artist.ifEmpty { "Artist" }
        binding.miniPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        val art = song.artUri
        if (!art.isNullOrEmpty() && art != "null") {
            Glide.with(this)
                .load(art)
                .placeholder(R.drawable.ic_music)
                .error(R.drawable.ic_music)
                .into(binding.miniArt)
        } else {
            binding.miniArt.setImageResource(R.drawable.ic_music)
        }
    }

    private fun updateMiniPlayerFromService() {
        if (_binding == null) return
        val currentSong = MusicService.playlist.getOrNull(MusicService.position)
        val isPlaying = MusicService.mediaPlayer?.isPlaying == true
        if (currentSong != null) {
            updateMiniPlayerView(currentSong, isPlaying)
        } else {
            binding.miniPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }

    private fun setupClicks() {
        binding.miniPlayPause.setOnClickListener {
            val isPlaying = MusicService.mediaPlayer?.isPlaying == true
            val willPlay = !isPlaying
            binding.miniPlayPause.setImageResource(
                if (willPlay) R.drawable.ic_pause else R.drawable.ic_play
            )
            val action = if (isPlaying) Constants.ACTION_PAUSE else Constants.ACTION_PLAY
            sendServiceAction(action)
        }

        binding.miniNext.setOnClickListener {
            playNextSong()
        }

        binding.miniPrev.setOnClickListener {
            playPrevSong()
        }
    }

    private fun playNextSong() {
        sendServiceAction(Constants.ACTION_NEXT)
        animateSwipeTransition(isNext = true)
    }

    private fun playPrevSong() {
        sendServiceAction(Constants.ACTION_PREVIOUS)
        animateSwipeTransition(isNext = false)
    }

    private fun sendServiceAction(action: String) {
        requireContext().startService(
            Intent(requireContext(), MusicService::class.java).setAction(action)
        )
    }

    private fun animateSwipeTransition(isNext: Boolean) {
        val translationDistance = if (isNext) -40f else 40f
        binding.miniInfoLayout.animate()
            .translationX(translationDistance)
            .alpha(0.5f)
            .setDuration(120)
            .withEndAction {
                binding.miniInfoLayout.translationX = -translationDistance
                binding.miniInfoLayout.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun openPlayerActivity() {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("class", "MiniPlayer")
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
