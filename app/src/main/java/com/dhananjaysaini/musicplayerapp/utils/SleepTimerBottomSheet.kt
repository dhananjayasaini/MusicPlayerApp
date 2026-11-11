package com.dhananjaysaini.musicplayerapp.utils

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetSleepTimerBinding
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SleepTimerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSleepTimerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSleepTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPickers()
        setupClicks()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            bottomSheet.setBackgroundResource(android.R.color.transparent)
        }
    }

    private fun setupPickers() {
        setupNumberPicker(binding.npHours, 0, 23)
        setupNumberPicker(binding.npMinutes, 0, 59)
        setupNumberPicker(binding.npSeconds, 0, 59)

        // Load active remaining time if a timer is already running
        val remaining = MusicService.musicService?.remainingTime ?: 0L
        if (remaining > 0L) {
            val totalSec = remaining / 1000L
            val hrs = (totalSec / 3600).toInt()
            val mins = ((totalSec % 3600) / 60).toInt()
            val secs = (totalSec % 60).toInt()
            setTime(hrs, mins, secs)
        } else {
            setTime(0, 0, 0)
        }
    }

    private fun setupNumberPicker(picker: NumberPicker, min: Int, max: Int) {
        picker.minValue = min
        picker.maxValue = max
        picker.wrapSelectorWheel = true
        picker.displayedValues = (min..max).map { String.format("%02d", it) }.toTypedArray()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            picker.textColor = Color.WHITE
            picker.textSize = 28f * resources.displayMetrics.scaledDensity
            picker.selectionDividerHeight = 0
        }

        // Apply white text color & bold styling to child edit text
        for (i in 0 until picker.childCount) {
            val child = picker.getChildAt(i)
            if (child is EditText) {
                child.setTextColor(Color.WHITE)
                child.textSize = 26f
                child.typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun setupClicks() {
        binding.btn15.setOnClickListener { setTime(0, 15, 0) }
        binding.btn30.setOnClickListener { setTime(0, 30, 0) }
        binding.btn45.setOnClickListener { setTime(0, 45, 0) }
        binding.btn60.setOnClickListener { setTime(1, 0, 0) }

        // Stop when current song ends
        binding.btnSongEnd.setOnClickListener {
            val player = MusicService.mediaPlayer
            if (player != null && (MusicService.playlist.isNotEmpty())) {
                val currentPos = player.currentPosition
                val duration = player.duration
                val remaining = (duration - currentPos).coerceAtLeast(0)
                if (remaining > 0) {
                    val totalSec = remaining / 1000
                    val hrs = (totalSec / 3600).toInt()
                    val mins = ((totalSec % 3600) / 60).toInt()
                    val secs = (totalSec % 60).toInt()
                    setTime(hrs, mins, secs)
                    Toast.makeText(
                        requireContext(),
                        "Set to end of current track (${String.format("%02d:%02d", mins, secs)})",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Song is near end", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Play a song first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            MusicService.musicService?.cancelSleepTimer()
            Toast.makeText(requireContext(), "Sleep Timer cancelled", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.btnStart.setOnClickListener {
            val totalMillis = getTotalMillis()

            if (totalMillis <= 0) {
                Toast.makeText(requireContext(), "Please select time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MusicService.musicService?.startSleepTimer(totalMillis)
            Toast.makeText(
                requireContext(),
                "Sleep Timer set for ${formatTotalTime(totalMillis)}",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }
    }

    private fun setTime(hours: Int, minutes: Int, seconds: Int) {
        binding.npHours.value = hours
        binding.npMinutes.value = minutes
        binding.npSeconds.value = seconds
    }

    private fun getTotalMillis(): Long {
        val hrs = binding.npHours.value
        val min = binding.npMinutes.value
        val sec = binding.npSeconds.value
        return ((hrs * 3600L) + (min * 60L) + sec) * 1000L
    }

    private fun formatTotalTime(millis: Long): String {
        val totalSec = millis / 1000
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return when {
            hrs > 0 -> "${hrs}h ${mins}m"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
