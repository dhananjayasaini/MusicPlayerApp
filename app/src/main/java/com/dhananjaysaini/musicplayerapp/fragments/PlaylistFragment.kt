package com.dhananjaysaini.musicplayerapp.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlaylistSongsActivity
import com.dhananjaysaini.musicplayerapp.adapter.PlaylistAdapter
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.databinding.FragmentPlaylistBinding
import com.dhananjaysaini.musicplayerapp.model.PlaylistEntity
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.viewmodel.FavouriteViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.PlaylistViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlaylistFragment : Fragment(R.layout.fragment_playlist) {

    private lateinit var binding: FragmentPlaylistBinding
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var playlistViewModel: PlaylistViewModel
    private lateinit var favouriteViewModel: FavouriteViewModel
    private lateinit var mainViewModel: MainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentPlaylistBinding.bind(view)

        playlistViewModel = ViewModelProvider(requireActivity()).get(PlaylistViewModel::class.java)
        favouriteViewModel = ViewModelProvider(requireActivity()).get(FavouriteViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        val db = MusicDatabase.getDatabase(requireContext())

        playlistAdapter = PlaylistAdapter(arrayListOf())

        binding.playlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.playlistRecycler.adapter = playlistAdapter

        // Total count & created playlists
        playlistViewModel.playlists.observe(viewLifecycleOwner) {
            val ordered = it.sortedByDescending { entity -> entity.createdAt }
            playlistAdapter.update(ordered)

            val total = 4 + it.size
            binding.totalPlaylist.text = LocaleHelper.getPlaylistCountString(requireContext(), total)
            binding.tvMyPlaylistsHeader.text = getString(R.string.my_playlists_header, it.size)
        }

        // Preset 1: Favourite count
        favouriteViewModel.favouriteIds.observe(viewLifecycleOwner) { ids ->
            binding.tvFavCount.text = LocaleHelper.getSongCountString(requireContext(), ids.size)
        }

        // Preset 2: Recently Added count
        mainViewModel.musicListLiveData.observe(viewLifecycleOwner) { list ->
            binding.tvRecentAddedCount.text = LocaleHelper.getSongCountString(requireContext(), list.size)
        }

        // Preset 3: Recently Played count
        db.playHistoryDao().getRecentlyPlayed().observe(viewLifecycleOwner) { history ->
            binding.tvRecentPlayedCount.text = LocaleHelper.getSongCountString(requireContext(), history.size)
        }

        // Preset 4: Top Tracks count
        db.playHistoryDao().getTopPlayed().observe(viewLifecycleOwner) { history ->
            binding.tvTopTracksCount.text = LocaleHelper.getSongCountString(requireContext(), history.size)
        }

        // Click: Preset Rows
        binding.rowMyFavourite.setOnClickListener {
            val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
                putExtra("playlistName", getString(R.string.my_favourite))
                putExtra("playlistType", "favourite")
            }
            startActivity(intent)
        }

        binding.rowRecentlyAdded.setOnClickListener {
            val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
                putExtra("playlistName", getString(R.string.recently_added))
                putExtra("playlistType", "recently_added")
            }
            startActivity(intent)
        }

        binding.rowRecentlyPlayed.setOnClickListener {
            val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
                putExtra("playlistName", getString(R.string.recently_played))
                putExtra("playlistType", "recently_played")
            }
            startActivity(intent)
        }

        binding.rowTopTracks.setOnClickListener {
            val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
                putExtra("playlistName", getString(R.string.my_top_tracks))
                putExtra("playlistType", "top_tracks")
            }
            startActivity(intent)
        }

        // Clicks: Create & Restore
        binding.btnCreatePlaylist.setOnClickListener {
            showCreateDialog()
        }

        binding.rowCreatePlaylist.setOnClickListener {
            showCreateDialog()
        }

        binding.btnRestorePlaylist.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Clicks: 3 dots menus
        binding.btnPlaylistMoreTop.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuFav.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuRecentAdded.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuRecentPlayed.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuTopTracks.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show()
        }

        playlistAdapter.onRenameClick = { playlist ->
            showRenameDialog(playlist)
        }

        openPlaylistSongs()
        deletePlaylist()
    }

    private fun openPlaylistSongs() {

        playlistAdapter.onItemClick = { playlist ->
            val intent = Intent(requireContext(), PlaylistSongsActivity::class.java)
            intent.putExtra("playlistId", playlist.songId)
            intent.putExtra("playlistName", playlist.name)

            startActivity(intent)
        }
    }

    private fun showCreateDialog() {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.playlist_name_hint)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_playlist)
            .setView(editText)
            .setPositiveButton(R.string.btn_create) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    playlistViewModel.createPlaylist(name)
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Color.BLACK)

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Color.BLACK)
    }

    private fun deletePlaylist(){
        playlistAdapter.onDeleteClick = { playlist ->

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_playlist_title)
                .setMessage(getString(R.string.delete_playlist_prompt, playlist.name))
                .setPositiveButton(R.string.btn_delete) { _, _ ->
                    playlistViewModel.deletePlaylist(playlist.songId)
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Color.BLACK)

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(Color.BLACK)
        }
    }

    private fun showRenameDialog(playlist: PlaylistEntity) {

        val editText = EditText(requireContext()).apply {
            setText(playlist.name)
            setSelection(text.length)
        }

       val dialog =  MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename_playlist)
            .setView(editText)
            .setPositiveButton(R.string.btn_rename) { _, _ ->

                val editName = editText.text.toString().trim()

                if (editName.isNotEmpty() && editName != playlist.name) {
                    playlistViewModel.renamePlaylist(
                        playlist.songId,
                        editName
                    )
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Color.BLACK)

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Color.BLACK)
    }


}


