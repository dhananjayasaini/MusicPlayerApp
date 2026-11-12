package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.AlbumSongsActivity
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.AlbumSongsAdapter
import com.dhananjaysaini.musicplayerapp.adapter.ArtistSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentAlbumBinding
import com.dhananjaysaini.musicplayerapp.databinding.FragmentAlbumSongsBinding
import com.dhananjaysaini.musicplayerapp.databinding.FragmentArtistSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager

class AlbumSongsFragment : Fragment(R.layout.fragment_album_songs) {

    private lateinit var binding: FragmentAlbumSongsBinding
    private lateinit var albumSongsAdapter: AlbumSongsAdapter
    private val songs = ArrayList<Music>()

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

        ThemeManager.applyThemeToActivity(requireActivity())

        albumSongsAdapter = AlbumSongsAdapter(requireContext(), songs)

        val recycler = binding.albumSongsRecycler

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = albumSongsAdapter

        albumSongsAdapter.onItemClick = { list, position ->
            playSong(ArrayList(list), position)
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

        updateSongs(albumName)
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

        albumSongsAdapter.updateList(ArrayList(songs))

        binding.albumSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)
    }
    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
    }
}
