package com.dhananjaysaini.musicplayerapp.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dhananjaysaini.musicplayerapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object BackgroundPlaybackPermissionHelper {

    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 501
    private const val PREFS_NAME = "bg_playback_prefs"
    private const val KEY_PROMPTED = "bg_playback_prompted"

    fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasPrompted(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_PROMPTED, false)
    }

    fun setPrompted(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_PROMPTED, true).apply()
    }

    /**
     * Checks background playback permission when an audio is clicked.
     * Shows the prominent disclosure dialog on first audio play if permission is not granted.
     *
     * @param activity The host activity.
     * @param onRequestSystemPermission Lambda to trigger the system permission request (e.g. launcher or ActivityCompat).
     * @param onProceed Lambda to proceed with playing the audio.
     */
    fun checkAndRequestOnAudioClick(
        activity: Activity,
        onRequestSystemPermission: (() -> Unit)? = null,
        onProceed: () -> Unit
    ) {
        if (isPermissionGranted(activity)) {
            onProceed()
            return
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.bg_playback_perm_title)
            .setMessage(R.string.bg_playback_perm_msg)
            .setCancelable(false)
            .setPositiveButton(R.string.btn_allow) { dialog, _ ->
                dialog.dismiss()
                setPrompted(activity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (onRequestSystemPermission != null) {
                        onRequestSystemPermission()
                    } else {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                        onProceed()
                    }
                } else {
                    onProceed()
                }
            }
            .setNegativeButton(R.string.btn_later) { dialog, _ ->
                dialog.dismiss()
                setPrompted(activity)
                onProceed()
            }
            .show()
    }

    fun checkAndRequest(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    fun checkAndRequestOnFirstAudioPlay(activity: Activity) {
        checkAndRequest(activity)
    }
}
