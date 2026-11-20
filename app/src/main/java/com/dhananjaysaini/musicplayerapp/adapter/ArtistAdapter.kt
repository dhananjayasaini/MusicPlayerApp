package com.dhananjaysaini.musicplayerapp.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.MusicArtist
import com.dhananjaysaini.musicplayerapp.utils.ArtistAvatarHelper
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper

class ArtistAdapter(
    private val list: MutableList<MusicArtist>
) : RecyclerView.Adapter<ArtistAdapter.Holder>() {

    var onItemClick: ((MusicArtist) -> Unit)? = null
    var onMenuClick: ((MusicArtist, View) -> Unit)? = null

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.artistName)
        val count: TextView = view.findViewById(R.id.artistCount)
        val artistImage: ImageView = view.findViewById(R.id.artistImage)
        val tvArtistInitial: TextView = view.findViewById(R.id.tvArtistInitial)
        val ivArtistIconFallback: ImageView = view.findViewById(R.id.ivArtistIconFallback)
        val menuIcon: ImageView = view.findViewById(R.id.artistMenuIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.artist_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val artist = list[position]

        holder.name.text = artist.name

        val albumStr = LocaleHelper.getAlbumCountString(holder.itemView.context, artist.albumCount).lowercase()
        val songStr = LocaleHelper.getSongCountString(holder.itemView.context, artist.songCount).lowercase()
        holder.count.text = "$albumStr | $songStr"

        val initial = ArtistAvatarHelper.getInitial(artist.name)
        if (initial != null) {
            holder.tvArtistInitial.visibility = View.VISIBLE
            holder.tvArtistInitial.text = initial
            holder.tvArtistInitial.setTextColor(ArtistAvatarHelper.getColorForName(artist.name))
            holder.ivArtistIconFallback.visibility = View.GONE
        } else {
            holder.tvArtistInitial.visibility = View.GONE
            holder.ivArtistIconFallback.visibility = View.VISIBLE
        }

        if (!artist.artUri.isNullOrEmpty()) {
            holder.artistImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(artist.artUri)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.artistImage.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(holder.artistImage)
        } else {
            holder.artistImage.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(artist)
        }

        holder.menuIcon.setOnClickListener {
            onMenuClick?.invoke(artist, it)
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<MusicArtist>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
