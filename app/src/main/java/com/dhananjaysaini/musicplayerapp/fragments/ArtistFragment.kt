package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.ArtistSongsActivity
import com.dhananjaysaini.musicplayerapp.adapter.ArtistAdapter
import com.dhananjaysaini.musicplayerapp.databinding.FragmentArtistBinding
import com.dhananjaysaini.musicplayerapp.model.MusicArtist
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.ArtistOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.SelectSongsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.SongSortManager
import com.dhananjaysaini.musicplayerapp.utils.SortSongsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel

class ArtistFragment : Fragment(R.layout.fragment_artist) {

    private lateinit var binding: FragmentArtistBinding
    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var mainViewModel: MainViewModel
    private var rawArtists: List<MusicArtist> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentArtistBinding.bind(view)

        artistAdapter = ArtistAdapter(mutableListOf())

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        val recycler = view.findViewById<RecyclerView>(R.id.artistRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = artistAdapter

        Log.d("artist1", "artistAdapter " + recycler.adapter)

        artistAdapter.onItemClick = { artist ->
            startActivity(
                Intent(requireContext(), ArtistSongsActivity::class.java).apply {
                    putExtra("artistName", artist.name)
                }
            )
        }

        artistAdapter.onMenuClick = { artist, _ ->
            val unknownArtist = getString(R.string.unknown_artist)
            val artistSongs = MusicService.allSongs.filter {
                (it.artist ?: unknownArtist) == artist.name
            }
            val sheet = ArtistOptionsBottomSheet.newInstance(
                artistName = artist.name,
                albumCount = artist.albumCount,
                artUri = artist.artUri,
                songs = artistSongs
            )
            sheet.onArtistUpdated = {
                // MainViewModel updates will automatically refresh the list via LiveData
            }
            sheet.show(parentFragmentManager, "artist_options_${artist.name}")
        }

        setupActionButtons()

        mainViewModel.musicListLiveData.observe(viewLifecycleOwner) { songs ->
            val unknownArtist = getString(R.string.unknown_artist)
            rawArtists = songs
                .groupBy { it.artist ?: unknownArtist }
                .map { (name, list) ->
                    val albums = list.mapNotNull { it.album }.filter { it.isNotBlank() }.distinct()
                    val albumCount = if (albums.isEmpty()) 1 else albums.size
                    MusicArtist(
                        name = name,
                        songCount = list.size,
                        artUri = list.firstOrNull()?.artUri,
                        artistName = name,
                        albumCount = albumCount
                    )
                }

            val sorted = applyArtistSort(rawArtists)
            artistAdapter.updateList(sorted)

            binding.totalArtist.text = LocaleHelper.getArtistCountString(requireContext(), sorted.size)
        }
    }

    private fun applyArtistSort(artists: List<MusicArtist>): List<MusicArtist> {
        val type = SongSortManager.getSortType(requireContext())
        val order = SongSortManager.getSortOrder(requireContext())
        val sorted = when (type) {
            SongSortManager.SortType.ALBUM_NAME -> artists.sortedBy { it.albumCount }
            SongSortManager.SortType.SIZE, SongSortManager.SortType.PLAY_COUNT -> artists.sortedBy { it.songCount }
            SongSortManager.SortType.RANDOM -> artists.shuffled()
            else -> artists.sortedBy { it.name.lowercase() }
        }
        return if (type == SongSortManager.SortType.RANDOM) {
            sorted
        } else if (order == SongSortManager.SortOrder.DESCENDING) {
            sorted.reversed()
        } else {
            sorted
        }
    }

    private fun setupActionButtons() {
        binding.btnSortArtists.setOnClickListener {
            val sortSheet = SortSongsBottomSheet()
            sortSheet.onSortApplied = {
                val sorted = applyArtistSort(rawArtists)
                artistAdapter.updateList(sorted)
            }
            sortSheet.show(parentFragmentManager, "SortSongsBottomSheet")
        }

        binding.btnSelectSongs.setOnClickListener {
            val allSongs = mainViewModel.musicListLiveData.value ?: MusicService.allSongs
            val sorted = SongSortManager.sortSongs(allSongs, requireContext())
            val selectSheet = SelectSongsBottomSheet.newInstance(ArrayList(sorted))
            selectSheet.show(parentFragmentManager, "SelectSongsBottomSheet")
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
    }
}
