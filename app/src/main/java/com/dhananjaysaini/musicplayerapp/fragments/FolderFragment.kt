package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.FolderSongsActivity
import com.dhananjaysaini.musicplayerapp.adapter.FolderAdapter
import com.dhananjaysaini.musicplayerapp.databinding.FragmentFolderBinding
import com.dhananjaysaini.musicplayerapp.model.SongFolder
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.HiddenSongManager
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.PlaylistFolderOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.viewmodel.FolderViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FolderFragment : Fragment(R.layout.fragment_folder) {

    private lateinit var binding: FragmentFolderBinding
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var folderViewModel: FolderViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFolderBinding.bind(view)

        folderViewModel = ViewModelProvider(this).get(FolderViewModel::class.java)
        folderAdapter = FolderAdapter(mutableListOf())

        val recycler = binding.folderRecycler
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = folderAdapter

        folderViewModel.folders.observe(viewLifecycleOwner) {
            folderAdapter.update(it)
            binding.totalFolder.text = LocaleHelper.getFolderCountString(requireContext(), it.size)
        }
        folderViewModel.loadFolders(requireContext())

        openFolderSongs()
        setupFolderMenu()
    }

    private fun openFolderSongs() {
        folderAdapter.onItemClick = { folder ->
            val intent = Intent(requireContext(), FolderSongsActivity::class.java).apply {
                putExtra("folderPath", folder.path)
                putExtra("folderName", folder.name)
                putExtra("playlistName", folder.name)
            }
            startActivity(intent)
        }
    }

    private fun setupFolderMenu() {
        folderAdapter.onMenuClick = { folder, _ ->
            val allSongs = MusicService.allSongs.ifEmpty {
                try {
                    ViewModelProvider(requireActivity()).get(MainViewModel::class.java).musicListLiveData.value ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val folderSongs = allSongs.filter { it.path.startsWith(folder.path) }
            val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                title = folder.name,
                iconRes = R.drawable.ic_neon_folder,
                bgDrawableRes = R.drawable.bg_dialog_playlist_icon,
                songs = folderSongs,
                isCustomPlaylist = false,
                showHideFolder = true
            )
            sheet.onHideFolderClick = {
                confirmHideFolder(folder)
            }
            sheet.show(parentFragmentManager, "folder_options_${folder.name}")
        }
    }

    private fun confirmHideFolder(folder: SongFolder) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.hide_folder_dialog_title)
            .setMessage(getString(R.string.hide_folder_dialog_msg, folder.name))
            .setPositiveButton(R.string.action_hide_folder) { _, _ ->
                HiddenSongManager.hideFolder(requireContext(), folder.path)
                folderViewModel.loadFolders(requireContext())
                try {
                    val mainVM = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
                    mainVM.loadMusic(requireContext(), force = true)
                } catch (e: Exception) {
                    // Ignore
                }
                Toast.makeText(requireContext(), getString(R.string.toast_folder_hidden), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
