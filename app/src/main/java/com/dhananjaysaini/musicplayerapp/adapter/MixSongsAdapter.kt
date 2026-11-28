package com.dhananjaysaini.musicplayerapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.Music
import java.io.File

class MixSongsAdapter(
    private val context: Context,
    private var songs: ArrayList<Music>,
    private val isRecap: Boolean = false,
    private val playCounts: Map<String, Int> = emptyMap()
) : RecyclerView.Adapter<MixSongsAdapter.ViewHolder>() {

    var onItemClick: ((List<Music>, Int) -> Unit)? = null
    var onMenuClick: ((Music, Int) -> Unit)? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val ivArt: ImageView = view.findViewById(R.id.ivArt)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val layoutPlayCount: LinearLayout = view.findViewById(R.id.layoutPlayCount)
        val tvPlayCount: TextView = view.findViewById(R.id.tvPlayCount)
        val btnMenu: ImageView = view.findViewById(R.id.btnMenu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_mix_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]

        holder.tvTitle.text = song.title

        val artist = song.artist ?: "<unknown>"
        val album = song.album?.takeIf { it.isNotBlank() }
        holder.tvSubtitle.text = if (album != null) "$artist - $album" else artist

        Glide.with(context)
            .load(song.artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_neon_music_note).centerCrop())
            .into(holder.ivArt)

        if (isRecap) {
            holder.tvRank.visibility = View.VISIBLE
            holder.tvRank.text = (position + 1).toString()

            val count = playCounts[song.id] ?: 1
            holder.layoutPlayCount.visibility = View.VISIBLE
            holder.tvPlayCount.text = count.toString()

            holder.tvBadge.visibility = View.GONE
        } else {
            holder.tvRank.visibility = View.GONE
            holder.layoutPlayCount.visibility = View.GONE

            if (isHighQuality(song)) {
                holder.tvBadge.visibility = View.VISIBLE
                val lower = song.path.lowercase()
                holder.tvBadge.text = if (lower.endsWith(".flac") || lower.endsWith(".wav")) "HD" else "320K"
            } else {
                holder.tvBadge.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(songs, position)
        }

        holder.btnMenu.setOnClickListener {
            onMenuClick?.invoke(song, position)
        }
    }

    override fun getItemCount(): Int = songs.size

    fun updateList(newSongs: List<Music>) {
        songs = ArrayList(newSongs)
        notifyDataSetChanged()
    }

    private fun isHighQuality(song: Music): Boolean {
        val lower = song.path.lowercase()
        if (lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".alac") || lower.endsWith(".ape")) {
            return true
        }
        try {
            if (song.duration > 0) {
                val file = File(song.path)
                if (file.exists()) {
                    val kbps = (file.length() * 8) / song.duration
                    if (kbps >= 256) return true
                }
            }
        } catch (_: Exception) {}
        return false
    }
}
