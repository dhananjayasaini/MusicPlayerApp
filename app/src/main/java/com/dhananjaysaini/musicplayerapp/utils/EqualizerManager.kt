package com.dhananjaysaini.musicplayerapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.EqualizerLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class EqualizerManager(private val context: Context) {

    private lateinit var binding: EqualizerLayoutBinding

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    fun showEqualizer(audioSessionId: Int) {
        if (audioSessionId <= 0 || !ensureSessionEffects(audioSessionId)) {
            Toast.makeText(
                context,
                context.getString(R.string.toast_audio_session_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialog = BottomSheetDialog(context)
        binding = EqualizerLayoutBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.btnBack.setOnClickListener { dialog.dismiss() }

        val bandControls = listOf(
            binding.seekBand60 to binding.tvBand60,
            binding.seekBand230 to binding.tvBand230,
            binding.seekBand910 to binding.tvBand910,
            binding.seekBand3600 to binding.tvBand3600,
            binding.seekBand14000 to binding.tvBand14000
        )

        setupMasterSwitch()
        setupEqualizerBandsAndPresets(bandControls)
        setupBassBoost()
        setupVirtualizer()
        setupReverb()

        dialog.show()
    }

    private fun setupMasterSwitch() {
        val currentState = safeEffectRead { equalizer?.enabled }
            ?: safeEffectRead { bassBoost?.enabled }
            ?: safeEffectRead { virtualizer?.enabled }
            ?: safeEffectRead { reverb?.enabled }
            ?: isEqEnabled
        isEqEnabled = currentState

        binding.switchEqualizer.isChecked = isEqEnabled
        binding.switchEqualizer.text = getSwitchLabel(isEqEnabled)

        binding.switchEqualizer.setOnCheckedChangeListener { _, isChecked ->
            isEqEnabled = isChecked
            binding.switchEqualizer.text = getSwitchLabel(isChecked)
            safeEffectCall { equalizer?.enabled = isChecked }
            safeEffectCall { bassBoost?.enabled = isChecked }
            safeEffectCall { virtualizer?.enabled = isChecked }
            safeEffectCall { reverb?.enabled = isChecked }
        }
    }

    private fun getSwitchLabel(enabled: Boolean): String =
        context.getString(if (enabled) R.string.status_on else R.string.status_off)

    private fun setupEqualizerBandsAndPresets(bandControls: List<Pair<EQVerticalBar, TextView>>) {
        val eq = equalizer
        if (eq == null) {
            binding.spinnerPreset.isEnabled = false
            binding.spinnerPreset.alpha = DISABLED_ALPHA
            binding.bandContainer.alpha = DISABLED_ALPHA
            bandControls.forEach { (bar, _) ->
                bar.isEnabled = false
                bar.setProgress(0)
            }
            return
        }

        val presetNames = ArrayList<String>()
        safeEffectCall {
            val count = eq.numberOfPresets.toInt()
            for (i in 0 until count) {
                presetNames.add(eq.getPresetName(i.toShort()))
            }
        }

        if (presetNames.isNotEmpty()) {
            val presetAdapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_item, presetNames)
            presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPreset.adapter = presetAdapter
            val initialPos = selectedPresetIndex.coerceIn(0, presetNames.lastIndex)
            binding.spinnerPreset.setSelection(initialPos, false)

            // Ignore initial callback matching initialPos so custom band tweaks persist on reopen.
            var lastPosition = initialPos
            binding.spinnerPreset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position == lastPosition) return
                    lastPosition = position
                    selectedPresetIndex = position
                    safeEffectCall { eq.usePreset(position.toShort()) }
                    bandControls.forEachIndexed { index, (bar, tv) ->
                        updateBandProgress(eq, index, bar, tv)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } else {
            binding.spinnerPreset.isEnabled = false
            binding.spinnerPreset.alpha = DISABLED_ALPHA
        }

        bandControls.forEachIndexed { index, (bar, tv) ->
            setupBand(eq, index, bar, tv)
        }
    }

    private fun setupBassBoost() {
        val bb = bassBoost
        val supported = bb != null && (safeEffectRead { bb.strengthSupported } == true)
        val initialStrength = if (supported) safeEffectRead { bb?.roundedStrength?.toInt() } ?: 0 else 0
        configureStrengthSeekBar(
            supported = supported,
            container = binding.bassBoostLayout,
            seekBar = binding.seekBass,
            initialProgress = initialStrength
        ) { progress ->
            safeEffectCall { bassBoost?.setStrength(progress.toShort()) }
        }
    }

    private fun setupVirtualizer() {
        val virt = virtualizer
        val supported = virt != null && (safeEffectRead { virt.strengthSupported } == true)
        val initialStrength = if (supported) safeEffectRead { virt?.roundedStrength?.toInt() } ?: 0 else 0
        configureStrengthSeekBar(
            supported = supported,
            container = binding.virtualizerLayout,
            seekBar = binding.seekVirtualizer,
            initialProgress = initialStrength
        ) { progress ->
            safeEffectCall { virtualizer?.setStrength(progress.toShort()) }
        }
    }

    private fun configureStrengthSeekBar(
        supported: Boolean,
        container: View,
        seekBar: SeekBar,
        initialProgress: Int,
        onStrengthChanged: (Int) -> Unit
    ) {
        seekBar.max = MAX_EFFECT_STRENGTH
        if (!supported) {
            seekBar.progress = 0
            seekBar.isEnabled = false
            container.alpha = DISABLED_ALPHA
            return
        }

        seekBar.isEnabled = true
        container.alpha = 1.0f
        seekBar.progress = initialProgress.coerceIn(0, MAX_EFFECT_STRENGTH)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    onStrengthChanged(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupReverb() {
        val reverbPresets = listOf(
            context.getString(R.string.reverb_none),
            context.getString(R.string.reverb_small_room),
            context.getString(R.string.reverb_medium_room),
            context.getString(R.string.reverb_large_room),
            context.getString(R.string.reverb_medium_hall),
            context.getString(R.string.reverb_large_hall),
            context.getString(R.string.reverb_plate)
        )

        val reverbAdapter =
            ArrayAdapter(context, android.R.layout.simple_spinner_item, reverbPresets)
        reverbAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerReverb.adapter = reverbAdapter

        val rev = reverb
        if (rev == null) {
            binding.spinnerReverb.isEnabled = false
            binding.reverbLayout.alpha = DISABLED_ALPHA
            return
        }

        binding.spinnerReverb.isEnabled = true
        binding.reverbLayout.alpha = 1.0f
        val initialReverbPos = selectedReverbIndex.coerceIn(0, reverbPresets.lastIndex)
        binding.spinnerReverb.setSelection(initialReverbPos, false)

        var lastReverbPos = initialReverbPos
        binding.spinnerReverb.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == lastReverbPos) return
                lastReverbPos = position
                selectedReverbIndex = position
                val presetValue = when (position) {
                    1 -> PresetReverb.PRESET_SMALLROOM
                    2 -> PresetReverb.PRESET_MEDIUMROOM
                    3 -> PresetReverb.PRESET_LARGEROOM
                    4 -> PresetReverb.PRESET_MEDIUMHALL
                    5 -> PresetReverb.PRESET_LARGEHALL
                    6 -> PresetReverb.PRESET_PLATE
                    else -> PresetReverb.PRESET_NONE
                }
                safeEffectCall { rev.preset = presetValue }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupBand(eq: Equalizer, bandIndex: Int, bar: EQVerticalBar, tv: TextView) {
        val numBands = safeEffectRead { eq.numberOfBands.toInt() } ?: 0
        val range = safeEffectRead { eq.bandLevelRange }
        if (bandIndex >= numBands || range == null || range.size < 2 || range[1] <= range[0]) {
            bar.isEnabled = false
            bar.setProgress(0)
            tv.text = "0"
            return
        }

        val minLevel = range[0]
        val maxLevel = range[1]
        bar.isEnabled = true
        bar.setMax(maxLevel - minLevel)

        val currentLevel = safeEffectRead { eq.getBandLevel(bandIndex.toShort()) } ?: 0
        bar.setProgress(currentLevel - minLevel)
        tv.text = (currentLevel / 100).toString()

        bar.onProgressChanged = { value ->
            val level = (value + minLevel).toShort()
            safeEffectCall { eq.setBandLevel(bandIndex.toShort(), level) }
            tv.text = (level / 100).toString()
        }
    }

    private fun updateBandProgress(
        eq: Equalizer,
        bandIndex: Int,
        bar: EQVerticalBar,
        tv: TextView
    ) {
        val numBands = safeEffectRead { eq.numberOfBands.toInt() } ?: 0
        val range = safeEffectRead { eq.bandLevelRange }
        if (bandIndex >= numBands || range == null || range.size < 2 || range[1] <= range[0]) return

        val minLevel = range[0]
        val currentLevel = safeEffectRead { eq.getBandLevel(bandIndex.toShort()) } ?: return
        bar.setProgress(currentLevel - minLevel)
        tv.text = (currentLevel / 100).toString()
    }

    fun release() {
        releaseShared()
    }

    companion object {
        private const val TAG = "EqualizerManager"
        private const val MAX_EFFECT_STRENGTH = 1000
        private const val DISABLED_ALPHA = 0.4f

        private var attachedSessionId: Int = -1
        private var equalizer: Equalizer? = null
        private var bassBoost: BassBoost? = null
        private var virtualizer: Virtualizer? = null
        private var reverb: PresetReverb? = null

        private var isEqEnabled: Boolean = true
        private var selectedPresetIndex: Int = 0
        private var selectedReverbIndex: Int = 0

        /**
         * Ensures native audio effects are attached to [audioSessionId], reusing valid instances
         * and gracefully skipping any individual effect rejected by the device HAL.
         */
        private fun ensureSessionEffects(audioSessionId: Int): Boolean {
            val sameSession = attachedSessionId == audioSessionId
            val existingAlive = sameSession && hasAliveEffect()
            if (!existingAlive) {
                releaseShared()
                attachedSessionId = audioSessionId
            }

            if (equalizer == null) {
                equalizer = createEffectSafely("Equalizer") {
                    Equalizer(0, audioSessionId).apply { enabled = isEqEnabled }
                }
            }
            if (bassBoost == null) {
                bassBoost = createEffectSafely("BassBoost") {
                    BassBoost(0, audioSessionId).apply { enabled = isEqEnabled }
                }
            }
            if (virtualizer == null) {
                virtualizer = createEffectSafely("Virtualizer") {
                    Virtualizer(0, audioSessionId).apply { enabled = isEqEnabled }
                }
            }
            if (reverb == null) {
                reverb = createEffectSafely("PresetReverb") {
                    PresetReverb(0, audioSessionId).apply { enabled = isEqEnabled }
                }
            }

            val anyInitialized = listOf<AudioEffect?>(equalizer, bassBoost, virtualizer, reverb)
                .any { it != null }
            if (!anyInitialized) {
                attachedSessionId = -1
            }
            return anyInitialized
        }

        private fun hasAliveEffect(): Boolean {
            return listOf<AudioEffect?>(equalizer, bassBoost, virtualizer, reverb)
                .filterNotNull()
                .any { effect -> safeEffectRead { effect.enabled } != null }
        }

        fun releaseShared() {
            listOf<AudioEffect?>(equalizer, bassBoost, virtualizer, reverb).forEach { effect ->
                if (effect != null) {
                    safeEffectCall { effect.enabled = false }
                    safeEffectCall { effect.release() }
                }
            }
            equalizer = null
            bassBoost = null
            virtualizer = null
            reverb = null
            attachedSessionId = -1
        }

        private inline fun <T> createEffectSafely(name: String, block: () -> T): T? {
            return try {
                block()
            } catch (e: Exception) {
                Log.w(TAG, "$name initialization failed: ${e.message}")
                null
            }
        }

        private inline fun safeEffectCall(block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                Log.w(TAG, "AudioEffect operation failed: ${e.message}")
            }
        }

        private inline fun <T> safeEffectRead(block: () -> T): T? {
            return try {
                block()
            } catch (e: Exception) {
                Log.w(TAG, "AudioEffect read failed: ${e.message}")
                null
            }
        }
    }
}
