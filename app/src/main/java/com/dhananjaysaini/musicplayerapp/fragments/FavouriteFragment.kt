package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.FavouriteAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentFavouriteBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.SongOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.viewmodel.FavouriteViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel

class FavouriteFragment : Fragment(R.layout.fragment_favourite) {

    private lateinit var binding: FragmentFavouriteBinding
    private lateinit var favAdapter: FavouriteAdapter
    private lateinit var favouriteViewModel: FavouriteViewModel
    private lateinit var mainViewModel: MainViewModel

    private val favouriteSongs = ArrayList<Music>()
    private var currentFavIds = listOf<String>()
    private var currentAllSongs = listOf<Music>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFavouriteBinding.bind(view)

        favouriteViewModel = ViewModelProvider(requireActivity()).get(FavouriteViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        setupRecyclerView()
        setupButtons()
        observeFavorites()
    }

    private fun setupRecyclerView() {
        favAdapter = FavouriteAdapter(requireContext(), favouriteSongs)

        binding.favRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favAdapter
            setHasFixedSize(true)
        }

        favAdapter.onItemClick = { list, position ->
            if (list.isNotEmpty()) {
                playSong(list, position)
            }
        }

        favAdapter.onMenuClick = { song, pos ->
            val config = SongMenuConfig(
                play = true,
                playNext = true,
                addToQueue = true,
                delete = true,
                edit = false,
                addToFav = false,
                removeFromFav = true,
                addToPlaylist = true,
                share = true
            )

            SongOptionsBottomSheet(song, pos, config)
                .show(parentFragmentManager, "fav_song_menu")
        }
    }

    private fun setupButtons() {
        binding.btnFavShuffle.setOnClickListener {
            if (favouriteSongs.isNotEmpty()) {
                val shuffled = ArrayList(favouriteSongs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnFavPlay.setOnClickListener {
            if (favouriteSongs.isNotEmpty()) {
                playSong(ArrayList(favouriteSongs), 0)
            }
        }
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

    private fun observeFavorites() {
        favouriteViewModel.favouriteIds.observe(viewLifecycleOwner) { favIds ->
            currentFavIds = favIds ?: emptyList()
            rebuildFavorites()
        }

        mainViewModel.musicListLiveData.observe(viewLifecycleOwner) { allSongs ->
            currentAllSongs = allSongs ?: emptyList()
            rebuildFavorites()
        }
    }

    private fun rebuildFavorites() {
        if (currentFavIds.isEmpty() || currentAllSongs.isEmpty()) {
            favouriteSongs.clear()
            favAdapter.updateList(arrayListOf())
            binding.tvFavSongCount.text = LocaleHelper.getSongCountString(requireContext(), 0)
            binding.layoutEmptyFav.visibility = View.VISIBLE
            binding.favRecyclerView.visibility = View.GONE
            return
        }

        val favIdSet = currentFavIds.toSet()
        val filtered = currentAllSongs.filter { it.id in favIdSet }

        favouriteSongs.clear()
        favouriteSongs.addAll(filtered)
        favAdapter.updateList(ArrayList(favouriteSongs))

        binding.tvFavSongCount.text = LocaleHelper.getSongCountString(requireContext(), filtered.size)

        if (filtered.isEmpty()) {
            binding.layoutEmptyFav.visibility = View.VISIBLE
            binding.favRecyclerView.visibility = View.GONE
        } else {
            binding.layoutEmptyFav.visibility = View.GONE
            binding.favRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
    }
}
