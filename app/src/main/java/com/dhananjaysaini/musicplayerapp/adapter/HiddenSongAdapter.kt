package com.dhananjaysaini.musicplayerapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.ItemHiddenSongBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.formatDuration

class HiddenSongAdapter(
    private val list: MutableList<Music>,
    private val isHiddenSection: Boolean,
    private val onActionClick: (Music) -> Unit
) : RecyclerView.Adapter<HiddenSongAdapter.Holder>() {

    class Holder(val binding: ItemHiddenSongBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemHiddenSongBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val song = list[position]
        holder.binding.tvSongTitle.text = song.title

        val durationStr = formatDuration(song.duration)
        val subtitle = if (song.artist.isNotEmpty() && song.artist != "<unknown>") {
            "${song.artist} • $durationStr"
        } else {
            durationStr
        }
        holder.binding.tvSongSubtitle.text = subtitle

        Glide.with(holder.itemView.context)
            .load(song.artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_neon_music_note)
                    .error(R.drawable.ic_neon_music_note)
                    .centerCrop()
            )
            .into(holder.binding.ivSongArt)

        if (isHiddenSection) {
            holder.binding.ivActionEye.setImageResource(R.drawable.ic_eye_visible)
            holder.binding.ivActionEye.contentDescription = "Unhide song"
            holder.binding.ivSongArt.alpha = 0.55f
            holder.binding.tvSongTitle.setTextColor(Color.parseColor("#A68F9E"))
            holder.binding.tvSongSubtitle.setTextColor(Color.parseColor("#735C6C"))
        } else {
            holder.binding.ivActionEye.setImageResource(R.drawable.ic_eye_off)
            holder.binding.ivActionEye.contentDescription = "Hide song"
            holder.binding.ivSongArt.alpha = 1.0f
            holder.binding.tvSongTitle.setTextColor(Color.WHITE)
            holder.binding.tvSongSubtitle.setTextColor(Color.parseColor("#8E8E93"))
        }

        holder.binding.ivActionEye.setOnClickListener {
            onActionClick(song)
        }
    }

    override fun getItemCount(): Int = list.size

    fun update(newList: List<Music>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
