package com.dhananjaysaini.musicplayerapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.ItemSelectSongBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.formatDuration

class SelectSongsAdapter(
    private val context: Context,
    private var allSongs: ArrayList<Music>
) : RecyclerView.Adapter<SelectSongsAdapter.SelectSongViewHolder>() {

    private val displaySongs: ArrayList<Music> = ArrayList(allSongs)
    private val selectedIds = mutableSetOf<String>()

    var onSelectionChanged: ((selectedCount: Int, isAllSelected: Boolean) -> Unit)? = null

    inner class SelectSongViewHolder(val binding: ItemSelectSongBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectSongViewHolder {
        val binding = ItemSelectSongBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SelectSongViewHolder(binding)
    }

    override fun getItemCount(): Int = displaySongs.size

    override fun onBindViewHolder(holder: SelectSongViewHolder, position: Int) {
        val song = displaySongs[position]
        val isSelected = selectedIds.contains(song.id)

        holder.binding.tvSongTitle.text = song.title
        val subtitle = if (song.artist.isNotBlank() && song.artist != "<unknown>") {
            "${song.artist} • ${song.album}"
        } else {
            song.album
        }
        holder.binding.tvSongSubtitle.text = subtitle

        if (isSelected) {
            holder.binding.ivCheck.setImageResource(R.drawable.ic_circle_checked)
        } else {
            holder.binding.ivCheck.setImageResource(R.drawable.ic_circle_unchecked)
        }

        Glide.with(context)
            .load(song.artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_neon_music_note)
                    .error(R.drawable.ic_neon_music_note)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
            )
            .into(holder.binding.ivSongArt)

        holder.itemView.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                val currentSong = displaySongs[currentPos]
                if (selectedIds.contains(currentSong.id)) {
                    selectedIds.remove(currentSong.id)
                } else {
                    selectedIds.add(currentSong.id)
                }
                notifyItemChanged(currentPos)
                onSelectionChanged?.invoke(selectedIds.size, isAllSelected())
            }
        }
    }

    fun isAllSelected(): Boolean {
        if (displaySongs.isEmpty()) return false
        return displaySongs.all { selectedIds.contains(it.id) }
    }

    fun getSelectedCount(): Int = selectedIds.size

    fun getSelectedSongs(): List<Music> {
        return allSongs.filter { selectedIds.contains(it.id) }
    }

    fun toggleSelectAll() {
        if (isAllSelected()) {
            displaySongs.forEach { selectedIds.remove(it.id) }
        } else {
            displaySongs.forEach { selectedIds.add(it.id) }
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedIds.size, isAllSelected())
    }

    fun filter(query: String) {
        displaySongs.clear()
        if (query.isBlank()) {
            displaySongs.addAll(allSongs)
        } else {
            val lower = query.trim().lowercase()
            for (song in allSongs) {
                if (song.title.lowercase().contains(lower) ||
                    song.artist.lowercase().contains(lower) ||
                    song.album.lowercase().contains(lower)
                ) {
                    displaySongs.add(song)
                }
            }
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedIds.size, isAllSelected())
    }

    fun updateSongs(newList: List<Music>) {
        allSongs = ArrayList(newList)
        selectedIds.retainAll(allSongs.map { it.id }.toSet())
        displaySongs.clear()
        displaySongs.addAll(allSongs)
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedIds.size, isAllSelected())
    }
}
