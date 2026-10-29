package com.dhananjaysaini.musicplayerapp.utils

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetSongOptionsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SongOptionsBottomSheet(
    private val song: Music,
    private val position: Int,
    private val config: SongMenuConfig
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSongOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSongOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSongTitle.text = song.title

        binding.btnPlay.visibility = if (config.play) View.VISIBLE else View.GONE
        binding.btnPlayNext.visibility = if (config.playNext) View.VISIBLE else View.GONE
        binding.btnAddToQueue.visibility = if (config.addToQueue) View.VISIBLE else View.GONE
        binding.btnAddFav.visibility = if (config.addToFav) View.VISIBLE else View.GONE
        binding.btnRemoveFav.visibility = if (config.removeFromFav) View.VISIBLE else View.GONE
        binding.btnAddPlaylist.visibility = if (config.addToPlaylist) View.VISIBLE else View.GONE
        binding.btnEdit.visibility = if (config.edit) View.VISIBLE else View.GONE
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
            SongMenuManager.handleRemoveFromPlaylist(requireContext(), song)
            dismiss()
        }

        binding.btnDeleteSong.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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