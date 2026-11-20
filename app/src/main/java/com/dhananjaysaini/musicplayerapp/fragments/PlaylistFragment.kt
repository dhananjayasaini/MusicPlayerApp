package com.dhananjaysaini.musicplayerapp.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlaylistSongsActivity
import com.dhananjaysaini.musicplayerapp.adapter.PlaylistAdapter
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.databinding.FragmentPlaylistBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.PlaylistEntity
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.CreatePlaylistDialog
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.PlaylistFolderOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.viewmodel.FavouriteViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.PlaylistViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PlaylistFragment : Fragment(R.layout.fragment_playlist) {

    private lateinit var binding: FragmentPlaylistBinding
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var playlistViewModel: PlaylistViewModel
    private lateinit var favouriteViewModel: FavouriteViewModel
    private lateinit var mainViewModel: MainViewModel

    private val recentPlayedSongs = ArrayList<Music>()
    private val topTracksSongs = ArrayList<Music>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentPlaylistBinding.bind(view)

        playlistViewModel = ViewModelProvider(requireActivity()).get(PlaylistViewModel::class.java)
        favouriteViewModel = ViewModelProvider(requireActivity()).get(FavouriteViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        val db = MusicDatabase.getDatabase(requireContext())

        playlistAdapter = PlaylistAdapter(arrayListOf(), showMenu = true)

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
            val all = MusicService.allSongs.ifEmpty { mainViewModel.musicListLiveData.value ?: emptyList() }
            val map = all.associateBy { it.id }
            recentPlayedSongs.clear()
            history.forEach { item ->
                map[item.songId]?.let { recentPlayedSongs.add(it) }
            }
            binding.tvRecentPlayedCount.text = LocaleHelper.getSongCountString(requireContext(), recentPlayedSongs.size)
        }

        // Preset 4: Top Tracks count
        db.playHistoryDao().getTopPlayed().observe(viewLifecycleOwner) { history ->
            val all = MusicService.allSongs.ifEmpty { mainViewModel.musicListLiveData.value ?: emptyList() }
            val map = all.associateBy { it.id }
            topTracksSongs.clear()
            history.forEach { item ->
                map[item.songId]?.let { topTracksSongs.add(it) }
            }
            binding.tvTopTracksCount.text = LocaleHelper.getSongCountString(requireContext(), topTracksSongs.size)
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

        // Clicks: 3 dots menus for folder presets
        binding.btnPlaylistMoreTop.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.toast_coming_soon), Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuFav.setOnClickListener {
            val songs = getFavouriteSongs()
            val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                title = getString(R.string.my_favourite),
                iconRes = R.drawable.ic_neon_favourite,
                bgDrawableRes = R.drawable.bg_preset_fav,
                songs = songs,
                isCustomPlaylist = false
            )
            sheet.show(parentFragmentManager, "folder_fav")
        }

        binding.btnMenuRecentAdded.setOnClickListener {
            val songs = getRecentAddedSongs()
            val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                title = getString(R.string.recently_added),
                iconRes = R.drawable.ic_neon_recent_add,
                bgDrawableRes = R.drawable.bg_preset_recent_add,
                songs = songs,
                isCustomPlaylist = false
            )
            sheet.show(parentFragmentManager, "folder_recent_add")
        }

        binding.btnMenuRecentPlayed.setOnClickListener {
            val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                title = getString(R.string.recently_played),
                iconRes = R.drawable.ic_neon_timer,
                bgDrawableRes = R.drawable.bg_preset_recent_play,
                songs = recentPlayedSongs,
                isCustomPlaylist = false
            )
            sheet.show(parentFragmentManager, "folder_recent_play")
        }

        binding.btnMenuTopTracks.setOnClickListener {
            val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                title = getString(R.string.my_top_tracks),
                iconRes = R.drawable.ic_neon_fire,
                bgDrawableRes = R.drawable.bg_preset_top,
                songs = topTracksSongs,
                isCustomPlaylist = false
            )
            sheet.show(parentFragmentManager, "folder_top_tracks")
        }

        playlistAdapter.onMenuClick = { playlist ->
            viewLifecycleOwner.lifecycleScope.launch {
                val songIds = playlistViewModel.getSongIdsList(playlist.songId)
                val all = MusicService.allSongs.ifEmpty { mainViewModel.musicListLiveData.value ?: emptyList() }
                val pSongs = all.filter { it.id in songIds }
                val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                    title = playlist.name,
                    iconRes = R.drawable.ic_neon_playlist_card,
                    bgDrawableRes = R.drawable.bg_dialog_playlist_icon,
                    songs = pSongs,
                    isCustomPlaylist = true
                )
                sheet.onRenameClick = {
                    showRenameDialog(playlist)
                }
                sheet.onDeleteClick = {
                    deletePlaylist(playlist)
                }
                sheet.show(parentFragmentManager, "folder_custom_${playlist.songId}")
            }
        }

        playlistAdapter.onRenameClick = { playlist ->
            showRenameDialog(playlist)
        }

        playlistAdapter.onDeleteClick = { playlist ->
            deletePlaylist(playlist)
        }

        openPlaylistSongs()
    }

    private fun getFavouriteSongs(): List<Music> {
        val ids = favouriteViewModel.favouriteIds.value ?: emptyList()
        val all = MusicService.allSongs.ifEmpty { mainViewModel.musicListLiveData.value ?: emptyList() }
        return all.filter { it.id in ids }
    }

    private fun getRecentAddedSongs(): List<Music> {
        val all = MusicService.allSongs.ifEmpty { mainViewModel.musicListLiveData.value ?: emptyList() }
        return all.sortedByDescending { it.date }
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
        val existingNames = playlistViewModel.playlists.value?.map { it.name } ?: emptyList()
        CreatePlaylistDialog.show(
            context = requireContext(),
            titleRes = R.string.create_playlist,
            positiveBtnRes = R.string.btn_create,
            existingNames = existingNames
        ) { name ->
            playlistViewModel.createPlaylist(name)
            Toast.makeText(
                requireContext(),
                getString(R.string.playlist_created_msg),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deletePlaylist(playlist: PlaylistEntity) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_playlist_title)
            .setMessage(getString(R.string.delete_playlist_prompt, playlist.name))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                playlistViewModel.deletePlaylist(playlist.songId)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(Color.parseColor("#FF5252"))

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(Color.WHITE)
    }

    private fun showRenameDialog(playlist: PlaylistEntity) {
        val existingNames = playlistViewModel.playlists.value?.map { it.name } ?: emptyList()
        CreatePlaylistDialog.show(
            context = requireContext(),
            titleRes = R.string.rename_playlist,
            initialName = playlist.name,
            positiveBtnRes = R.string.btn_rename,
            existingNames = existingNames
        ) { editName ->
            if (editName != playlist.name) {
                playlistViewModel.renamePlaylist(
                    playlist.songId,
                    editName
                )
            }
        }
    }
}
