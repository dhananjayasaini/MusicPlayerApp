package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.FolderSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentFolderSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.FavouriteManager
import com.dhananjaysaini.musicplayerapp.utils.HiddenSongManager
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.PlaylistFolderOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.SongOptionsBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class FolderSongsFragment : Fragment(R.layout.fragment_folder_songs) {

    private lateinit var binding: FragmentFolderSongsBinding
    private lateinit var folderSongsAdapter: FolderSongsAdapter

    private val songs = ArrayList<Music>()

    private var isSearchMode = false
    private var currentSearchQuery = ""
    private lateinit var searchBackCallback: OnBackPressedCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFolderSongsBinding.bind(view)
        binding.folderCardIcon.imageTintList = null

        val folderPath = requireArguments().getString("folderPath") ?: return
        val folderName = requireArguments().getString("folderName")
            ?: File(folderPath).name.ifEmpty { getString(R.string.tab_folders) }

        binding.folderTitle.text = folderName

        songs.addAll(
            MusicService.allSongs.filter {
                it.path.startsWith(folderPath)
            }
        )

        folderSongsAdapter = FolderSongsAdapter(requireContext(), ArrayList(songs))
        folderSongsAdapter.attachLifecycle(viewLifecycleOwner)

        binding.folderSongsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.folderSongsRecycler.adapter = folderSongsAdapter

        folderSongsAdapter.onItemClick = { list, position ->
            val clickedSong = list.getOrNull(position)
            if (isSearchMode && clickedSong != null) {
                hideKeyboard()
                val realIndex = songs.indexOfFirst { it.id == clickedSong.id }
                if (realIndex >= 0) {
                    playSong(ArrayList(songs), realIndex)
                } else {
                    playSong(ArrayList(list), position)
                }
            } else {
                playSong(ArrayList(list), position)
            }
        }

        folderSongsAdapter.onMenuClick = { song, pos ->
            val isFav = FavouriteManager.isFavourite(song)
            val config = SongMenuConfig(
                play = true,
                playNext = true,
                addToQueue = true,
                delete = true,
                edit = true,
                addToFav = !isFav,
                removeFromFav = isFav,
                addToPlaylist = true,
                removeFromPlaylist = false,
                setAsRingtone = true,
                goToAlbum = true,
                hideSong = true,
                share = true
            )

            val sheet = SongOptionsBottomSheet(song, pos, config)
            sheet.onSongHidden = { s ->
                songs.removeAll { it.id == s.id }
                binding.folderSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)
                if (isSearchMode) {
                    applySearchFilter(currentSearchQuery)
                } else {
                    folderSongsAdapter.updateList(ArrayList(songs))
                }
            }
            sheet.show(parentFragmentManager, "folder_song_menu_${song.id}")
        }

        binding.btnFolderShuffle.setOnClickListener {
            if (songs.isNotEmpty()) {
                val shuffled = ArrayList(songs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnFolderPlay.setOnClickListener {
            if (songs.isNotEmpty()) {
                playSong(ArrayList(songs), 0)
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnFolderOptions.setOnClickListener {
            val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                title = folderName,
                iconRes = R.drawable.ic_neon_folder,
                bgDrawableRes = R.drawable.bg_dialog_playlist_icon,
                songs = songs,
                isCustomPlaylist = false,
                showHideFolder = true
            )
            sheet.onHideFolderClick = {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.hide_folder_dialog_title)
                    .setMessage(getString(R.string.hide_folder_dialog_msg, folderName))
                    .setPositiveButton(R.string.action_hide_folder) { _, _ ->
                        HiddenSongManager.hideFolder(requireContext(), folderPath)
                        Toast.makeText(requireContext(), getString(R.string.toast_folder_hidden), Toast.LENGTH_SHORT).show()
                        requireActivity().finish()
                    }
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show()
            }
            sheet.show(parentFragmentManager, "folder_options_details")
        }

        setupSearch()
        updateSongs(folderPath)
    }

    private fun setupSearch() {
        searchBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (isSearchMode) {
                    closeSearchMode()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, searchBackCallback)

        binding.btnFolderSearch.setOnClickListener {
            openSearchMode()
        }

        binding.btnCloseSearch.setOnClickListener {
            closeSearchMode()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etFolderSearch.setText("")
            binding.etFolderSearch.requestFocus()
        }

        binding.etFolderSearch.doAfterTextChanged { editable ->
            if (isSearchMode) {
                applySearchFilter(editable?.toString().orEmpty())
            }
        }

        binding.etFolderSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun openSearchMode() {
        isSearchMode = true
        searchBackCallback.isEnabled = true

        binding.topBarNormal.visibility = View.GONE
        binding.topBarSearch.visibility = View.VISIBLE
        binding.folderHeaderContainer.visibility = View.GONE
        binding.tvSearchResultCount.visibility = View.VISIBLE

        binding.etFolderSearch.requestFocus()
        binding.etFolderSearch.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etFolderSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        applySearchFilter(currentSearchQuery)
    }

    private fun closeSearchMode() {
        isSearchMode = false
        currentSearchQuery = ""
        searchBackCallback.isEnabled = false

        hideKeyboard()
        binding.etFolderSearch.setText("")
        binding.etFolderSearch.clearFocus()

        binding.topBarSearch.visibility = View.GONE
        binding.topBarNormal.visibility = View.VISIBLE
        binding.folderHeaderContainer.visibility = View.VISIBLE
        binding.tvSearchResultCount.visibility = View.GONE
        binding.layoutEmptySearch.visibility = View.GONE
        binding.folderSongsRecycler.visibility = View.VISIBLE

        folderSongsAdapter.updateList(ArrayList(songs))
    }

    private fun applySearchFilter(query: String) {
        currentSearchQuery = query
        val trimmed = query.trim()

        binding.btnClearSearch.visibility = if (trimmed.isNotEmpty()) View.VISIBLE else View.GONE

        val filtered = if (trimmed.isEmpty()) {
            ArrayList(songs)
        } else {
            ArrayList(
                songs.filter { song ->
                    song.title.contains(trimmed, ignoreCase = true) ||
                        song.artist.contains(trimmed, ignoreCase = true) ||
                        song.album.contains(trimmed, ignoreCase = true)
                }
            )
        }

        folderSongsAdapter.updateList(filtered)

        if (isSearchMode) {
            binding.tvSearchResultCount.text = LocaleHelper.getSongCountString(requireContext(), filtered.size)
            if (filtered.isEmpty()) {
                binding.folderSongsRecycler.visibility = View.GONE
                binding.layoutEmptySearch.visibility = View.VISIBLE
                binding.tvEmptySearchMsg.text = if (trimmed.isNotEmpty()) {
                    getString(R.string.no_songs_matching, trimmed)
                } else {
                    getString(R.string.no_songs_found)
                }
            } else {
                binding.folderSongsRecycler.visibility = View.VISIBLE
                binding.layoutEmptySearch.visibility = View.GONE
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etFolderSearch.windowToken, 0)
    }

    private fun playSong(list: ArrayList<Music>, position: Int) {
        val mainActivity = activity as? com.dhananjaysaini.musicplayerapp.activities.MainActivity
        if (mainActivity != null) {
            mainActivity.playAudioTrack(list, position)
            return
        }
        val act = activity ?: return
        if (!com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper.isPermissionGranted(act)) {
            com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper.checkAndRequestOnAudioClick(act) {
                executeDirectPlay(list, position)
            }
            return
        }
        executeDirectPlay(list, position)
    }

    private fun executeDirectPlay(list: ArrayList<Music>, position: Int) {
        MusicService.playlist = ArrayList(list)
        MusicService.position = position

        requireContext().startService(
            Intent(requireContext(), MusicService::class.java).apply {
                action = Constants.ACTION_PLAY_AT
                putExtra("songPosition", position)
            }
        )

        requireContext().sendBroadcast(
            Intent("SHOW_MINI_PLAYER").setPackage(requireContext().packageName)
        )
        startActivity(Intent(requireContext(), PlayerActivity::class.java))
    }

    private fun updateSongs(folderPath: String) {
        val allSongs = MusicService.allSongs

        songs.clear()
        songs.addAll(
            allSongs.filter { it.path.startsWith(folderPath) }
        )

        binding.folderSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)
        if (isSearchMode) {
            applySearchFilter(currentSearchQuery)
        } else {
            folderSongsAdapter.updateList(ArrayList(songs))
        }
    }

    override fun onResume() {
        super.onResume()
        folderSongsAdapter.updatePlayingState(
            MusicService.song?.id,
            MusicService.mediaPlayer?.isPlaying == true
        )
    }
}
