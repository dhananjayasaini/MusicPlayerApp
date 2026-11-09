package com.dhananjaysaini.musicplayerapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.MainActivity
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                sendServiceAction(context, Constants.ACTION_TOGGLE_PLAY)
            }
            ACTION_WIDGET_PREV -> {
                sendServiceAction(context, Constants.ACTION_PREVIOUS)
            }
            ACTION_WIDGET_NEXT -> {
                sendServiceAction(context, Constants.ACTION_NEXT)
            }
            ACTION_WIDGET_UPDATE,
            "UPDATE_UI",
            "MUSIC_PLAYER_UI_UPDATE",
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                updateAllWidgets(context)
            }
        }
    }

    private fun sendServiceAction(context: Context, action: String) {
        if (MusicService.playlist.isEmpty()) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(launchIntent)
            return
        }

        val serviceIntent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            try {
                context.startService(serviceIntent)
            } catch (ex: Exception) {
                Log.e("MusicAppWidget", "Failed to send service action $action: ${ex.message}")
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.dhananjaysaini.musicplayerapp.widget.ACTION_PLAY_PAUSE"
        const val ACTION_WIDGET_PREV = "com.dhananjaysaini.musicplayerapp.widget.ACTION_PREV"
        const val ACTION_WIDGET_NEXT = "com.dhananjaysaini.musicplayerapp.widget.ACTION_NEXT"
        const val ACTION_WIDGET_UPDATE = "com.dhananjaysaini.musicplayerapp.widget.ACTION_UPDATE"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, MusicAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicAppWidget", "Error updating all widgets: ${e.message}")
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val prefs = context.getSharedPreferences("widget_playback_state", Context.MODE_PRIVATE)
                val isPlaying = MusicService.mediaPlayer?.isPlaying == true

                val currentSong = if (MusicService.playlist.isNotEmpty() && MusicService.position in MusicService.playlist.indices) {
                    MusicService.playlist[MusicService.position]
                } else null

                val title = currentSong?.title ?: prefs.getString("title", null) ?: context.getString(R.string.widget_no_song)
                val artist = currentSong?.artist ?: prefs.getString("artist", null) ?: context.getString(R.string.widget_tap_to_play)
                val artUri = currentSong?.artUri ?: prefs.getString("artUri", null)

                val views = RemoteViews(context.packageName, R.layout.widget_music_player)
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_artist, artist)

                val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)
                views.setContentDescription(
                    R.id.widget_btn_play_pause,
                    if (isPlaying) "Pause" else "Play"
                )

                // Previous
                val prevIntent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_PREV
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_prev,
                    PendingIntent.getBroadcast(
                        context,
                        101,
                        prevIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )

                // Play / Pause
                val playPauseIntent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_PLAY_PAUSE
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_play_pause,
                    PendingIntent.getBroadcast(
                        context,
                        102,
                        playPauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )

                // Next
                val nextIntent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_NEXT
                }
                views.setOnClickPendingIntent(
                    R.id.widget_btn_next,
                    PendingIntent.getBroadcast(
                        context,
                        103,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )

                // Click to open App
                val openIntent = if (MusicService.playlist.isNotEmpty()) {
                    Intent(context, PlayerActivity::class.java).apply {
                        putExtra("class", "Widget")
                        putExtra("index", MusicService.position)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } else {
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
                val openPI = PendingIntent.getActivity(
                    context,
                    100,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_art, openPI)
                views.setOnClickPendingIntent(R.id.widget_info_layout, openPI)
                views.setOnClickPendingIntent(R.id.widget_root, openPI)

                val hasArt = !artUri.isNullOrEmpty() && artUri != "null"
                if (!hasArt) {
                    views.setImageViewResource(R.id.widget_art, R.drawable.music_player_icon_splash_screen)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } else {
                    views.setImageViewResource(R.id.widget_art, R.drawable.music_player_icon_splash_screen)
                    appWidgetManager.updateAppWidget(appWidgetId, views)

                    CoroutineScope(Dispatchers.IO).launch {
                        val bitmap = loadRoundedBitmap(context, artUri)
                        withContext(Dispatchers.Main) {
                            if (bitmap != null) {
                                views.setImageViewBitmap(R.id.widget_art, bitmap)
                            } else {
                                views.setImageViewResource(R.id.widget_art, R.drawable.music_player_icon_splash_screen)
                            }
                            appWidgetManager.updateAppWidget(appWidgetId, views)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicAppWidget", "Error updating widget $appWidgetId: ${e.message}")
            }
        }

        private fun loadRoundedBitmap(context: Context, uriString: String?): Bitmap? {
            if (uriString.isNullOrEmpty() || uriString == "null") return null
            return try {
                val uri = Uri.parse(uriString)
                val resolver = context.contentResolver
                val inputStream = resolver.openInputStream(uri) ?: return null

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                val targetSize = 180
                var inSampleSize = 1
                if (options.outHeight > targetSize || options.outWidth > targetSize) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / inSampleSize) >= targetSize && (halfWidth / inSampleSize) >= targetSize) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val stream2 = resolver.openInputStream(uri) ?: return null
                val decoded = BitmapFactory.decodeStream(stream2, null, decodeOptions)
                stream2.close()

                decoded?.let { bmp ->
                    val scaled = Bitmap.createScaledBitmap(bmp, targetSize, targetSize, true)
                    getRoundedCornerBitmap(scaled, 28f)
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val rectF = RectF(rect)
            canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }
    }
}
