package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.AlbumSongsActivity
import com.dhananjaysaini.musicplayerapp.adapter.AlbumAdapter
import com.dhananjaysaini.musicplayerapp.databinding.FragmentAlbumBinding
import com.dhananjaysaini.musicplayerapp.model.MusicArtist
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel

class AlbumFragment : Fragment(R.layout.fragment_album) {

        private lateinit var binding: FragmentAlbumBinding
        private lateinit var albumAdapter: AlbumAdapter
        private lateinit var mainViewModel: MainViewModel

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            binding = FragmentAlbumBinding.bind(view)

            albumAdapter = AlbumAdapter(mutableListOf())

            mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

            val recycler = view.findViewById<RecyclerView>(R.id.artistRecycler)
            recycler.layoutManager = GridLayoutManager(requireContext(), 2)
            recycler.adapter = albumAdapter

            albumAdapter.onItemClick = { artist ->

                startActivity(
                    Intent(requireContext(), AlbumSongsActivity::class.java).apply {
                        putExtra("albumName", artist.name)
                    }
                )
            }

            mainViewModel.musicListLiveData.observe(viewLifecycleOwner) { songs ->

                val unknownAlbum = getString(R.string.unknown_album)
                val albums = songs
                    .groupBy { it.album ?: unknownAlbum }
                    .map { (name, list) ->
                        MusicArtist(name, list.size, list.firstOrNull()?.artUri)
                    }
                    .sortedBy { it.name.lowercase() }

                albumAdapter.update(albums)

                binding.totalArtist.text = LocaleHelper.getAlbumCountString(requireContext(), albums.size)
            }
        }
    }
