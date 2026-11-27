package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.PlaylistSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.databinding.FragmentPlaylistSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.CreatePlaylistDialog
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.PlaylistFolderOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.SongOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.viewmodel.FavouriteViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.PlaylistViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlaylistSongsFragment : Fragment(R.layout.fragment_playlist_songs) {

    private lateinit var binding: FragmentPlaylistSongsBinding
    private lateinit var mainVM: MainViewModel
    private lateinit var playlistVM: PlaylistViewModel
    private lateinit var favouriteVM: FavouriteViewModel
    private lateinit var playlistSongsAdapter: PlaylistSongsAdapter
    private val songs = ArrayList<Music>()
    private var currentSongIds: List<String> = emptyList()

    private var isSearchMode = false
    private var currentSearchQuery = ""
    private lateinit var searchBackCallback: OnBackPressedCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentPlaylistSongsBinding.bind(view)

        playlistVM = ViewModelProvider(requireActivity()).get(PlaylistViewModel::class.java)
        mainVM = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        favouriteVM = ViewModelProvider(requireActivity()).get(FavouriteViewModel::class.java)

        val playlistType = arguments?.getString("playlistType")
        val playlistId = arguments?.getInt("playlistId", -1) ?: -1
        val playlistName = when (playlistType) {
            "favourite" -> getString(R.string.my_favourite)
            "recently_added" -> getString(R.string.recently_added)
            "recently_played" -> getString(R.string.recently_played)
            "top_tracks" -> getString(R.string.my_top_tracks)
            else -> arguments?.getString("playlistName") ?: getString(R.string.tab_playlists)
        }

        playlistSongsAdapter = PlaylistSongsAdapter(requireContext(), ArrayList(songs))
        playlistSongsAdapter.attachLifecycle(viewLifecycleOwner)

        binding.playlistSongsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.playlistSongsRecycler.adapter = playlistSongsAdapter

        playlistSongsAdapter.onItemClick = { list: ArrayList<Music>, position: Int ->
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

        // Inside playlist folder's audio 3 dots -> Show audio bottom sheet in list form with all functionalities
        playlistSongsAdapter.onMenuClick = { song, pos ->
            val favIds = favouriteVM.favouriteIds.value ?: emptyList()
            val isFav = song.id in favIds
            val isCustom = playlistId != -1
            val isFavPlaylist = playlistType == "favourite"

            val config = SongMenuConfig(
                play = true,
                playNext = true,
                addToQueue = true,
                delete = true,
                edit = true,
                addToFav = !isFav && !isFavPlaylist,
                removeFromFav = isFav || isFavPlaylist,
                addToPlaylist = true,
                removeFromPlaylist = isCustom,
                share = true
            )

            val sheet = SongOptionsBottomSheet(song, pos, config)
            if (isCustom) {
                sheet.onRemoveFromPlaylist = { s ->
                    playlistVM.removeSongFromPlaylist(playlistId, s.id)
                    Toast.makeText(requireContext(), getString(R.string.removed_from_playlist_msg), Toast.LENGTH_SHORT).show()
                }
            }
            sheet.onSongHidden = { s ->
                songs.removeAll { it.id == s.id }
                refreshDisplayedSongs()
            }
            sheet.show(parentFragmentManager, "playlist_song_options_${song.id}")
        }

        binding.btnPlaylistShuffle.setOnClickListener {
            if (songs.isNotEmpty()) {
                val shuffled = ArrayList(songs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnPlaylistPlay.setOnClickListener {
            if (songs.isNotEmpty()) {
                playSong(ArrayList(songs), 0)
            }
        }

        binding.playlistTitle.text = playlistName

        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnAddSongs.setOnClickListener {
            openAddSongsSheet()
        }

        setupSearch()

        // Top-right 3 dots on playlist detail screen
        binding.btnPlaylistOptions.setOnClickListener {
            val iconRes = when (playlistType) {
                "favourite" -> R.drawable.ic_neon_favourite
                "recently_added" -> R.drawable.ic_neon_recent_add
                "recently_played" -> R.drawable.ic_neon_timer
                "top_tracks" -> R.drawable.ic_neon_fire
                else -> R.drawable.ic_neon_playlist_card
            }
            val bgRes = when (playlistType) {
                "favourite" -> R.drawable.bg_preset_fav
                "recently_added" -> R.drawable.bg_preset_recent_add
                "recently_played" -> R.drawable.bg_preset_recent_play
                "top_tracks" -> R.drawable.bg_preset_top
                else -> R.drawable.bg_dialog_playlist_icon
            }
            val isCustom = playlistId != -1
            val currentTitle = binding.playlistTitle.text.toString()
            val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                title = currentTitle,
                iconRes = iconRes,
                bgDrawableRes = bgRes,
                songs = songs,
                isCustomPlaylist = isCustom
            )
            if (isCustom || playlistType == "favourite") {
                sheet.onAddSongsClick = {
                    openAddSongsSheet()
                }
            }
            if (isCustom) {
                sheet.onRenameClick = {
                    val existingNames = playlistVM.playlists.value?.map { it.name } ?: emptyList()
                    CreatePlaylistDialog.show(
                        context = requireContext(),
                        titleRes = R.string.rename_playlist,
                        initialName = currentTitle,
                        positiveBtnRes = R.string.btn_rename,
                        existingNames = existingNames
                    ) { newName ->
                        if (newName != currentTitle) {
                            playlistVM.renamePlaylist(playlistId, newName)
                            binding.playlistTitle.text = newName
                        }
                    }
                }
                sheet.onDeleteClick = {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete_playlist_title)
                        .setMessage(getString(R.string.delete_playlist_prompt, currentTitle))
                        .setPositiveButton(R.string.btn_delete) { _, _ ->
                            playlistVM.deletePlaylist(playlistId)
                            requireActivity().finish()
                        }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
                }
            }
            sheet.show(parentFragmentManager, "folder_options_details")
        }

        when (playlistType) {
            "favourite" -> {
                binding.playlistCardBox.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_preset_fav)
                binding.playlistCardIcon.setImageResource(R.drawable.ic_neon_favourite)
                binding.playlistCardIcon.imageTintList = null

                favouriteVM.favouriteIds.observe(viewLifecycleOwner) { ids ->
                    val all = MusicService.allSongs.ifEmpty { mainVM.musicListLiveData.value ?: emptyList() }
                    songs.clear()
                    songs.addAll(all.filter { it.id in ids })
                    refreshDisplayedSongs()
                }
            }
            "recently_added" -> {
                binding.playlistCardBox.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_preset_recent_add)
                binding.playlistCardIcon.setImageResource(R.drawable.ic_neon_recent_add)
                binding.playlistCardIcon.imageTintList = null

                mainVM.musicListLiveData.observe(viewLifecycleOwner) { all ->
                    songs.clear()
                    songs.addAll(all.sortedByDescending { it.date })
                    refreshDisplayedSongs()
                }
            }
            "recently_played" -> {
                binding.playlistCardBox.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_preset_recent_play)
                binding.playlistCardIcon.setImageResource(R.drawable.ic_neon_timer)
                binding.playlistCardIcon.imageTintList = null

                MusicDatabase.getDatabase(requireContext()).playHistoryDao().getRecentlyPlayed()
                    .observe(viewLifecycleOwner) { history ->
                        val all = MusicService.allSongs.ifEmpty { mainVM.musicListLiveData.value ?: emptyList() }
                        val songMap = all.associateBy { it.id }
                        songs.clear()
                        history.forEach { item ->
                            songMap[item.songId]?.let { songs.add(it) }
                        }
                        refreshDisplayedSongs()
                    }
            }
            "top_tracks" -> {
                binding.playlistCardBox.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_preset_top)
                binding.playlistCardIcon.setImageResource(R.drawable.ic_neon_fire)
                binding.playlistCardIcon.imageTintList = null

                MusicDatabase.getDatabase(requireContext()).playHistoryDao().getTopPlayed()
                    .observe(viewLifecycleOwner) { history ->
                        val all = MusicService.allSongs.ifEmpty { mainVM.musicListLiveData.value ?: emptyList() }
                        val songMap = all.associateBy { it.id }
                        songs.clear()
                        history.forEach { item ->
                            songMap[item.songId]?.let { songs.add(it) }
                        }
                        refreshDisplayedSongs()
                    }
            }
            else -> {
                // Custom playlist
                binding.playlistCardIcon.setImageResource(R.drawable.ic_neon_playlist_card)
                binding.playlistCardIcon.imageTintList = null
                if (playlistId != -1) {
                    playlistVM.getPlaylistSongIds(playlistId).observe(viewLifecycleOwner) { ids ->
                        currentSongIds = ids
                        updateSongs()
                    }
                    mainVM.musicListLiveData.observe(viewLifecycleOwner) {
                        updateSongs()
                    }
                    updateTitle()
                    removeSong()
                }
            }
        }
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

        binding.btnPlaylistSearch.setOnClickListener {
            openSearchMode()
        }

        binding.btnCloseSearch.setOnClickListener {
            closeSearchMode()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etPlaylistSearch.setText("")
            binding.etPlaylistSearch.requestFocus()
        }

        binding.etPlaylistSearch.doAfterTextChanged { editable ->
            if (isSearchMode) {
                applySearchFilter(editable?.toString().orEmpty())
            }
        }

        binding.etPlaylistSearch.setOnEditorActionListener { _, actionId, _ ->
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
        binding.playlistHeaderContainer.visibility = View.GONE
        binding.tvSearchResultCount.visibility = View.VISIBLE

        binding.etPlaylistSearch.requestFocus()
        binding.etPlaylistSearch.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etPlaylistSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        applySearchFilter(currentSearchQuery)
    }

    private fun closeSearchMode() {
        isSearchMode = false
        currentSearchQuery = ""
        searchBackCallback.isEnabled = false

        hideKeyboard()
        binding.etPlaylistSearch.setText("")
        binding.etPlaylistSearch.clearFocus()

        binding.topBarSearch.visibility = View.GONE
        binding.topBarNormal.visibility = View.VISIBLE
        binding.playlistHeaderContainer.visibility = View.VISIBLE
        binding.tvSearchResultCount.visibility = View.GONE
        binding.layoutEmptySearch.visibility = View.GONE
        binding.playlistSongsRecycler.visibility = View.VISIBLE

        playlistSongsAdapter.updateList(ArrayList(songs))
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

        playlistSongsAdapter.updateList(filtered)

        if (isSearchMode) {
            binding.tvSearchResultCount.text = LocaleHelper.getSongCountString(requireContext(), filtered.size)
            if (filtered.isEmpty()) {
                binding.playlistSongsRecycler.visibility = View.GONE
                binding.layoutEmptySearch.visibility = View.VISIBLE
                binding.tvEmptySearchMsg.text = if (trimmed.isNotEmpty()) {
                    getString(R.string.no_songs_matching, trimmed)
                } else {
                    getString(R.string.no_songs_found)
                }
            } else {
                binding.playlistSongsRecycler.visibility = View.VISIBLE
                binding.layoutEmptySearch.visibility = View.GONE
            }
        }
    }

    private fun refreshDisplayedSongs() {
        binding.playlistSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)
        if (isSearchMode) {
            applySearchFilter(currentSearchQuery)
        } else {
            playlistSongsAdapter.updateList(ArrayList(songs))
            if (songs.isEmpty()) {
                binding.layoutEmptyPlaylist.visibility = View.VISIBLE
                binding.playlistSongsRecycler.visibility = View.GONE
                binding.playlistSongCount.visibility = View.GONE
                binding.rowPlaylistActions.visibility = View.GONE
            } else {
                binding.layoutEmptyPlaylist.visibility = View.GONE
                binding.playlistSongsRecycler.visibility = View.VISIBLE
                binding.playlistSongCount.visibility = View.VISIBLE
                binding.rowPlaylistActions.visibility = View.VISIBLE
            }
        }
    }

    private fun openAddSongsSheet() {
        val excludedIds = songs.map { it.id }
        val addSheet = com.dhananjaysaini.musicplayerapp.utils.AddSongsBottomSheet.newInstance(excludedIds)
        addSheet.onSongsSelected = { selectedSongs ->
            val playlistType = arguments?.getString("playlistType")
            val playlistId = arguments?.getInt("playlistId", -1) ?: -1
            val context = requireContext()

            if (playlistType == "favourite") {
                for (song in selectedSongs) {
                    com.dhananjaysaini.musicplayerapp.utils.FavouriteManager.addFavourite(song)
                    favouriteVM.setFavourite(song.id, true)
                }
                Toast.makeText(context, getString(R.string.songs_added_success, selectedSongs.size), Toast.LENGTH_SHORT).show()
            } else if (playlistId != -1) {
                for (song in selectedSongs) {
                    playlistVM.addSongToPlaylist(playlistId, song.id)
                }
                Toast.makeText(context, getString(R.string.songs_added_success, selectedSongs.size), Toast.LENGTH_SHORT).show()
            }
        }
        addSheet.show(parentFragmentManager, "AddSongsBottomSheet")
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etPlaylistSearch.windowToken, 0)
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
        startActivity(
            Intent(requireContext(), PlayerActivity::class.java)
                .putExtra("source", "playlist")
        )
    }

    private fun updateSongs() {
        val allSongs = MusicService.allSongs

        if (currentSongIds.isEmpty()) {
            songs.clear()
            refreshDisplayedSongs()
            return
        }

        songs.clear()
        songs.addAll(allSongs.filter { it.id in currentSongIds })

        refreshDisplayedSongs()
    }

    private fun removeSong() {
        playlistSongsAdapter.onRemoveFromPlaylist = { song: Music ->
            val playlistId = requireArguments().getInt("playlistId")
            playlistVM.removeSongFromPlaylist(
                playlistId = playlistId,
                songId = song.id
            )
        }
    }

    private fun updateTitle() {
        val playlistId = requireArguments().getInt("playlistId")
        playlistVM.getPlaylistById(playlistId)
            .observe(viewLifecycleOwner) { playlist ->
                if (playlist != null) {
                    binding.playlistTitle.text = playlist.name
                }
            }
    }

    override fun onResume() {
        super.onResume()
        playlistSongsAdapter.updatePlayingState(
            MusicService.song?.id,
            MusicService.mediaPlayer?.isPlaying == true
        )
    }
}
