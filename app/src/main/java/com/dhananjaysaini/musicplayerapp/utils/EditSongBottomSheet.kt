package com.dhananjaysaini.musicplayerapp.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetEditSongBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditSongBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditSongBinding? = null
    private val binding get() = _binding!!

    private var song: Music? = null

    companion object {
        private const val ARG_SONG = "arg_song"

        fun newInstance(song: Music): EditSongBottomSheet {
            return EditSongBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_SONG, song)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        song = arguments?.getSerializable(ARG_SONG) as? Music
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditSongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.setBackgroundResource(android.R.color.transparent)
        }
        dialog?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply active theme to bottom sheet background
        ThemeManager.applyThemeToBottomSheet(requireContext(), binding.root)

        val currentSong = song ?: run {
            dismissAllowingStateLoss()
            return
        }

        // Preview header
        binding.tvEditCurrentTitle.text = currentSong.title
        binding.tvEditCurrentArtist.text = currentSong.artist

        Glide.with(this)
            .load(currentSong.artUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_neon_music_note)
                    .error(R.drawable.ic_neon_music_note)
                    .override(120, 120)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            )
            .into(binding.ivEditSongArt)

        // Pre-fill editable input fields
        binding.etSongTitle.setText(currentSong.title)
        binding.etSongArtist.setText(currentSong.artist)
        binding.etSongAlbum.setText(currentSong.album)

        // Select title text ready for editing
        binding.etSongTitle.post {
            binding.etSongTitle.setSelection(binding.etSongTitle.text.length)
        }

        binding.btnCloseEdit.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnCancelEdit.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnSaveEdit.setOnClickListener {
            val newTitle = binding.etSongTitle.text.toString().trim()
            if (newTitle.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.toast_song_title_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newArtist = binding.etSongArtist.text.toString().trim().ifEmpty { "Unknown" }
            val newAlbum = binding.etSongAlbum.text.toString().trim().ifEmpty { "Unknown" }

            val mainViewModel = try {
                ViewModelProvider(requireActivity())[MainViewModel::class.java]
            } catch (_: Exception) {
                null
            }

            EditSongManager.saveEdit(
                context = requireContext().applicationContext,
                song = currentSong,
                newTitle = newTitle,
                newArtist = newArtist,
                newAlbum = newAlbum,
                mainViewModel = mainViewModel
            )

            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
