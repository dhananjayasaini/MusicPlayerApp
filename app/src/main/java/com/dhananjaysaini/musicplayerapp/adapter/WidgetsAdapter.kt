package com.dhananjaysaini.musicplayerapp.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.ItemWidgetCardBinding
import com.dhananjaysaini.musicplayerapp.model.WidgetStyleItem
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.widget.MusicAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WidgetsAdapter(
    private val widgetStyles: List<WidgetStyleItem>,
    private val onWidgetClick: (WidgetStyleItem) -> Unit
) : RecyclerView.Adapter<WidgetsAdapter.WidgetViewHolder>() {

    companion object {
        private val vinylCache = LruCache<String, Bitmap>(20)
    }

    inner class WidgetViewHolder(val binding: ItemWidgetCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetViewHolder {
        val binding = ItemWidgetCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WidgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
        val item = widgetStyles[position]
        val context = holder.itemView.context

        val currentTheme = ThemeManager.getCurrentTheme(context)
        val isDark = currentTheme.isDark

        if (isDark) {
            holder.binding.cardWidgetRoot.setBackgroundResource(R.drawable.bg_widget_list_card)
            holder.binding.tvWidgetName.setTextColor(Color.WHITE)
            holder.binding.tvWidgetSize.setTextColor(Color.parseColor("#D8BED0"))
            holder.binding.btnAddWidget.setBackgroundResource(R.drawable.bg_btn_add_widget)
            holder.binding.btnAddWidget.setTextColor(Color.parseColor("#FF9F00"))
        } else {
            holder.binding.cardWidgetRoot.setBackgroundResource(R.drawable.bg_widget_list_card_light)
            holder.binding.tvWidgetName.setTextColor(Color.parseColor("#111827"))
            holder.binding.tvWidgetSize.setTextColor(Color.parseColor("#6B7280"))
            holder.binding.btnAddWidget.setBackgroundResource(R.drawable.bg_btn_add_widget_light)
            holder.binding.btnAddWidget.setTextColor(Color.parseColor("#E65100"))
        }

        holder.binding.tvWidgetName.text = context.getString(item.nameRes)
        holder.binding.tvWidgetSize.text = context.getString(item.sizeRes)

        // Reuse previewView if matching item.id is already in previewContainer
        val previewView = if (holder.binding.previewContainer.tag == item.id && holder.binding.previewContainer.childCount > 0) {
            holder.binding.previewContainer.getChildAt(0)
        } else {
            holder.binding.previewContainer.removeAllViews()
            val view = LayoutInflater.from(context).inflate(
                item.previewLayoutRes,
                holder.binding.previewContainer,
                false
            )
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
            holder.binding.previewContainer.addView(view, params)
            holder.binding.previewContainer.tag = item.id
            view
        }

        // Populate song info and artwork if available
        val prefs = context.getSharedPreferences("widget_playback_state", Context.MODE_PRIVATE)
        val currentSong = if (MusicService.playlist.isNotEmpty() && MusicService.position in MusicService.playlist.indices) {
            MusicService.playlist[MusicService.position]
        } else null

        val title = currentSong?.title ?: prefs.getString("title", null) ?: "Love Story"
        val artist = currentSong?.artist ?: prefs.getString("artist", null) ?: "Taylor Swift"
        val artUri = currentSong?.artUri ?: prefs.getString("artUri", null)
        val songPath = currentSong?.path ?: prefs.getString("path", null)

        previewView.findViewById<TextView>(R.id.tvSongTitle)?.text = title
        previewView.findViewById<TextView>(R.id.tvSongArtist)?.text = artist

        // Bind artwork to ivSongArt asynchronously
        val ivSongArt = previewView.findViewById<ImageView>(R.id.ivSongArt)
        if (ivSongArt != null) {
            val hasArt = (!artUri.isNullOrEmpty() && artUri != "null") || (!songPath.isNullOrEmpty() && File(songPath).exists())

            when (item.id) {
                "practical", "vinyl" -> {
                    val discSize = if (item.id == "practical") 180 else 160
                    val centerSize = if (item.id == "practical") 120 else 100
                    val cacheKey = "${artUri}_${songPath}_$discSize"
                    val cached = vinylCache.get(cacheKey)
                    if (cached != null) {
                        ivSongArt.setImageBitmap(cached)
                    } else {
                        if (hasArt) {
                            val targetTag = item.id
                            ivSongArt.tag = targetTag
                            CoroutineScope(Dispatchers.IO).launch {
                                val centerArt = MusicAppWidgetProvider.loadArtworkBitmap(context, artUri, songPath, centerSize)
                                    ?: MusicAppWidgetProvider.createDefaultArtworkBitmap(item.id, centerSize)
                                val composite = MusicAppWidgetProvider.getVinylBitmapWithCenterArt(context, centerArt, discSize)
                                vinylCache.put(cacheKey, composite)
                                withContext(Dispatchers.Main) {
                                    if (ivSongArt.tag == targetTag) {
                                        ivSongArt.setImageBitmap(composite)
                                    }
                                }
                            }
                        } else {
                            val defaultCenter = MusicAppWidgetProvider.createDefaultArtworkBitmap(item.id, centerSize)
                            val composite = MusicAppWidgetProvider.getVinylBitmapWithCenterArt(context, defaultCenter, discSize)
                            vinylCache.put(cacheKey, composite)
                            ivSongArt.setImageBitmap(composite)
                        }
                    }
                }
                "circular" -> {
                    val targetSize = 160
                    val defaultBmp = MusicAppWidgetProvider.createDefaultArtworkBitmap(item.id, targetSize)
                    val circularBmp = MusicAppWidgetProvider.getCircularBitmap(defaultBmp)
                    if (hasArt) {
                        val model: Any = if (!artUri.isNullOrEmpty() && artUri != "null") artUri else File(songPath!!)
                        Glide.with(context)
                            .asBitmap()
                            .load(model)
                            .circleCrop()
                            .placeholder(BitmapDrawable(context.resources, circularBmp))
                            .error(BitmapDrawable(context.resources, circularBmp))
                            .into(ivSongArt)
                    } else {
                        ivSongArt.setImageBitmap(circularBmp)
                    }
                }
                else -> {
                    val targetSize = when (item.id) {
                        "stylish" -> 200
                        "standard", "card" -> 160
                        else -> 140
                    }
                    val cornerRadius = when (item.id) {
                        "stylish" -> 28f
                        "standard", "card" -> 24f
                        "mini" -> 22f
                        "simple" -> 20f
                        else -> 18f
                    }
                    val defaultBmp = MusicAppWidgetProvider.getRoundedCornerBitmap(
                        MusicAppWidgetProvider.createDefaultArtworkBitmap(item.id, targetSize),
                        cornerRadius
                    )
                    if (hasArt) {
                        val model: Any = if (!artUri.isNullOrEmpty() && artUri != "null") artUri else File(songPath!!)
                        Glide.with(context)
                            .asBitmap()
                            .load(model)
                            .transform(CenterCrop(), RoundedCorners(cornerRadius.toInt()))
                            .placeholder(BitmapDrawable(context.resources, defaultBmp))
                            .error(BitmapDrawable(context.resources, defaultBmp))
                            .into(ivSongArt)
                    } else {
                        ivSongArt.setImageBitmap(defaultBmp)
                    }
                }
            }
        }

        // Click listeners
        holder.binding.cardWidgetRoot.setOnClickListener {
            onWidgetClick(item)
        }
        holder.binding.btnAddWidget.setOnClickListener {
            onWidgetClick(item)
        }
    }

    override fun getItemCount(): Int = widgetStyles.size

    private fun getDefaultArtResource(styleId: String): Int {
        return when (styleId) {
            "mini" -> R.drawable.theme_nature_4
            "standard" -> R.drawable.theme_nature_5
            "card" -> R.drawable.theme_nature_3
            "practical" -> R.drawable.ic_widget_vinyl_disc
            "stylish" -> R.drawable.theme_nature_1
            "classic" -> R.drawable.theme_nature_2
            "lite" -> R.drawable.theme_nature_7
            "vinyl" -> R.drawable.ic_widget_vinyl_disc
            "simple" -> R.drawable.theme_nature_8
            "circular" -> R.drawable.theme_nature_6
            else -> R.drawable.theme_nature_1
        }
    }
}
