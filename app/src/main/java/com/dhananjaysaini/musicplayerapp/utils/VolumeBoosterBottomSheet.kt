package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.SeekBar
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.LayoutVolumeBoosterBinding
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.google.android.material.bottomsheet.BottomSheetDialog

class VolumeBoosterBottomSheet(private val context: Context) {

    private val dialog = BottomSheetDialog(context)
    private val binding = LayoutVolumeBoosterBinding.inflate(LayoutInflater.from(context))
    private val handler = Handler(Looper.getMainLooper())

    private var volumeObserver: ContentObserver? = null

    init {
        VolumeBoosterManager.init(context)
        // If MusicService is running and has active session, attach it
        MusicService.mediaPlayer?.audioSessionId?.let { sessionId ->
            if (sessionId > 0) {
                VolumeBoosterManager.attachSession(sessionId)
            }
        }

        dialog.setContentView(binding.root)
        setupViews()
        registerVolumeObserver()
    }

    private fun setupViews() {
        // Close button
        binding.btnCloseBooster.setOnClickListener {
            dialog.dismiss()
        }

        // Enable / Disable switch
        binding.switchBoosterEnable.isChecked = VolumeBoosterManager.isBoostEnabled()
        updateBoostViewsEnabled(binding.switchBoosterEnable.isChecked)

        binding.switchBoosterEnable.setOnCheckedChangeListener { _, isChecked ->
            VolumeBoosterManager.setBoostEnabled(isChecked)
            updateBoostViewsEnabled(isChecked)
            updateBoostText(VolumeBoosterManager.getBoostPercentage())
        }

        // Media volume slider
        val maxVolume = VolumeBoosterManager.getMaxMediaVolume()
        val currentVolume = VolumeBoosterManager.getMediaVolume()
        binding.seekMediaVolume.max = maxVolume
        binding.seekMediaVolume.progress = currentVolume
        updateMediaVolumeText(currentVolume, maxVolume)

        binding.seekMediaVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    VolumeBoosterManager.setMediaVolume(progress)
                    updateMediaVolumeText(progress, maxVolume)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Boost slider
        val currentBoost = VolumeBoosterManager.getBoostPercentage()
        binding.seekBoost.progress = currentBoost
        updateBoostText(currentBoost)
        updatePresetSelection(currentBoost)

        binding.seekBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    VolumeBoosterManager.setBoostPercentage(progress)
                    updateBoostText(progress)
                    updatePresetSelection(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Preset buttons
        binding.btnPreset0.setOnClickListener { setBoostPreset(0) }
        binding.btnPreset30.setOnClickListener { setBoostPreset(30) }
        binding.btnPreset60.setOnClickListener { setBoostPreset(60) }
        binding.btnPreset100.setOnClickListener { setBoostPreset(100) }
        binding.btnPresetMax.setOnClickListener { setBoostPreset(100) } // 100% max hardware boost

        // Quick action: Mute
        binding.btnMute.setOnClickListener {
            VolumeBoosterManager.setMediaVolume(0)
            binding.seekMediaVolume.progress = 0
            updateMediaVolumeText(0, maxVolume)
        }

        // Quick action: Max Loudness
        binding.btnMaxVolume.setOnClickListener {
            VolumeBoosterManager.setMediaVolume(maxVolume)
            binding.seekMediaVolume.progress = maxVolume
            updateMediaVolumeText(maxVolume, maxVolume)

            if (!VolumeBoosterManager.isBoostEnabled()) {
                binding.switchBoosterEnable.isChecked = true
            }
            setBoostPreset(100)
        }

        dialog.setOnDismissListener {
            unregisterVolumeObserver()
        }
    }

    private fun setBoostPreset(percent: Int) {
        if (!VolumeBoosterManager.isBoostEnabled()) {
            binding.switchBoosterEnable.isChecked = true
        }
        binding.seekBoost.progress = percent
        VolumeBoosterManager.setBoostPercentage(percent)
        updateBoostText(percent)
        updatePresetSelection(percent)
    }

    private fun updateMediaVolumeText(current: Int, max: Int) {
        val pct = if (max > 0) (current.toFloat() / max * 100).toInt() else 0
        binding.tvMediaVolumeValue.text = "$pct%"
    }

    private fun updateBoostText(percent: Int) {
        if (!VolumeBoosterManager.isBoostEnabled()) {
            binding.tvBoostValue.text = context.getString(R.string.status_disabled)
            return
        }
        val dbGain = (percent.toFloat() / 100f * 20).toInt()
        if (percent == 0) {
            binding.tvBoostValue.text = context.getString(R.string.boost_normal)
        } else {
            binding.tvBoostValue.text = "+$percent% (+$dbGain dB)"
        }
    }

    private fun updateBoostViewsEnabled(enabled: Boolean) {
        binding.seekBoost.isEnabled = enabled
        binding.seekBoost.alpha = if (enabled) 1.0f else 0.4f
        binding.btnPreset0.isEnabled = enabled
        binding.btnPreset30.isEnabled = enabled
        binding.btnPreset60.isEnabled = enabled
        binding.btnPreset100.isEnabled = enabled
        binding.btnPresetMax.isEnabled = enabled
    }

    private fun updatePresetSelection(percent: Int) {
        binding.btnPreset0.isSelected = percent == 0
        binding.btnPreset30.isSelected = percent in 25..35
        binding.btnPreset60.isSelected = percent in 55..65
        binding.btnPreset100.isSelected = percent in 95..100
        binding.btnPresetMax.isSelected = percent == 100
    }

    private fun registerVolumeObserver() {
        volumeObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val currentVol = VolumeBoosterManager.getMediaVolume()
                val maxVol = VolumeBoosterManager.getMaxMediaVolume()
                binding.seekMediaVolume.progress = currentVol
                updateMediaVolumeText(currentVol, maxVol)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver!!
        )
    }

    private fun unregisterVolumeObserver() {
        volumeObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
            volumeObserver = null
        }
    }

    fun show() {
        dialog.show()
    }
}
