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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.AlbumSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentAlbumSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.AlbumCoverManager
import com.dhananjaysaini.musicplayerapp.utils.AlbumOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.FavouriteManager
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.SongOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager

class AlbumSongsFragment : Fragment(R.layout.fragment_album_songs) {

    private lateinit var binding: FragmentAlbumSongsBinding
    private lateinit var albumSongsAdapter: AlbumSongsAdapter
    private val songs = ArrayList<Music>()

    private var isSearchMode = false
    private var currentSearchQuery = ""
    private lateinit var searchBackCallback: OnBackPressedCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAlbumSongsBinding.bind(view)

        val albumName = requireArguments().getString("albumName") ?: return

        binding.albumTitle.text = albumName

        val unknownAlbum = getString(R.string.unknown_album)
        songs.addAll(
            MusicService.allSongs.filter {
                (it.album ?: unknownAlbum) == albumName
            }
        )

        val customCover = AlbumCoverManager.getCustomCover(requireContext(), albumName)
        val coverToLoad = customCover ?: songs.firstOrNull()?.artUri
        Glide.with(this)
            .load(coverToLoad)
            .apply(RequestOptions().placeholder(R.drawable.ic_neon_album).centerCrop())
            .into(binding.ivAlbumCoverBig)

        ThemeManager.applyThemeToActivity(requireActivity())

        albumSongsAdapter = AlbumSongsAdapter(requireContext(), songs)
        albumSongsAdapter.attachLifecycle(viewLifecycleOwner)

        val recycler = binding.albumSongsRecycler

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = albumSongsAdapter

        albumSongsAdapter.onItemClick = { list, position ->
            playSong(ArrayList(list), position)
        }

        albumSongsAdapter.onMenuClick = { song, pos ->
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
                goToAlbum = false,
                hideSong = true,
                share = true
            )
            val sheet = SongOptionsBottomSheet(song, pos, config)
            sheet.onSongHidden = { s ->
                songs.removeAll { it.id == s.id }
                binding.albumSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)
                if (isSearchMode) {
                    applySearchFilter(currentSearchQuery)
                } else {
                    albumSongsAdapter.updateList(ArrayList(songs))
                }
            }
            sheet.show(parentFragmentManager, "album_song_menu_${song.id}")
        }

        binding.btnAlbumShuffle.setOnClickListener {
            if (songs.isNotEmpty()) {
                val shuffled = ArrayList(songs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnAlbumPlay.setOnClickListener {
            if (songs.isNotEmpty()) {
                playSong(ArrayList(songs), 0)
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnAlbumMore.setOnClickListener {
            val artistName = songs.firstOrNull()?.artist ?: "<unknown>"
            val sheet = AlbumOptionsBottomSheet.newInstance(
                albumName = albumName,
                artistName = artistName,
                artUri = songs.firstOrNull()?.artUri,
                songs = songs
            )
            sheet.onAlbumCoverChanged = { _: String, newUri: String ->
                Glide.with(this)
                    .load(newUri)
                    .apply(RequestOptions().placeholder(R.drawable.ic_neon_album).centerCrop())
                    .into(binding.ivAlbumCoverBig)
            }
            sheet.show(parentFragmentManager, "album_options_header")
        }

        setupSearch()
        updateSongs(albumName)
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

        binding.btnAlbumSearch.setOnClickListener {
            openSearchMode()
        }

        binding.btnCloseSearch.setOnClickListener {
            closeSearchMode()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etAlbumSearch.setText("")
            binding.etAlbumSearch.requestFocus()
        }

        binding.etAlbumSearch.doAfterTextChanged { editable ->
            if (isSearchMode) {
                applySearchFilter(editable?.toString().orEmpty())
            }
        }

        binding.etAlbumSearch.setOnEditorActionListener { _, actionId, _ ->
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
        binding.albumHeaderContainer.visibility = View.GONE
        binding.tvSearchResultCount.visibility = View.VISIBLE

        binding.etAlbumSearch.requestFocus()
        binding.etAlbumSearch.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etAlbumSearch, InputMethodManager.SHOW_IMPLICIT)
        }

        applySearchFilter(currentSearchQuery)
    }

    private fun closeSearchMode() {
        isSearchMode = false
        currentSearchQuery = ""
        searchBackCallback.isEnabled = false

        hideKeyboard()
        binding.etAlbumSearch.setText("")
        binding.etAlbumSearch.clearFocus()

        binding.topBarSearch.visibility = View.GONE
        binding.topBarNormal.visibility = View.VISIBLE
        binding.albumHeaderContainer.visibility = View.VISIBLE
        binding.tvSearchResultCount.visibility = View.GONE
        binding.layoutEmptySearch.visibility = View.GONE
        binding.albumSongsRecycler.visibility = View.VISIBLE

        albumSongsAdapter.updateList(ArrayList(songs))
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

        albumSongsAdapter.updateList(filtered)

        if (isSearchMode) {
            binding.tvSearchResultCount.text = LocaleHelper.getSongCountString(requireContext(), filtered.size)
            if (filtered.isEmpty()) {
                binding.albumSongsRecycler.visibility = View.GONE
                binding.layoutEmptySearch.visibility = View.VISIBLE
                binding.tvEmptySearchMsg.text = if (trimmed.isNotEmpty()) {
                    getString(R.string.no_songs_matching, trimmed)
                } else {
                    getString(R.string.no_songs_found)
                }
            } else {
                binding.albumSongsRecycler.visibility = View.VISIBLE
                binding.layoutEmptySearch.visibility = View.GONE
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etAlbumSearch.windowToken, 0)
    }

    private fun playSong(list: ArrayList<Music>, position: Int) {
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

    private fun updateSongs(album: String) {
        val allSongs = MusicService.allSongs

        songs.clear()
        songs.addAll(
            allSongs.filter { it.album == album }
        )

        binding.albumSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)

        if (isSearchMode) {
            applySearchFilter(currentSearchQuery)
        } else {
            albumSongsAdapter.updateList(ArrayList(songs))
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
        albumSongsAdapter.updatePlayingState(
            MusicService.song?.id,
            MusicService.mediaPlayer?.isPlaying == true
        )
    }
}
