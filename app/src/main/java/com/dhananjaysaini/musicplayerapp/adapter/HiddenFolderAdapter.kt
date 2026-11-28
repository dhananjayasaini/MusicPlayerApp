package com.dhananjaysaini.musicplayerapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.ItemHiddenFolderBinding
import com.dhananjaysaini.musicplayerapp.model.SongFolder
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper

class HiddenFolderAdapter(
    private val list: MutableList<SongFolder>,
    private val isHiddenSection: Boolean,
    private val onActionClick: (SongFolder) -> Unit
) : RecyclerView.Adapter<HiddenFolderAdapter.Holder>() {

    class Holder(val binding: ItemHiddenFolderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemHiddenFolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val folder = list[position]
        holder.binding.tvFolderName.text = folder.name
        holder.binding.tvFolderSongCount.text =
            LocaleHelper.getSongCountString(holder.itemView.context, folder.songCount)

        if (isHiddenSection) {
            holder.binding.ivActionEye.setImageResource(R.drawable.ic_eye_visible)
            holder.binding.ivActionEye.contentDescription = "Unhide folder"
            holder.binding.ivFolderIcon.setColorFilter(Color.parseColor("#8B5200"))
            holder.binding.tvFolderName.setTextColor(Color.parseColor("#A68F9E"))
            holder.binding.tvFolderSongCount.setTextColor(Color.parseColor("#735C6C"))
        } else {
            holder.binding.ivActionEye.setImageResource(R.drawable.ic_eye_off)
            holder.binding.ivActionEye.contentDescription = "Hide folder"
            holder.binding.ivFolderIcon.setColorFilter(Color.parseColor("#FFA000"))
            holder.binding.tvFolderName.setTextColor(Color.WHITE)
            holder.binding.tvFolderSongCount.setTextColor(Color.parseColor("#8E8E93"))
        }

        holder.binding.ivActionEye.setOnClickListener {
            onActionClick(folder)
        }
    }

    override fun getItemCount(): Int = list.size

    fun update(newList: List<SongFolder>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
