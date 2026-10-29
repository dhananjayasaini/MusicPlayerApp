package com.dhananjaysaini.musicplayerapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.MusicViewBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.formatDuration

class AllSongsAdapter(
    context: Context,
    list: ArrayList<Music>
) : BaseMusicAdapter(context, list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            MusicViewBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val song = musicList[position]

        holder.title.text = song.title
        holder.album.text = song.album
        holder.duration.text = formatDuration(song.duration)

        Glide.with(holder.itemView.context)
            .load(song.artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_music)
                    .error(R.drawable.ic_music)
                    .override(150, 150)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            )
            .into(holder.image)

        bindClick(holder, position)
    }

    override fun updateList( list: ArrayList<Music>) {
        musicList = list
        notifyDataSetChanged()
    }
}
