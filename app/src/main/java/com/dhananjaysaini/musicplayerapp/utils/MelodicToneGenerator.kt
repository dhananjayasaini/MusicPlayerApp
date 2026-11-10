package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object MelodicToneGenerator {

    data class MelodicPreset(
        val id: String,
        val title: String,
        val tag: String,
        val notes: List<Double>, // frequencies in Hz
        val tempoBpm: Int = 130
    )

    val PRESETS = listOf(
        MelodicPreset("tone_cyber_pulse", "Cyber Pulse", "Electronic", listOf(440.0, 554.37, 659.25, 880.0, 659.25, 554.37, 440.0, 554.37)),
        MelodicPreset("tone_neon_horizon", "Neon Horizon", "Synthwave", listOf(523.25, 659.25, 783.99, 1046.50, 783.99, 659.25, 587.33, 523.25)),
        MelodicPreset("tone_electric_groove", "Electric Groove", "Modern", listOf(392.0, 493.88, 587.33, 783.99, 880.0, 783.99, 587.33, 493.88)),
        MelodicPreset("tone_crystal_bell", "Crystal Bell", "Chime", listOf(659.25, 783.99, 987.77, 1318.51, 987.77, 783.99, 659.25, 783.99)),
        MelodicPreset("tone_cosmic_dream", "Cosmic Dream", "Ambient", listOf(349.23, 440.0, 523.25, 698.46, 523.25, 440.0, 392.0, 349.23)),
        MelodicPreset("tone_sunset_melody", "Sunset Melody", "Melody", listOf(440.0, 493.88, 554.37, 659.25, 739.99, 880.0, 739.99, 659.25)),
        MelodicPreset("tone_techno_beats", "Techno Beats", "Beats", listOf(329.63, 440.0, 493.88, 659.25, 493.88, 440.0, 329.63, 392.0)),
        MelodicPreset("tone_retro_arcade", "Retro Arcade", "8-Bit", listOf(523.25, 587.33, 659.25, 783.99, 880.0, 987.77, 1046.5, 783.99)),
        MelodicPreset("tone_zen_garden", "Zen Garden", "Relax", listOf(392.0, 440.0, 523.25, 587.33, 659.25, 587.33, 523.25, 440.0)),
        MelodicPreset("tone_midnight_vibe", "Midnight Vibe", "Electro", listOf(440.0, 523.25, 659.25, 783.99, 659.25, 523.25, 440.0, 392.0)),
        MelodicPreset("tone_morning_alarm", "Morning Alarm", "Wake Up", listOf(587.33, 739.99, 880.0, 1174.66, 880.0, 739.99, 587.33, 739.99)),
        MelodicPreset("tone_aura_bell", "Aura Bell", "Notification", listOf(783.99, 987.77, 1174.66, 1567.98, 1174.66, 987.77, 783.99, 987.77))
    )

    /**
     * Generates all presets into files/ringtones/ if not already present.
     */
    fun ensurePresetsGenerated(context: Context): List<File> {
        val dir = File(context.filesDir, "ringtones")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val resultFiles = mutableListOf<File>()

        for (preset in PRESETS) {
            val file = File(dir, "${preset.id}.wav")
            if (!file.exists() || file.length() < 1000) {
                generateWavFile(file, preset)
            }
            if (file.exists()) {
                resultFiles.add(file)
            }
        }
        return resultFiles
    }

    private fun generateWavFile(file: File, preset: MelodicPreset) {
        val sampleRate = 44100
        val noteDurationSec = 60.0 / preset.tempoBpm
        val totalNotes = preset.notes.size * 2 // play twice for a smooth ringtone loop
        val totalSamples = (noteDurationSec * totalNotes * sampleRate).toInt()

        val pcmData = ByteArray(totalSamples * 2) // 16-bit mono = 2 bytes per sample
        val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)

        var sampleIndex = 0
        for (repeat in 0 until 2) {
            for (freq in preset.notes) {
                val noteSamples = (noteDurationSec * sampleRate).toInt()
                for (s in 0 until noteSamples) {
                    val t = s.toDouble() / sampleRate
                    // Harmonic overtone synthesis + soft envelope
                    val fundamental = sin(2.0 * Math.PI * freq * t)
                    val overtone = sin(2.0 * Math.PI * freq * 2.0 * t) * 0.35
                    val subHarmonic = sin(2.0 * Math.PI * (freq / 2.0) * t) * 0.15

                    // ADSR envelope: fast attack, smooth decay
                    val progress = s.toDouble() / noteSamples
                    val attack = (progress * 20.0).coerceAtMost(1.0)
                    val decay = (1.0 - progress).coerceIn(0.0, 1.0)
                    val envelope = attack * (decay * decay)

                    val sampleValue = ((fundamental + overtone + subHarmonic) * envelope * 24000.0).toInt().coerceIn(-32767, 32767)
                    if (sampleIndex < totalSamples) {
                        buffer.putShort(sampleValue.toShort())
                        sampleIndex++
                    }
                }
            }
        }

        FileOutputStream(file).use { fos ->
            writeWavHeader(fos, totalSamples * 2, sampleRate)
            fos.write(pcmData)
        }
    }

    private fun writeWavHeader(out: FileOutputStream, pcmSize: Int, sampleRate: Int) {
        val totalDataLen = pcmSize + 36
        val byteRate = sampleRate * 2 // 16-bit mono

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // SubChunk1Size for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat = 1 (PCM)
        header[21] = 0
        header[22] = 1 // NumChannels = 1 (Mono)
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2 // BlockAlign = 2 bytes
        header[33] = 0
        header[34] = 16 // BitsPerSample = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmSize and 0xff).toByte()
        header[41] = ((pcmSize shr 8) and 0xff).toByte()
        header[42] = ((pcmSize shr 16) and 0xff).toByte()
        header[43] = ((pcmSize shr 24) and 0xff).toByte()
        out.write(header, 0, 44)
    }
}
