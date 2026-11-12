package com.dhananjaysaini.musicplayerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.PlaylistEntity

class PlaylistAdapter(
    private val list: ArrayList<PlaylistEntity>
) : RecyclerView.Adapter<PlaylistAdapter.Holder>() {

    var onItemClick: ((PlaylistEntity) -> Unit)? = null
    var onDeleteClick: ((PlaylistEntity) -> Unit)? = null
    var onRenameClick: ((PlaylistEntity) -> Unit)? = null



    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val playlistName: TextView = view.findViewById(R.id.playlistName)
//        val playlistSongCount: TextView = view.findViewById(R.id.playlistSongCount)
        val playlistMenuIcon:ImageView = view.findViewById(R.id.playlistMenuIcon)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.playlist_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val playlist = list[position]
        holder.playlistName.text = playlist.name

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(playlist)
        }

//        holder.playlistSongCount.text =
//            when (playlist.songCount) {
//                0 -> "No songs"
//                1 -> "1 song"
//                else -> "${playlist.songCount} songs"
//            }

        holder.playlistMenuIcon.setOnClickListener { view ->

            val popup = PopupMenu(view.context, view)

            popup.menu.add(0, 1, 0, view.context.getString(R.string.rename_playlist))
            popup.menu.add(0, 2, 1, view.context.getString(R.string.delete_playlist_title))

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {

                    1 -> {
                        onRenameClick?.invoke(playlist)
                        true
                    }

                    2 -> {
                        onDeleteClick?.invoke(playlist)
                        true
                    }

                    else -> false
                }
            }

            popup.show()

        }
    }

    override fun getItemCount() = list.size

    fun update(newList: List<PlaylistEntity>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

}
