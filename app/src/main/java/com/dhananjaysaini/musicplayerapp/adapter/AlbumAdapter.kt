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

class AlbumAdapter(
    private val list: MutableList<MusicArtist>
) : RecyclerView.Adapter<AlbumAdapter.Holder>() {

    var onItemClick: ((MusicArtist) -> Unit)? = null

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.albumName)
        val subName: TextView = view.findViewById(R.id.itemSubName)
        val count: TextView = view.findViewById(R.id.albumCount)
        val albumImage: ImageView = itemView.findViewById(R.id.albumImage)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_view_grid, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val album = list[position]

        holder.name.text = album.name
        holder.subName.text = album.name


        holder.count.text =
            if (album.songCount == 1)
                "1 Song"
            else
                "${album.songCount} Songs"

        Glide.with(holder.itemView.context)
            .load(album.artUri)
            .apply(RequestOptions().placeholder(R.drawable.ic_neon_album).centerCrop())
            .into(holder.albumImage)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(album)
        }

    }

    override fun getItemCount() = list.size

    fun update(newList: List<MusicArtist>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}

