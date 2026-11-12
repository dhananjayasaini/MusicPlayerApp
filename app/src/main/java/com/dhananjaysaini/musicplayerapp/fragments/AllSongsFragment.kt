package com.dhananjaysaini.musicplayerapp.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.AllSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentAllSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.DeleteManager
import com.dhananjaysaini.musicplayerapp.utils.FavouriteManager
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.SongOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel

open class AllSongsFragment : Fragment() {

    private lateinit var binding: FragmentAllSongsBinding
    private lateinit var allSongsAdapter: AllSongsAdapter
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        allSongsAdapter = AllSongsAdapter(requireContext(), ArrayList())

        binding = FragmentAllSongsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        observeSongs()

        allSongsAdapter.onMenuClick = { song, pos ->

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
                share = true
            )

            SongOptionsBottomSheet(song, pos, config)
                .show(parentFragmentManager, "song_menu")
        }
    }

    private fun setupButtons() {
        binding.btnShuffleAll.setOnClickListener {
            val list = mainViewModel.musicListLiveData.value
            if (!list.isNullOrEmpty()) {
                val shuffled = ArrayList(list)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnPlayAll.setOnClickListener {
            val list = mainViewModel.musicListLiveData.value
            if (!list.isNullOrEmpty()) {
                playSong(ArrayList(list), 0)
            }
        }
    }

    private fun setupRecyclerView() {
        allSongsAdapter = AllSongsAdapter(requireContext(), ArrayList())

        binding.musicRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AllSongsFragment.allSongsAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }

        allSongsAdapter.onItemClick = { list, position ->
            playSong(list, position)
        }
    }

    private fun observeSongs() {
        mainViewModel.musicListLiveData.observe(viewLifecycleOwner) { list ->
            allSongsAdapter.updateList(ArrayList(list))

            binding.totalSongsTv.text = LocaleHelper.getSongCountString(requireContext(), list.size)
        }
    }

    fun playSong(list: ArrayList<Music>, position: Int) {

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

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
    }

}