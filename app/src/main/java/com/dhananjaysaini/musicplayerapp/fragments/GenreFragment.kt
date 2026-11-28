package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.GenreSongsActivity
import com.dhananjaysaini.musicplayerapp.adapter.GenreAdapter
import com.dhananjaysaini.musicplayerapp.databinding.FragmentGenreBinding
import com.dhananjaysaini.musicplayerapp.repository.GenreRepository
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.GenreOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel

class GenreFragment : Fragment(R.layout.fragment_genre) {

    private lateinit var binding: FragmentGenreBinding
    private lateinit var genreAdapter: GenreAdapter
    private lateinit var mainViewModel: MainViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentGenreBinding.bind(view)

        genreAdapter = GenreAdapter(mutableListOf())

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        val recycler = view.findViewById<RecyclerView>(R.id.genreRecycler)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = genreAdapter

        genreAdapter.onItemClick = { genre ->
            startActivity(
                Intent(requireContext(), GenreSongsActivity::class.java).apply {
                    putExtra("genreName", genre.name)
                }
            )
        }

        genreAdapter.onMenuClick = { genre, _ ->
            val allSongs = MusicService.allSongs
            val genreSongs = allSongs.filter { genre.songIds.contains(it.id) }
            val sheet = GenreOptionsBottomSheet.newInstance(
                genreName = genre.name,
                artUri = genre.artUri,
                songs = genreSongs
            )
            sheet.onGenreUpdated = {
                // LiveData update will trigger refresh
            }
            sheet.show(parentFragmentManager, "genre_options_${genre.name}")
        }

        mainViewModel.musicListLiveData.observe(viewLifecycleOwner) { songs ->
            val genres = GenreRepository.fetchGenres(requireContext(), songs)
            genreAdapter.updateList(genres)
            binding.totalGenres.text = LocaleHelper.getGenreCountString(requireContext(), genres.size)
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
    }
}
