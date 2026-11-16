package com.dhananjaysaini.musicplayerapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
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
import java.io.File

open class MusicAppWidgetProvider : AppWidgetProvider() {

    open val layoutResId: Int = R.layout.widget_style_standard
    open val widgetStyleId: String = "standard"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, layoutResId)
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

        val ALL_PROVIDER_CLASSES = listOf(
            MusicAppWidgetProvider::class.java,
            MiniWidgetProvider::class.java,
            StandardWidgetProvider::class.java,
            CardWidgetProvider::class.java,
            PracticalWidgetProvider::class.java,
            StylishWidgetProvider::class.java,
            ClassicWidgetProvider::class.java,
            LiteWidgetProvider::class.java,
            VinylWidgetProvider::class.java,
            SimpleWidgetProvider::class.java,
            CircularWidgetProvider::class.java
        )

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                for (cls in ALL_PROVIDER_CLASSES) {
                    val componentName = ComponentName(context, cls)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                        val layoutRes = getLayoutForProviderClass(cls)
                        for (appWidgetId in appWidgetIds) {
                            updateAppWidget(context, appWidgetManager, appWidgetId, layoutRes)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicAppWidget", "Error updating all widgets: ${e.message}")
            }
        }

        fun getLayoutForProviderClass(cls: Class<*>): Int {
            return when (cls) {
                MiniWidgetProvider::class.java -> R.layout.widget_style_mini
                StandardWidgetProvider::class.java -> R.layout.widget_style_standard
                CardWidgetProvider::class.java -> R.layout.widget_style_card
                PracticalWidgetProvider::class.java -> R.layout.widget_style_practical
                StylishWidgetProvider::class.java -> R.layout.widget_style_stylish
                ClassicWidgetProvider::class.java -> R.layout.widget_style_classic
                LiteWidgetProvider::class.java -> R.layout.widget_style_lite
                VinylWidgetProvider::class.java -> R.layout.widget_style_vinyl
                SimpleWidgetProvider::class.java -> R.layout.widget_style_simple
                CircularWidgetProvider::class.java -> R.layout.widget_style_circular
                else -> R.layout.widget_style_standard
            }
        }

        fun buildRemoteViews(
            context: Context,
            layoutRes: Int,
            forPreview: Boolean = false
        ): RemoteViews {
            val prefs = context.getSharedPreferences("widget_playback_state", Context.MODE_PRIVATE)
            val isPlaying = MusicService.mediaPlayer?.isPlaying == true

            val currentSong = if (MusicService.playlist.isNotEmpty() && MusicService.position in MusicService.playlist.indices) {
                MusicService.playlist[MusicService.position]
            } else null

            val defaultTitle = "Love Story"
            val defaultArtist = "Taylor Swift"

            val title = if (forPreview) {
                currentSong?.title ?: defaultTitle
            } else {
                currentSong?.title ?: prefs.getString("title", null) ?: context.getString(R.string.widget_no_song)
            }

            val artist = if (forPreview) {
                currentSong?.artist ?: defaultArtist
            } else {
                currentSong?.artist ?: prefs.getString("artist", null) ?: context.getString(R.string.widget_tap_to_play)
            }

            val views = RemoteViews(context.packageName, layoutRes)
            views.setTextViewText(R.id.widget_title, title)
            try {
                views.setTextViewText(R.id.widget_artist, artist)
            } catch (_: Exception) {}

            val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            try {
                views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)
            } catch (_: Exception) {}

            // Previous
            val prevIntent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PREV
            }
            try {
                views.setOnClickPendingIntent(
                    R.id.widget_btn_prev,
                    PendingIntent.getBroadcast(
                        context,
                        101,
                        prevIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } catch (_: Exception) {}

            // Play / Pause
            val playPauseIntent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PLAY_PAUSE
            }
            try {
                views.setOnClickPendingIntent(
                    R.id.widget_btn_play_pause,
                    PendingIntent.getBroadcast(
                        context,
                        102,
                        playPauseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } catch (_: Exception) {}

            // Next
            val nextIntent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_NEXT
            }
            try {
                views.setOnClickPendingIntent(
                    R.id.widget_btn_next,
                    PendingIntent.getBroadcast(
                        context,
                        103,
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } catch (_: Exception) {}

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
            try {
                views.setOnClickPendingIntent(R.id.widget_root, openPI)
            } catch (_: Exception) {}
            try {
                views.setOnClickPendingIntent(R.id.widget_art, openPI)
            } catch (_: Exception) {}
            try {
                views.setOnClickPendingIntent(R.id.widget_btn_playlist, openPI)
            } catch (_: Exception) {}

            val currentSongForArt = if (MusicService.playlist.isNotEmpty() && MusicService.position in MusicService.playlist.indices) {
                MusicService.playlist[MusicService.position]
            } else null
            val artUri = currentSongForArt?.artUri ?: prefs.getString("artUri", null)
            val path = currentSongForArt?.path ?: prefs.getString("path", null)
            val songId = currentSongForArt?.id ?: prefs.getString("songId", null)

            applyArtwork(context, views, layoutRes, artUri, path, forPreview, songId)

            return views
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            layoutRes: Int = R.layout.widget_style_standard
        ) {
            try {
                val views = buildRemoteViews(context, layoutRes, forPreview = false)
                appWidgetManager.updateAppWidget(appWidgetId, views)

                val currentSong = if (MusicService.playlist.isNotEmpty() && MusicService.position in MusicService.playlist.indices) {
                    MusicService.playlist[MusicService.position]
                } else null
                val prefs = context.getSharedPreferences("widget_playback_state", Context.MODE_PRIVATE)
                val artUri = currentSong?.artUri ?: prefs.getString("artUri", null)
                val path = currentSong?.path ?: prefs.getString("path", null)
                val songId = currentSong?.id ?: prefs.getString("songId", null)

                val hasArt = (!artUri.isNullOrEmpty() && artUri != "null") || (!path.isNullOrEmpty() && File(path).exists())
                if (hasArt) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val updated = loadAndApplyArtworkAsync(context, views, layoutRes, artUri, path, songId)
                        if (updated) {
                            withContext(Dispatchers.Main) {
                                try {
                                    appWidgetManager.updateAppWidget(appWidgetId, views)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicAppWidget", "Error updating widget $appWidgetId: ${e.message}")
            }
        }

        private fun applyArtwork(
            context: Context,
            views: RemoteViews,
            layoutRes: Int,
            artUri: String?,
            path: String?,
            forPreview: Boolean,
            songId: String? = null
        ) {
            try {
                when (layoutRes) {
                    R.layout.widget_style_circular -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 160, songId)
                            ?: createDefaultArtworkBitmap("circular", 160)
                        views.setImageViewBitmap(R.id.widget_art, getCircularBitmap(baseBitmap))
                    }
                    R.layout.widget_style_stylish -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 220, songId)
                            ?: createDefaultArtworkBitmap("stylish", 220)
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 28f))
                    }
                    R.layout.widget_style_classic -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 140, songId)
                            ?: createDefaultArtworkBitmap("classic", 140)
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 18f))
                    }
                    R.layout.widget_style_mini -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 150, songId)
                            ?: createDefaultArtworkBitmap("mini", 150)
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 22f))
                    }
                    R.layout.widget_style_standard -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 160, songId)
                            ?: createDefaultArtworkBitmap("standard", 160)
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 24f))
                    }
                    R.layout.widget_style_card -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 160, songId)
                            ?: createDefaultArtworkBitmap("card", 160)
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 24f))
                    }
                    R.layout.widget_style_lite -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 140, songId)
                            ?: createDefaultArtworkBitmap("lite", 140)
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 18f))
                    }
                    R.layout.widget_style_simple -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 150, songId)
                            ?: createDefaultArtworkBitmap("simple", 150)
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 20f))
                    }
                    R.layout.widget_style_practical -> {
                        val centerArt = loadArtworkBitmap(context, artUri, path, 140, songId)
                            ?: createDefaultArtworkBitmap("practical", 140)
                        val vinylBmp = getVinylBitmapWithCenterArt(context, centerArt, 200)
                        views.setImageViewBitmap(R.id.widget_art, vinylBmp)
                    }
                    R.layout.widget_style_vinyl -> {
                        val centerArt = loadArtworkBitmap(context, artUri, path, 120, songId)
                            ?: createDefaultArtworkBitmap("vinyl", 120)
                        val vinylBmp = getVinylBitmapWithCenterArt(context, centerArt, 180)
                        views.setImageViewBitmap(R.id.widget_art, vinylBmp)
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicAppWidget", "applyArtwork error: ${e.message}")
            }
        }

        private fun loadAndApplyArtworkAsync(
            context: Context,
            views: RemoteViews,
            layoutRes: Int,
            artUri: String?,
            path: String?,
            songId: String? = null
        ): Boolean {
            return try {
                when (layoutRes) {
                    R.layout.widget_style_circular -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 160, songId) ?: return false
                        views.setImageViewBitmap(R.id.widget_art, getCircularBitmap(baseBitmap))
                        true
                    }
                    R.layout.widget_style_stylish -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 220, songId) ?: return false
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 28f))
                        true
                    }
                    R.layout.widget_style_classic -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 140, songId) ?: return false
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 18f))
                        true
                    }
                    R.layout.widget_style_mini -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 150, songId) ?: return false
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 22f))
                        true
                    }
                    R.layout.widget_style_standard -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 160, songId) ?: return false
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 24f))
                        true
                    }
                    R.layout.widget_style_card -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 160, songId) ?: return false
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 24f))
                        true
                    }
                    R.layout.widget_style_lite -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 140, songId) ?: return false
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 18f))
                        true
                    }
                    R.layout.widget_style_simple -> {
                        val baseBitmap = loadArtworkBitmap(context, artUri, path, 150, songId) ?: return false
                        views.setImageViewBitmap(R.id.widget_art, getRoundedCornerBitmap(baseBitmap, 20f))
                        true
                    }
                    R.layout.widget_style_practical -> {
                        val centerArt = loadArtworkBitmap(context, artUri, path, 140, songId) ?: return false
                        val vinylBmp = getVinylBitmapWithCenterArt(context, centerArt, 200)
                        views.setImageViewBitmap(R.id.widget_art, vinylBmp)
                        true
                    }
                    R.layout.widget_style_vinyl -> {
                        val centerArt = loadArtworkBitmap(context, artUri, path, 120, songId) ?: return false
                        val vinylBmp = getVinylBitmapWithCenterArt(context, centerArt, 180)
                        views.setImageViewBitmap(R.id.widget_art, vinylBmp)
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
        }

        internal fun loadArtworkBitmap(
            context: Context,
            uriString: String?,
            path: String?,
            targetSize: Int,
            songId: String? = null
        ): Bitmap? {
            var bmp: Bitmap? = null

            // 1. Try MediaStore song thumbnail on API 29+
            if (!songId.isNullOrEmpty()) {
                val sId = songId.toLongOrNull()
                if (sId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, sId)
                        bmp = context.contentResolver.loadThumbnail(songUri, Size(targetSize, targetSize), null)
                    } catch (_: Exception) {}
                }
            }

            // 2. If uriString has albumart ID, try ContentUris with MediaStore.Audio.Albums on API 29+
            if (bmp == null && !uriString.isNullOrEmpty() && uriString != "null") {
                val albumId = uriString.substringAfterLast("/").toLongOrNull()
                if (albumId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
                        bmp = context.contentResolver.loadThumbnail(albumUri, Size(targetSize, targetSize), null)
                    } catch (_: Exception) {}
                }
            }

            // 3. Try raw uriString via ContentResolver (openInputStream or loadThumbnail)
            if (bmp == null && !uriString.isNullOrEmpty() && uriString != "null") {
                bmp = loadScaledBitmapFromUri(context, uriString, targetSize)
            }

            // 4. Try embedded picture from file path via MediaMetadataRetriever
            if (bmp == null && !path.isNullOrEmpty()) {
                bmp = loadScaledBitmapFromPath(path, targetSize)
            }

            // 5. Query MediaStore by path to resolve album ID and load thumbnail
            if (bmp == null && !path.isNullOrEmpty()) {
                try {
                    val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
                    val selection = "${MediaStore.Audio.Media.DATA} = ?"
                    val cursor = context.contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        arrayOf(path),
                        null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val albumId = it.getLong(0)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
                                bmp = context.contentResolver.loadThumbnail(albumUri, Size(targetSize, targetSize), null)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            return bmp
        }

        private fun loadScaledBitmapFromPath(path: String, targetSize: Int): Bitmap? {
            return try {
                val file = File(path)
                if (!file.exists()) return null
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(path)
                val artBytes = retriever.embeddedPicture
                retriever.release()
                if (artBytes != null && artBytes.isNotEmpty()) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, options)
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
                    val decoded = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, decodeOptions)
                    decoded?.let { Bitmap.createScaledBitmap(it, targetSize, targetSize, true) }
                } else null
            } catch (e: Exception) {
                null
            }
        }

        private fun loadScaledBitmapFromUri(context: Context, uriString: String?, targetSize: Int): Bitmap? {
            if (uriString.isNullOrEmpty() || uriString == "null") return null
            return try {
                val uri = Uri.parse(uriString)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val thumb = context.contentResolver.loadThumbnail(uri, Size(targetSize, targetSize), null)
                        if (thumb != null) {
                            return Bitmap.createScaledBitmap(thumb, targetSize, targetSize, true)
                        }
                    } catch (_: Exception) {}
                }

                val resolver = context.contentResolver
                val inputStream = resolver.openInputStream(uri) ?: return null

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

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
                    Bitmap.createScaledBitmap(bmp, targetSize, targetSize, true)
                }
            } catch (e: Exception) {
                null
            }
        }

        internal fun createDefaultArtworkBitmap(styleId: String, targetSize: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val (startColor, endColor, accentColor) = when (styleId) {
                "standard" -> Triple(Color.parseColor("#3D1B2A"), Color.parseColor("#1D0B14"), Color.parseColor("#FF7043"))
                "card" -> Triple(Color.parseColor("#381E11"), Color.parseColor("#1A0E08"), Color.parseColor("#FFA726"))
                "stylish" -> Triple(Color.parseColor("#1A1235"), Color.parseColor("#0E0920"), Color.parseColor("#FF8A65"))
                "lite" -> Triple(Color.parseColor("#0D2538"), Color.parseColor("#06131D"), Color.parseColor("#4DD0E1"))
                "practical" -> Triple(Color.parseColor("#151D28"), Color.parseColor("#0B1017"), Color.parseColor("#FF7043"))
                "mini" -> Triple(Color.parseColor("#2B0927"), Color.parseColor("#140412"), Color.parseColor("#BA68C8"))
                "classic" -> Triple(Color.parseColor("#0D2F28"), Color.parseColor("#051613"), Color.parseColor("#4DB6AC"))
                "circular" -> Triple(Color.parseColor("#0B2830"), Color.parseColor("#041317"), Color.parseColor("#26A69A"))
                "simple" -> Triple(Color.parseColor("#2A1208"), Color.parseColor("#120703"), Color.parseColor("#FF9800"))
                "vinyl" -> Triple(Color.parseColor("#2D1B0D"), Color.parseColor("#150B04"), Color.parseColor("#FFA726"))
                else -> Triple(Color.parseColor("#1E1B4B"), Color.parseColor("#0F0C29"), Color.parseColor("#818CF8"))
            }

            // Rich background linear gradient
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, targetSize.toFloat(), targetSize.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, targetSize.toFloat(), targetSize.toFloat(), bgPaint)

            // Radiant sun/moon circle
            val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                alpha = 230
            }
            val sunRadius = targetSize * 0.22f
            canvas.drawCircle(targetSize * 0.5f, targetSize * 0.40f, sunRadius, sunPaint)

            // Ambient glow around sun
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                alpha = 60
            }
            canvas.drawCircle(targetSize * 0.5f, targetSize * 0.40f, sunRadius * 1.4f, glowPaint)

            // Landscape / wave hills in foreground
            val hillPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#25FFFFFF")
            }
            val path1 = Path().apply {
                moveTo(0f, targetSize.toFloat())
                lineTo(0f, targetSize * 0.65f)
                quadTo(targetSize * 0.35f, targetSize * 0.52f, targetSize * 0.70f, targetSize * 0.66f)
                lineTo(targetSize.toFloat(), targetSize * 0.60f)
                lineTo(targetSize.toFloat(), targetSize.toFloat())
                close()
            }
            canvas.drawPath(path1, hillPaint1)

            val hillPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#40000000")
            }
            val path2 = Path().apply {
                moveTo(0f, targetSize.toFloat())
                lineTo(0f, targetSize * 0.78f)
                quadTo(targetSize * 0.50f, targetSize * 0.68f, targetSize.toFloat(), targetSize * 0.74f)
                lineTo(targetSize.toFloat(), targetSize.toFloat())
                close()
            }
            canvas.drawPath(path2, hillPaint2)

            // Subtle musical note symbol in center
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 220
                style = Paint.Style.FILL
            }
            val noteRadius = (targetSize * 0.055f).coerceAtLeast(3f)
            canvas.drawCircle(targetSize * 0.45f, targetSize * 0.42f, noteRadius, notePaint)
            val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 220
                strokeWidth = (targetSize * 0.025f).coerceAtLeast(2f)
                style = Paint.Style.STROKE
            }
            val stemX = targetSize * 0.45f + noteRadius - (stemPaint.strokeWidth / 2f)
            val stemTop = targetSize * 0.30f
            val stemBottom = targetSize * 0.42f
            canvas.drawLine(stemX, stemBottom, stemX, stemTop, stemPaint)
            canvas.drawLine(stemX, stemTop, stemX + targetSize * 0.08f, stemTop + targetSize * 0.02f, stemPaint)

            return bitmap
        }

        internal fun getVinylBitmapWithCenterArt(
            context: Context,
            centerArt: Bitmap?,
            targetSize: Int
        ): Bitmap {
            val discBitmap = loadScaledBitmapFromRes(context, R.drawable.ic_widget_vinyl_disc, targetSize)
                ?: Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888).apply {
                    Canvas(this).drawColor(Color.parseColor("#151517"))
                }

            val safeCenterArt = centerArt ?: createDefaultArtworkBitmap("vinyl", targetSize / 2)

            val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawBitmap(discBitmap, 0f, 0f, null)

            // Center circle diameter is ~38% of vinyl disc size (matching ic_widget_vinyl_disc center radius 12 out of 32)
            val centerDiameter = (targetSize * (24f / 64f)).toInt().coerceAtLeast(16)
            val circularCenter = getCircularBitmap(Bitmap.createScaledBitmap(safeCenterArt, centerDiameter, centerDiameter, true))

            val left = (targetSize - centerDiameter) / 2f
            val top = (targetSize - centerDiameter) / 2f
            canvas.drawBitmap(circularCenter, left, top, null)

            // Center spindle hole (diameter ~11% of vinyl size)
            val spindleDiameter = (targetSize * (7f / 64f)).toInt().coerceAtLeast(4)
            val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#111113")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(targetSize / 2f, targetSize / 2f, spindleDiameter / 2f, holePaint)

            return result
        }

        private fun loadScaledBitmapFromRes(context: Context, resId: Int, targetSize: Int): Bitmap? {
            return try {
                val drawable = ContextCompat.getDrawable(context, resId) ?: return null
                val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, targetSize, targetSize)
                drawable.draw(canvas)
                bitmap
            } catch (e: Exception) {
                null
            }
        }

        internal fun getCircularBitmap(bitmap: Bitmap): Bitmap {
            val size = minOf(bitmap.width, bitmap.height)
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            val srcRect = Rect(
                (bitmap.width - size) / 2,
                (bitmap.height - size) / 2,
                (bitmap.width + size) / 2,
                (bitmap.height + size) / 2
            )
            val dstRect = Rect(0, 0, size, size)
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
            return output
        }

        internal fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
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

// Subclasses for all 10 distinct widget styles
class MiniWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_mini
    override val widgetStyleId: String = "mini"
}

class StandardWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_standard
    override val widgetStyleId: String = "standard"
}

class CardWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_card
    override val widgetStyleId: String = "card"
}

class PracticalWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_practical
    override val widgetStyleId: String = "practical"
}

class StylishWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_stylish
    override val widgetStyleId: String = "stylish"
}

class ClassicWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_classic
    override val widgetStyleId: String = "classic"
}

class LiteWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_lite
    override val widgetStyleId: String = "lite"
}

class VinylWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_vinyl
    override val widgetStyleId: String = "vinyl"
}

class SimpleWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_simple
    override val widgetStyleId: String = "simple"
}

class CircularWidgetProvider : MusicAppWidgetProvider() {
    override val layoutResId: Int = R.layout.widget_style_circular
    override val widgetStyleId: String = "circular"
}
