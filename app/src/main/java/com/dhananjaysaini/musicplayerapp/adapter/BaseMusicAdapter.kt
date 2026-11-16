package com.dhananjaysaini.musicplayerapp.adapter

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.MusicViewBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService

abstract class BaseMusicAdapter(
    protected val context: Context,
    protected var musicList: ArrayList<Music>
) : RecyclerView.Adapter<BaseMusicAdapter.MyHolder>() {

    companion object {
        const val PAYLOAD_PLAYING_STATE = "PAYLOAD_PLAYING_STATE"
    }

    var onItemClick: ((ArrayList<Music>, Int) -> Unit)? = null
    var onMenuClick: ((Music, Int) -> Unit)? = null
    var onAddToPlaylist: ((Music) -> Unit)? = null
    var onRemoveFromPlaylist: ((Music) -> Unit)? = null

    var currentSongId: String? = MusicService.song?.id
    var isCurrentPlaying: Boolean = MusicService.mediaPlayer?.isPlaying == true

    private var activeLifecycleOwner: LifecycleOwner? = null
    private val songObserver = Observer<Music?> { song ->
        updatePlayingState(song?.id, MusicService.mediaPlayer?.isPlaying == true)
    }
    private val playingObserver = Observer<Boolean> { isPlaying ->
        updatePlayingState(MusicService.song?.id, isPlaying)
    }

    class MyHolder(val binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameMV
        val album = binding.songAlbumMV
        val duration = binding.songDuration
        val image = binding.imageMV
        val menuIcon = binding.menuIcon
        val root = binding.root
        val artBorderView = binding.artBorderView
    }

    override fun getItemCount(): Int = musicList.size

    open fun updateList(list: ArrayList<Music>) {
        musicList = list
        notifyDataSetChanged()
    }

    fun attachLifecycle(lifecycleOwner: LifecycleOwner) {
        if (activeLifecycleOwner == lifecycleOwner) return
        detachLifecycle()
        activeLifecycleOwner = lifecycleOwner
        MusicService.currentSongLiveData.observe(lifecycleOwner, songObserver)
        MusicService.isPlayingLiveData.observe(lifecycleOwner, playingObserver)
        updatePlayingState(MusicService.song?.id, MusicService.mediaPlayer?.isPlaying == true)
    }

    fun detachLifecycle() {
        activeLifecycleOwner?.let {
            MusicService.currentSongLiveData.removeObserver(songObserver)
            MusicService.isPlayingLiveData.removeObserver(playingObserver)
            activeLifecycleOwner = null
        }
    }

    fun updatePlayingState(songId: String?, isPlaying: Boolean) {
        val oldSongId = currentSongId
        val oldIsPlaying = isCurrentPlaying

        currentSongId = songId
        isCurrentPlaying = isPlaying

        if (oldSongId == songId && oldIsPlaying == isPlaying) return

        if (oldSongId != songId) {
            if (oldSongId != null) {
                val oldIndex = musicList.indexOfFirst { it.id == oldSongId }
                if (oldIndex != -1) {
                    notifyItemChanged(oldIndex, PAYLOAD_PLAYING_STATE)
                }
            }
            if (songId != null) {
                val newIndex = musicList.indexOfFirst { it.id == songId }
                if (newIndex != -1) {
                    notifyItemChanged(newIndex, PAYLOAD_PLAYING_STATE)
                }
            }
        } else {
            if (songId != null) {
                val index = musicList.indexOfFirst { it.id == songId }
                if (index != -1) {
                    notifyItemChanged(index, PAYLOAD_PLAYING_STATE)
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        currentSongId = MusicService.song?.id
        isCurrentPlaying = MusicService.mediaPlayer?.isPlaying == true
        val owner = (recyclerView.context as? LifecycleOwner)
        if (owner != null) {
            attachLifecycle(owner)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        detachLifecycle()
    }

    override fun onViewRecycled(holder: MyHolder) {
        super.onViewRecycled(holder)
        holder.artBorderView.stopAnimation()
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_PLAYING_STATE)) {
            if (position in musicList.indices) {
                bindPlayingState(holder, musicList[position])
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    open fun bindPlayingState(holder: MyHolder, song: Music) {
        val activeSongId = currentSongId ?: MusicService.song?.id
        val isThisSong = !activeSongId.isNullOrEmpty() && song.id == activeSongId
        val isPlaying = isThisSong && (isCurrentPlaying || (MusicService.mediaPlayer?.isPlaying == true))

        if (isThisSong) {
            // Highlight ALL text with duration
            val highlightColor = ContextCompat.getColor(context, R.color.bright_yellow)
            holder.title.setTextColor(highlightColor)
            holder.album.setTextColor(highlightColor)
            holder.duration.setTextColor(highlightColor)

            // No extra layout cover whole audio
            holder.root.background = null

            // Border animation around art URI
            holder.artBorderView.visibility = View.VISIBLE
            if (isPlaying) {
                holder.artBorderView.startAnimation()
            } else {
                holder.artBorderView.pauseAnimation()
            }
        } else {
            // Standard theme colors
            val typedValue = TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            holder.title.setTextColor(typedValue.data)

            val variantValue = TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, variantValue, true)
            holder.album.setTextColor(variantValue.data)
            holder.duration.setTextColor(variantValue.data)

            // No extra background
            holder.root.background = null

            // Hide border animation
            holder.artBorderView.visibility = View.GONE
            holder.artBorderView.stopAnimation()
        }
    }

    protected fun bindClick(holder: MyHolder, position: Int) {
        holder.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onItemClick?.invoke(musicList, pos)
            }
        }

        holder.menuIcon.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onMenuClick?.invoke(musicList[pos], pos)
            }
        }
    }
}
