package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.GenreSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentGenreSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.repository.GenreRepository
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.FavouriteManager
import com.dhananjaysaini.musicplayerapp.utils.GenreOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.SongOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager

class GenreSongsFragment : Fragment(R.layout.fragment_genre_songs) {

    private lateinit var binding: FragmentGenreSongsBinding
    private lateinit var genreSongsAdapter: GenreSongsAdapter
    private val songs = ArrayList<Music>()

    private var isSearchMode = false
    private var currentSearchQuery = ""
    private lateinit var searchBackCallback: OnBackPressedCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentGenreSongsBinding.bind(view)

        val genreName = requireArguments().getString("genreName") ?: return

        binding.genreTitle.text = genreName

        val fetched = GenreRepository.getSongsForGenre(requireContext(), genreName, MusicService.allSongs)
        songs.addAll(fetched)

        ThemeManager.applyThemeToActivity(requireActivity())

        genreSongsAdapter = GenreSongsAdapter(requireContext(), songs)
        genreSongsAdapter.attachLifecycle(viewLifecycleOwner)

        val recycler = binding.genreSongsRecycler
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = genreSongsAdapter

        genreSongsAdapter.onItemClick = { list, position ->
            playSong(ArrayList(list), position)
        }

        genreSongsAdapter.onMenuClick = { song, pos ->
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
                setAsRingtone = true,
                goToAlbum = true,
                hideSong = true,
                share = true
            )
            val sheet = SongOptionsBottomSheet(song, pos, config)
            sheet.onSongHidden = { s ->
                songs.removeAll { it.id == s.id }
                binding.genreSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)
                if (isSearchMode) {
                    applySearchFilter(currentSearchQuery)
                } else {
                    genreSongsAdapter.updateList(ArrayList(songs))
                }
            }
            sheet.show(parentFragmentManager, "genre_song_menu_${song.id}")
        }

        binding.btnGenreShuffle.setOnClickListener {
            if (songs.isNotEmpty()) {
                val shuffled = ArrayList(songs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnGenrePlay.setOnClickListener {
            if (songs.isNotEmpty()) {
                playSong(ArrayList(songs), 0)
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnGenreMore.setOnClickListener {
            val sheet = GenreOptionsBottomSheet.newInstance(
                genreName = genreName,
                artUri = songs.firstOrNull()?.artUri,
                songs = songs
            )
            sheet.onGenreUpdated = {
                updateSongs(genreName)
            }
            sheet.show(parentFragmentManager, "genre_options_header")
        }

        setupSearch()
        updateSongs(genreName)
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

        binding.btnGenreSearch.setOnClickListener {
            openSearchMode()
        }

        binding.btnCloseSearch.setOnClickListener {
            closeSearchMode()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etGenreSearch.setText("")
            binding.etGenreSearch.requestFocus()
        }

        binding.etGenreSearch.doAfterTextChanged { editable ->
            if (isSearchMode) {
                applySearchFilter(editable?.toString().orEmpty())
            }
        }

        binding.etGenreSearch.setOnEditorActionListener { _, actionId, _ ->
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
        binding.genreHeaderContainer.visibility = View.GONE
        binding.tvSearchResultCount.visibility = View.VISIBLE

        binding.etGenreSearch.requestFocus()
        binding.etGenreSearch.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etGenreSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        applySearchFilter(currentSearchQuery)
    }

    private fun closeSearchMode() {
        isSearchMode = false
        currentSearchQuery = ""
        searchBackCallback.isEnabled = false

        hideKeyboard()
        binding.etGenreSearch.setText("")
        binding.etGenreSearch.clearFocus()

        binding.topBarSearch.visibility = View.GONE
        binding.topBarNormal.visibility = View.VISIBLE
        binding.genreHeaderContainer.visibility = View.VISIBLE
        binding.tvSearchResultCount.visibility = View.GONE
        binding.layoutEmptySearch.visibility = View.GONE
        binding.genreSongsRecycler.visibility = View.VISIBLE

        genreSongsAdapter.updateList(ArrayList(songs))
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

        genreSongsAdapter.updateList(filtered)

        if (isSearchMode) {
            binding.tvSearchResultCount.text = LocaleHelper.getSongCountString(requireContext(), filtered.size)
            if (filtered.isEmpty()) {
                binding.genreSongsRecycler.visibility = View.GONE
                binding.layoutEmptySearch.visibility = View.VISIBLE
                binding.tvEmptySearchMsg.text = if (trimmed.isNotEmpty()) {
                    getString(R.string.no_songs_matching, trimmed)
                } else {
                    getString(R.string.no_songs_found)
                }
            } else {
                binding.genreSongsRecycler.visibility = View.VISIBLE
                binding.layoutEmptySearch.visibility = View.GONE
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etGenreSearch.windowToken, 0)
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

    private fun updateSongs(genre: String) {
        val allSongs = MusicService.allSongs
        val fetched = GenreRepository.getSongsForGenre(requireContext(), genre, allSongs)

        songs.clear()
        songs.addAll(fetched)

        binding.genreSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)

        if (isSearchMode) {
            applySearchFilter(currentSearchQuery)
        } else {
            genreSongsAdapter.updateList(ArrayList(songs))
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
        genreSongsAdapter.updatePlayingState(
            MusicService.song?.id,
            MusicService.mediaPlayer?.isPlaying == true
        )
    }
}
