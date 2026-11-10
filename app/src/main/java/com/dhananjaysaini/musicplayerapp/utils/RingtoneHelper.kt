package com.dhananjaysaini.musicplayerapp.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream

object RingtoneHelper {

    private const val TAG = "RingtoneHelper"

    fun canModifySettings(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    fun openSettingsPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        }
    }

    fun applyRingtone(
        context: Context,
        file: File,
        title: String,
        type: Int
    ): Boolean {
        if (!canModifySettings(context)) {
            Log.e(TAG, "Cannot modify system settings, permission missing")
            return false
        }

        try {
            val contentUri = insertAudioToMediaStore(context, file, title, type)
            if (contentUri != null) {
                RingtoneManager.setActualDefaultRingtoneUri(context, type, contentUri)
                vibrateSuccess(context)
                val typeName = when (type) {
                    RingtoneManager.TYPE_NOTIFICATION -> "Notification sound"
                    RingtoneManager.TYPE_ALARM -> "Alarm tone"
                    else -> "Phone ringtone"
                }
                Toast.makeText(context, "$typeName set successfully! 🎵", Toast.LENGTH_SHORT).show()
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply ringtone: ${e.message}")
            Toast.makeText(context, "Could not set ringtone: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    private fun insertAudioToMediaStore(
        context: Context,
        file: File,
        title: String,
        type: Int
    ): Uri? {
        val resolver = context.contentResolver

        val mimeType = when (file.extension.lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            else -> "audio/mpeg"
        }

        val relativeDir = when (type) {
            RingtoneManager.TYPE_NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
            RingtoneManager.TYPE_ALARM -> Environment.DIRECTORY_ALARMS
            else -> Environment.DIRECTORY_RINGTONES
        }

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.DISPLAY_NAME, "${title}_${System.currentTimeMillis()}.${file.extension}")
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.IS_RINGTONE, if (type == RingtoneManager.TYPE_RINGTONE) 1 else 0)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, if (type == RingtoneManager.TYPE_NOTIFICATION) 1 else 0)
            put(MediaStore.Audio.Media.IS_ALARM, if (type == RingtoneManager.TYPE_ALARM) 1 else 0)
            put(MediaStore.Audio.Media.IS_MUSIC, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativeDir)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(file).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Error copying tone to media store: ${e.message}")
            try {
                resolver.delete(uri, null, null)
            } catch (_: Exception) {}
            return null
        }
    }

    private fun vibrateSuccess(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }
}
