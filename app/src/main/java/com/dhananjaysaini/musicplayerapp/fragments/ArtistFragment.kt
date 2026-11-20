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
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel

class ArtistFragment : Fragment(R.layout.fragment_artist) {

    private lateinit var binding: FragmentArtistBinding
    private lateinit var artistAdapter: ArtistAdapter
    private lateinit var mainViewModel: MainViewModel

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

        mainViewModel.musicListLiveData.observe(viewLifecycleOwner) { songs ->
            val unknownArtist = getString(R.string.unknown_artist)
            val artists = songs
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
                .sortedBy { it.name.lowercase() }

            artistAdapter.updateList(artists)

            binding.totalArtist.text = LocaleHelper.getArtistCountString(requireContext(), artists.size)
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
    }
}
