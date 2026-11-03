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
            binding.totalPlaylist.text = when (total) {
                1 -> "1 Playlist"
                else -> "$total Playlists"
            }
            binding.tvMyPlaylistsHeader.text = "My playlists (${it.size})"
        }

        // Preset 1: Favourite count
        favouriteViewModel.favouriteIds.observe(viewLifecycleOwner) { ids ->
            val count = ids.size
            binding.tvFavCount.text = when (count) {
                1 -> "1 song"
                else -> "$count songs"
            }
        }

        // Preset 2: Recently Added count
        mainViewModel.musicListLiveData.observe(viewLifecycleOwner) { list ->
            val count = list.size
            binding.tvRecentAddedCount.text = when (count) {
                1 -> "1 song"
                else -> "$count songs"
            }
        }

        // Preset 3: Recently Played count
        db.playHistoryDao().getRecentlyPlayed().observe(viewLifecycleOwner) { history ->
            val count = history.size
            binding.tvRecentPlayedCount.text = when (count) {
                1 -> "1 song"
                else -> "$count songs"
            }
        }

        // Preset 4: Top Tracks count
        db.playHistoryDao().getTopPlayed().observe(viewLifecycleOwner) { history ->
            val count = history.size
            binding.tvTopTracksCount.text = when (count) {
                1 -> "1 song"
                else -> "$count songs"
            }
        }

        // Click: Preset Rows
        binding.rowMyFavourite.setOnClickListener {
            val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
                putExtra("playlistName", "My favourite")
                putExtra("playlistType", "favourite")
            }
            startActivity(intent)
        }

        binding.rowRecentlyAdded.setOnClickListener {
            val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
                putExtra("playlistName", "Recently added")
                putExtra("playlistType", "recently_added")
            }
            startActivity(intent)
        }

        binding.rowRecentlyPlayed.setOnClickListener {
            val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
                putExtra("playlistName", "Recently played")
                putExtra("playlistType", "recently_played")
            }
            startActivity(intent)
        }

        binding.rowTopTracks.setOnClickListener {
            val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
                putExtra("playlistName", "My top tracks")
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
            Toast.makeText(requireContext(), "Restore playlist coming soon", Toast.LENGTH_SHORT).show()
        }

        // Clicks: 3 dots menus
        binding.btnPlaylistMoreTop.setOnClickListener {
            Toast.makeText(requireContext(), "Playlist options coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuFav.setOnClickListener {
            Toast.makeText(requireContext(), "Favourite options coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuRecentAdded.setOnClickListener {
            Toast.makeText(requireContext(), "Recently added options coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuRecentPlayed.setOnClickListener {
            Toast.makeText(requireContext(), "Recently played options coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuTopTracks.setOnClickListener {
            Toast.makeText(requireContext(), "Top tracks options coming soon", Toast.LENGTH_SHORT).show()
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
        val editText = EditText(requireContext())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Playlist")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                playlistViewModel.createPlaylist(editText.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Color.BLACK)

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Color.BLACK)
    }

    private fun deletePlaylist(){
        playlistAdapter.onDeleteClick = { playlist ->

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete playlist")
                .setMessage("Delete '${playlist.name}'?")
                .setPositiveButton("Delete") { _, _ ->
                    playlistViewModel.deletePlaylist(playlist.songId)
                }
                .setNegativeButton("Cancel", null)
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
            .setTitle("Rename playlist")
            .setView(editText)
            .setPositiveButton("Rename") { _, _ ->

                val editName = editText.text.toString().trim()

                if (editName.isNotEmpty() && editName != playlist.name) {
                    playlistViewModel.renamePlaylist(
                        playlist.songId,
                        editName
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Color.BLACK)

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Color.BLACK)
    }


}


