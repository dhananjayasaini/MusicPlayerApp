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
import com.dhananjaysaini.musicplayerapp.repository.GenreRepository.GenreItem
import com.dhananjaysaini.musicplayerapp.utils.ArtistAvatarHelper
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper

class GenreAdapter(
    private val list: MutableList<GenreItem>
) : RecyclerView.Adapter<GenreAdapter.Holder>() {

    var onItemClick: ((GenreItem) -> Unit)? = null
    var onMenuClick: ((GenreItem, View) -> Unit)? = null

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.genreName)
        val count: TextView = view.findViewById(R.id.genreCount)
        val genreImage: ImageView = view.findViewById(R.id.genreImage)
        val tvGenreInitial: TextView = view.findViewById(R.id.tvGenreInitial)
        val ivGenreIconFallback: ImageView = view.findViewById(R.id.ivGenreIconFallback)
        val menuIcon: ImageView = view.findViewById(R.id.genreMenuIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.genre_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val genre = list[position]

        holder.name.text = genre.name
        holder.count.text = LocaleHelper.getSongCountString(holder.itemView.context, genre.songCount)

        val initial = ArtistAvatarHelper.getInitial(genre.name)
        if (initial != null) {
            holder.tvGenreInitial.visibility = View.VISIBLE
            holder.tvGenreInitial.text = initial
            holder.tvGenreInitial.setTextColor(ArtistAvatarHelper.getColorForName(genre.name))
            holder.ivGenreIconFallback.visibility = View.GONE
        } else {
            holder.tvGenreInitial.visibility = View.GONE
            holder.ivGenreIconFallback.visibility = View.VISIBLE
        }

        if (!genre.artUri.isNullOrEmpty()) {
            holder.genreImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(genre.artUri)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.genreImage.visibility = View.GONE
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
                .into(holder.genreImage)
        } else {
            holder.genreImage.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(genre)
        }

        holder.menuIcon.setOnClickListener {
            onMenuClick?.invoke(genre, it)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<GenreItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
