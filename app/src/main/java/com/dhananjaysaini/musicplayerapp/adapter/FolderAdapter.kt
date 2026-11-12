package com.dhananjaysaini.musicplayerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.model.SongFolder
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper

class FolderAdapter(
    private val list: MutableList<SongFolder>
) : RecyclerView.Adapter<FolderAdapter.Holder>() {

    var onItemClick: ((SongFolder) -> Unit)? = null

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.folderName)
        val count: TextView = view.findViewById(R.id.folderCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.folder_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val folder = list[position]

        holder.name.text = folder.name
        holder.count.text = LocaleHelper.getSongCountString(holder.itemView.context, folder.songCount)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(folder)
        }
    }

    override fun getItemCount() = list.size

    fun update(newList: List<SongFolder>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
