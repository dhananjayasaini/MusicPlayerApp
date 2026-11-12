package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.ArtistSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentArtistSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper

class ArtistSongsFragment : Fragment(R.layout.fragment_artist_songs) {

    private lateinit var binding: FragmentArtistSongsBinding
    private lateinit var artistSongsAdapter: ArtistSongsAdapter
    private val songs = ArrayList<Music>()

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

        artistSongsAdapter = ArtistSongsAdapter(requireContext(), songs)

        val recycler = binding.artistSongsRecycler

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = artistSongsAdapter

        artistSongsAdapter.onItemClick = { list, position ->
            playSong(ArrayList(list), position)
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

        updateSongs(artistName)
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

    private fun updateSongs(artist: String) {

        val allSongs = MusicService.allSongs

        songs.clear()
        songs.addAll(
            allSongs.filter { it.artist == artist }
        )

        artistSongsAdapter.updateList(ArrayList(songs))

        binding.artistSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)
    }
}
