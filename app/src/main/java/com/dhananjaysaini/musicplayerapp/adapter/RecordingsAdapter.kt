package com.dhananjaysaini.musicplayerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.ItemRecordingBinding
import com.dhananjaysaini.musicplayerapp.model.RecordingItem
import com.dhananjaysaini.musicplayerapp.model.formatDuration
import java.text.SimpleDateFormat
import java.util.*

class RecordingsAdapter(
    private var items: MutableList<RecordingItem>,
    private val onPlayClick: (RecordingItem, Int) -> Unit,
    private val onDeleteClick: (RecordingItem) -> Unit,
    private val onRenameClick: (RecordingItem) -> Unit,
    private val onShareClick: (RecordingItem) -> Unit,
    private val onSeek: (RecordingItem, Int) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {

    var playingPosition: Int = -1
    var isPlaying: Boolean = false
    private var currentPlayProgressMs: Int = 0
    private var currentPlayDurationMs: Int = 0

    private val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    fun updateList(newItems: List<RecordingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updatePlaybackProgress(position: Int, progressMs: Int, durationMs: Int, playing: Boolean) {
        val prevPos = playingPosition
        val posChanged = prevPos != position
        playingPosition = position
        isPlaying = playing
        currentPlayProgressMs = progressMs
        currentPlayDurationMs = durationMs

        if (posChanged && prevPos != -1) {
            notifyItemChanged(prevPos)
        }
        if (position in items.indices) {
            notifyItemChanged(position, "PROGRESS_PAYLOAD")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val binding = ItemRecordingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun onBindViewHolder(
        holder: RecordingViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains("PROGRESS_PAYLOAD")) {
            holder.updateProgressOnly(items[position], position)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class RecordingViewHolder(private val binding: ItemRecordingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecordingItem, position: Int) {
            binding.tvRecordingName.text = item.name

            val dateStr = dateFormat.format(Date(item.dateMs))
            val durationStr = formatDuration(item.durationMs)
            val sizeStr = formatFileSize(item.sizeBytes)
            binding.tvRecordingDetails.text = "$durationStr • $dateStr • $sizeStr"

            val isThisPlaying = position == playingPosition

            binding.btnPlayRecording.setImageResource(
                if (isThisPlaying && isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            if (isThisPlaying) {
                binding.layoutInlinePlayer.visibility = View.VISIBLE
                binding.seekRecordingProgress.max = currentPlayDurationMs.coerceAtLeast(1)
                binding.seekRecordingProgress.progress = currentPlayProgressMs
                binding.tvCurrentPlayTime.text = formatDuration(currentPlayProgressMs.toLong())
                binding.tvTotalPlayTime.text = formatDuration(currentPlayDurationMs.toLong())
            } else {
                binding.layoutInlinePlayer.visibility = View.GONE
            }

            binding.btnPlayRecording.setOnClickListener {
                onPlayClick(item, position)
            }

            binding.seekRecordingProgress.setOnSeekBarChangeListener(object :
                android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        onSeek(item, progress)
                    }
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })

            binding.btnMoreOptions.setOnClickListener { v ->
                val popup = PopupMenu(v.context, v)
                popup.menu.add(0, 1, 0, "Rename")
                popup.menu.add(0, 2, 1, "Share")
                popup.menu.add(0, 3, 2, "Delete")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> onRenameClick(item)
                        2 -> onShareClick(item)
                        3 -> onDeleteClick(item)
                    }
                    true
                }
                popup.show()
            }
        }

        fun updateProgressOnly(item: RecordingItem, position: Int) {
            val isThisPlaying = position == playingPosition
            binding.btnPlayRecording.setImageResource(
                if (isThisPlaying && isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            if (isThisPlaying) {
                binding.layoutInlinePlayer.visibility = View.VISIBLE
                binding.seekRecordingProgress.max = currentPlayDurationMs.coerceAtLeast(1)
                binding.seekRecordingProgress.progress = currentPlayProgressMs
                binding.tvCurrentPlayTime.text = formatDuration(currentPlayProgressMs.toLong())
                binding.tvTotalPlayTime.text = formatDuration(currentPlayDurationMs.toLong())
            } else {
                binding.layoutInlinePlayer.visibility = View.GONE
            }
        }

        private fun formatFileSize(size: Long): String {
            if (size <= 0) return "0 B"
            val kb = size / 1024f
            val mb = kb / 1024f
            return if (mb >= 1.0f) {
                String.format(Locale.getDefault(), "%.1f MB", mb)
            } else {
                String.format(Locale.getDefault(), "%.0f KB", kb)
            }
        }
    }
}
