package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

object VolumeBoosterManager {

    private const val TAG = "VolumeBooster"
    private const val PREFS_NAME = "volume_booster_prefs"
    private const val KEY_BOOST_PERCENT = "key_boost_percent"
    private const val KEY_BOOST_ENABLED = "key_boost_enabled"

    // Max gain in millibels: 2000 mB = +20 dB boost
    private const val MAX_GAIN_MB = 2000

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = -1
    private var prefs: SharedPreferences? = null
    private var audioManager: AudioManager? = null

    private var isBoostEnabled: Boolean = true
    private var boostPercentage: Int = 0 // 0 to 100

    fun init(context: Context) {
        val appCtx = context.applicationContext
        audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        isBoostEnabled = prefs?.getBoolean(KEY_BOOST_ENABLED, true) ?: true
        boostPercentage = prefs?.getInt(KEY_BOOST_PERCENT, 0) ?: 0
    }

    /**
     * Attach the LoudnessEnhancer to the active audio session from MediaPlayer.
     */
    fun attachSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && loudnessEnhancer != null) {
            applyBoostSettings()
            return
        }

        try {
            release()
            currentSessionId = audioSessionId
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                applyBoostSettings()
            }
            Log.d(TAG, "Attached LoudnessEnhancer to session: $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach LoudnessEnhancer: ${e.message}")
            loudnessEnhancer = null
        }
    }

    fun isBoostEnabled(): Boolean = isBoostEnabled

    fun setBoostEnabled(enabled: Boolean) {
        isBoostEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_BOOST_ENABLED, enabled)?.apply()
        applyBoostSettings()
    }

    fun getBoostPercentage(): Int = boostPercentage

    fun setBoostPercentage(percent: Int) {
        boostPercentage = percent.coerceIn(0, 100)
        prefs?.edit()?.putInt(KEY_BOOST_PERCENT, boostPercentage)?.apply()
        applyBoostSettings()
    }

    private fun applyBoostSettings() {
        try {
            loudnessEnhancer?.let { enhancer ->
                if (isBoostEnabled && boostPercentage > 0) {
                    val gainmB = ((boostPercentage.toFloat() / 100f) * MAX_GAIN_MB).toInt()
                    enhancer.setTargetGain(gainmB)
                    enhancer.enabled = true
                } else {
                    enhancer.setTargetGain(0)
                    enhancer.enabled = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying boost settings: ${e.message}")
        }
    }

    // Media Volume Controls
    fun getMediaVolume(): Int {
        return audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    }

    fun getMaxMediaVolume(): Int {
        return audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }

    fun setMediaVolume(volume: Int) {
        val max = getMaxMediaVolume()
        val safeVol = volume.coerceIn(0, max)
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, safeVol, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting stream volume: ${e.message}")
        }
    }

    fun release() {
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing LoudnessEnhancer: ${e.message}")
        } finally {
            loudnessEnhancer = null
            currentSessionId = -1
        }
    }
}
