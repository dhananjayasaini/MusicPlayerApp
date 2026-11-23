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
import com.dhananjaysaini.musicplayerapp.adapter.ArtistSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentArtistSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.ArtistOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.FavouriteManager
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.SongOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager

class ArtistSongsFragment : Fragment(R.layout.fragment_artist_songs) {

    private lateinit var binding: FragmentArtistSongsBinding
    private lateinit var artistSongsAdapter: ArtistSongsAdapter
    private val songs = ArrayList<Music>()

    private var isSearchMode = false
    private var currentSearchQuery = ""
    private lateinit var searchBackCallback: OnBackPressedCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentArtistSongsBinding.bind(view)

        val artistName = requireArguments().getString("artistName") ?: return

        binding.artistTitle.text = artistName

        val unknownArtist = getString(R.string.unknown_artist)
        songs.addAll(
            MusicService.allSongs.filter {
                (it.artist ?: unknownArtist) == artistName
            }
        )

        ThemeManager.applyThemeToActivity(requireActivity())

        artistSongsAdapter = ArtistSongsAdapter(requireContext(), songs)
        artistSongsAdapter.attachLifecycle(viewLifecycleOwner)

        val recycler = binding.artistSongsRecycler

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = artistSongsAdapter

        artistSongsAdapter.onItemClick = { list, position ->
            playSong(ArrayList(list), position)
        }

        artistSongsAdapter.onMenuClick = { song, pos ->
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
                binding.artistSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)
                if (isSearchMode) {
                    applySearchFilter(currentSearchQuery)
                } else {
                    artistSongsAdapter.updateList(ArrayList(songs))
                }
            }
            sheet.show(parentFragmentManager, "artist_song_menu_${song.id}")
        }

        binding.btnArtistShuffle.setOnClickListener {
            if (songs.isNotEmpty()) {
                val shuffled = ArrayList(songs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnArtistPlay.setOnClickListener {
            if (songs.isNotEmpty()) {
                playSong(ArrayList(songs), 0)
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnArtistMore.setOnClickListener {
            val albums = songs.mapNotNull { it.album }.filter { it.isNotBlank() }.distinct()
            val albumCount = if (albums.isEmpty()) 1 else albums.size
            val sheet = ArtistOptionsBottomSheet.newInstance(
                artistName = artistName,
                albumCount = albumCount,
                artUri = songs.firstOrNull()?.artUri,
                songs = songs
            )
            sheet.onArtistUpdated = {
                updateSongs(artistName)
            }
            sheet.show(parentFragmentManager, "artist_options_header")
        }

        setupSearch()
        updateSongs(artistName)
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

        binding.btnArtistSearch.setOnClickListener {
            openSearchMode()
        }

        binding.btnCloseSearch.setOnClickListener {
            closeSearchMode()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etArtistSearch.setText("")
            binding.etArtistSearch.requestFocus()
        }

        binding.etArtistSearch.doAfterTextChanged { editable ->
            if (isSearchMode) {
                applySearchFilter(editable?.toString().orEmpty())
            }
        }

        binding.etArtistSearch.setOnEditorActionListener { _, actionId, _ ->
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
        binding.artistHeaderContainer.visibility = View.GONE
        binding.tvSearchResultCount.visibility = View.VISIBLE

        binding.etArtistSearch.requestFocus()
        binding.etArtistSearch.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etArtistSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        applySearchFilter(currentSearchQuery)
    }

    private fun closeSearchMode() {
        isSearchMode = false
        currentSearchQuery = ""
        searchBackCallback.isEnabled = false

        hideKeyboard()
        binding.etArtistSearch.setText("")
        binding.etArtistSearch.clearFocus()

        binding.topBarSearch.visibility = View.GONE
        binding.topBarNormal.visibility = View.VISIBLE
        binding.artistHeaderContainer.visibility = View.VISIBLE
        binding.tvSearchResultCount.visibility = View.GONE
        binding.layoutEmptySearch.visibility = View.GONE
        binding.artistSongsRecycler.visibility = View.VISIBLE

        artistSongsAdapter.updateList(ArrayList(songs))
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

        artistSongsAdapter.updateList(filtered)

        if (isSearchMode) {
            binding.tvSearchResultCount.text = LocaleHelper.getSongCountString(requireContext(), filtered.size)
            if (filtered.isEmpty()) {
                binding.artistSongsRecycler.visibility = View.GONE
                binding.layoutEmptySearch.visibility = View.VISIBLE
                binding.tvEmptySearchMsg.text = if (trimmed.isNotEmpty()) {
                    getString(R.string.no_songs_matching, trimmed)
                } else {
                    getString(R.string.no_songs_found)
                }
            } else {
                binding.artistSongsRecycler.visibility = View.VISIBLE
                binding.layoutEmptySearch.visibility = View.GONE
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etArtistSearch.windowToken, 0)
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

    private fun updateSongs(artist: String) {
        val allSongs = MusicService.allSongs

        songs.clear()
        songs.addAll(
            allSongs.filter { it.artist == artist }
        )

        binding.artistSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)

        if (isSearchMode) {
            applySearchFilter(currentSearchQuery)
        } else {
            artistSongsAdapter.updateList(ArrayList(songs))
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
        artistSongsAdapter.updatePlayingState(
            MusicService.song?.id,
            MusicService.mediaPlayer?.isPlaying == true
        )
    }
}
