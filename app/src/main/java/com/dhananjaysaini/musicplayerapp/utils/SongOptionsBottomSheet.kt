package com.dhananjaysaini.musicplayerapp.utils

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetSongOptionsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.model.formatDuration
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongOptionsBottomSheet(
    private val song: Music,
    private val position: Int,
    private val config: SongMenuConfig
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSongOptionsBinding? = null
    private val binding get() = _binding!!

    var onRemoveFromPlaylist: ((Music) -> Unit)? = null
    var onSongHidden: ((Music) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSongOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply active theme to bottom sheet background
        ThemeManager.applyThemeToBottomSheet(requireContext(), binding.root)

        // Header info
        binding.tvSongTitle.text = song.title
        binding.tvSongSubtitle.text = "${song.artist} • ${song.album}"
        binding.tvSongDuration.text = formatDuration(song.duration)

        Glide.with(this)
            .load(song.artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_neon_music_note)
                    .error(R.drawable.ic_neon_music_note)
                    .override(150, 150)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            )
            .into(binding.ivSongArt)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Action visibility based on config
        binding.btnPlay.visibility = if (config.play) View.VISIBLE else View.GONE
        binding.btnPlayNext.visibility = if (config.playNext) View.VISIBLE else View.GONE
        binding.btnAddToQueue.visibility = if (config.addToQueue) View.VISIBLE else View.GONE
        binding.btnAddFav.visibility = if (config.addToFav) View.VISIBLE else View.GONE
        binding.btnRemoveFav.visibility = if (config.removeFromFav) View.VISIBLE else View.GONE
        binding.btnAddPlaylist.visibility = if (config.addToPlaylist) View.VISIBLE else View.GONE
        binding.btnRemovePlaylist.visibility = if (config.removeFromPlaylist) View.VISIBLE else View.GONE
        binding.btnSetRingtone.visibility = if (config.setAsRingtone) View.VISIBLE else View.GONE
        binding.btnGoToAlbum.visibility = if (config.goToAlbum) View.VISIBLE else View.GONE
        binding.btnEdit.visibility = if (config.edit) View.VISIBLE else View.GONE
        binding.btnHideSong.visibility = if (config.hideSong) View.VISIBLE else View.GONE
        binding.btnDeleteSong.visibility = if (config.delete) View.VISIBLE else View.GONE
        binding.btnShare.visibility = if (config.share) View.VISIBLE else View.GONE

        clickListeners()
    }

    private fun clickListeners() {
        binding.btnPlay.setOnClickListener {
            SongMenuManager.handlePlay(requireContext(), song)
            dismiss()
        }

        binding.btnPlayNext.setOnClickListener {
            SongMenuManager.handlePlayNext(requireContext(), song)
            dismiss()
        }

        binding.btnAddToQueue.setOnClickListener {
            SongMenuManager.handleAddToQueue(requireContext(), song)
            dismiss()
        }

        binding.btnAddPlaylist.setOnClickListener {
            SongMenuManager.handleAddToPlaylist(parentFragmentManager, song)
            dismiss()
        }

        binding.btnAddFav.setOnClickListener {
            SongMenuManager.handleAddToFavourite(requireContext(), song)
            dismiss()
        }

        binding.btnRemoveFav.setOnClickListener {
            SongMenuManager.handleRemoveFromFavourite(requireContext(), song)
            dismiss()
        }

        binding.btnRemovePlaylist.setOnClickListener {
            if (onRemoveFromPlaylist != null) {
                onRemoveFromPlaylist?.invoke(song)
            } else {
                SongMenuManager.handleRemoveFromPlaylist(requireContext(), song)
            }
            dismiss()
        }

        binding.btnSetRingtone.setOnClickListener {
            dismiss()
            SongMenuManager.handleSetAsRingtone(requireActivity(), song)
        }

        binding.btnGoToAlbum.setOnClickListener {
            dismiss()
            SongMenuManager.handleGoToAlbum(requireContext(), song)
        }

        binding.btnEdit.setOnClickListener {
            val fm = parentFragmentManager
            dismissAllowingStateLoss()
            EditSongBottomSheet.newInstance(song).show(fm, "edit_song")
        }

        binding.btnHideSong.setOnClickListener {
            dismiss()
            SongMenuManager.handleHideSong(requireActivity(), song) {
                onSongHidden?.invoke(song)
            }
        }

        binding.btnDeleteSong.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SongMenuManager.handleDelete(requireActivity(), song)
            } else {
                SongMenuManager.handleDelete(requireActivity(), song)
            }
            dismiss()
        }

        binding.btnShare.setOnClickListener {
            SongMenuManager.handleShare(requireContext(), song)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
