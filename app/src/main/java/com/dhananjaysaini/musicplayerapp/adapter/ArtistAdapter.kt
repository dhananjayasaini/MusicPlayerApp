package com.dhananjaysaini.musicplayerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.MusicArtist

class ArtistAdapter(
    private val list: MutableList<MusicArtist>
) : RecyclerView.Adapter<ArtistAdapter.Holder>() {

    var onItemClick: ((MusicArtist) -> Unit)? = null

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.artistName)
        val count: TextView = view.findViewById(R.id.artistCount)
        val artistImage: ImageView = itemView.findViewById(R.id.artistImage)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.artist_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val artist = list[position]

        holder.name.text = artist.name
        holder.count.text =
            if (artist.songCount == 1)
                "1 song"
            else
                "${artist.songCount} songs"

        Glide.with(holder.itemView.context)
            .load(artist.artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_neon_artist).centerCrop())
            .into(holder.artistImage)


        holder.itemView.setOnClickListener {
            onItemClick?.invoke(artist)
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<MusicArtist>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}

