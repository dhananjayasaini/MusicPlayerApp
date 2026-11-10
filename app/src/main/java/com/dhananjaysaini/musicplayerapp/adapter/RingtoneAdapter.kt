package com.dhananjaysaini.musicplayerapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.ItemRingtoneBinding
import com.dhananjaysaini.musicplayerapp.model.formatDuration
import java.io.File

data class RingtoneModel(
    val id: String,
    val title: String,
    val tag: String,
    val file: File,
    val durationMs: Long,
    val isFromDevice: Boolean = false
)

class RingtoneAdapter(
    private var items: MutableList<RingtoneModel>,
    private val onPreviewClick: (RingtoneModel, Int) -> Unit,
    private val onSetClick: (RingtoneModel) -> Unit
) : RecyclerView.Adapter<RingtoneAdapter.RingtoneViewHolder>() {

    var playingPosition: Int = -1
    var isPlaying: Boolean = false

    fun updateList(newItems: List<RingtoneModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setPlaybackState(position: Int, playing: Boolean) {
        val prev = playingPosition
        playingPosition = position
        isPlaying = playing
        if (prev != -1 && prev != position) {
            notifyItemChanged(prev)
        }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RingtoneViewHolder {
        val binding = ItemRingtoneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RingtoneViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RingtoneViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class RingtoneViewHolder(private val binding: ItemRingtoneBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RingtoneModel, position: Int) {
            binding.tvRingtoneTitle.text = item.title
            binding.tvRingtoneTag.text = item.tag
            binding.tvRingtoneDuration.text = formatDuration(item.durationMs)

            val isThisPlaying = position == playingPosition && isPlaying

            binding.btnPreviewRingtone.setImageResource(
                if (isThisPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            binding.btnPreviewRingtone.setOnClickListener {
                onPreviewClick(item, position)
            }

            binding.btnSetRingtone.setOnClickListener {
                onSetClick(item)
            }
        }
    }
}
