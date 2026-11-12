package com.dhananjaysaini.musicplayerapp.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.dhananjaysaini.musicplayerapp.R
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.adapter.PlaylistAdapter
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetPlaylistPickerBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.viewmodel.PlaylistViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class PlaylistPickerBottomSheet(
    private val song: Music
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetPlaylistPickerBinding
    private lateinit var playListAdapter: PlaylistAdapter

    private val playlistViewModel: PlaylistViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = BottomSheetPlaylistPickerBinding.inflate(inflater, container, false)

        setupRecycler()
        observePlaylists()

        binding.btnCreatePlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }

        return binding.root
    }

    private fun setupRecycler() {

        playListAdapter = PlaylistAdapter(arrayListOf())

        binding.playlistRecycler.layoutManager =
            LinearLayoutManager(requireContext())

        binding.playlistRecycler.adapter = playListAdapter

        playListAdapter.onItemClick = { playlist ->

            playlistViewModel.addSongToPlaylist(
                playlist.songId,
                song.id
            )

            Toast.makeText(requireContext(), getString(R.string.toast_added_to_playlist, playlist.name),
                Toast.LENGTH_SHORT
            ).show()

            dismiss()
        }

        playListAdapter.onDeleteClick = {}
        playListAdapter.onRenameClick = {}
    }

    private fun observePlaylists() {

        playlistViewModel.playlists.observe(viewLifecycleOwner) { playlists ->

            playListAdapter.update(playlists)

        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun showCreatePlaylistDialog() {

        val editText = EditText(requireContext())
        editText.hint = getString(R.string.playlist_name_hint)

         val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_playlist)
            .setView(editText)
            .setPositiveButton(R.string.btn_create, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

            dialog.show()


        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Color.BLACK)

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Color.BLACK)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

            val name = editText.text.toString().trim()

            if (name.isEmpty()) {
                editText.error = getString(R.string.error_enter_playlist_name)
                return@setOnClickListener
            }

            val exists = playlistViewModel.playlists.value
                ?.any { it.name.equals(name, true) }

            if (exists == true) {
                editText.error = getString(R.string.error_playlist_exists)
                return@setOnClickListener
            }

            playlistViewModel.createPlaylist(name)

            Toast.makeText(
                requireContext(),
                getString(R.string.playlist_created_msg),
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }
    }
}