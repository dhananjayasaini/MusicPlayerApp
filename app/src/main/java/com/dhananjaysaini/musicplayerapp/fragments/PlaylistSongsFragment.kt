package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.PlaylistSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.databinding.FragmentPlaylistSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.viewmodel.FavouriteViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.PlaylistViewModel

class PlaylistSongsFragment : Fragment(R.layout.fragment_playlist_songs) {

    private lateinit var binding: FragmentPlaylistSongsBinding
    private lateinit var mainVM: MainViewModel
    private lateinit var playlistVM: PlaylistViewModel
    private lateinit var favouriteVM: FavouriteViewModel
    private lateinit var playlistSongsAdapter: PlaylistSongsAdapter
    private val songs = ArrayList<Music>()
    private var currentSongIds: List<String> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentPlaylistSongsBinding.bind(view)

        playlistVM = ViewModelProvider(requireActivity()).get(PlaylistViewModel::class.java)
        mainVM = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        favouriteVM = ViewModelProvider(requireActivity()).get(FavouriteViewModel::class.java)

        playlistSongsAdapter = PlaylistSongsAdapter(requireContext(), songs)

        binding.playlistSongsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.playlistSongsRecycler.adapter = playlistSongsAdapter

        playlistSongsAdapter.onItemClick = { list: ArrayList<Music>, position: Int ->
            playSong(ArrayList(list), position)
        }

        binding.btnPlaylistShuffle.setOnClickListener {
            if (songs.isNotEmpty()) {
                val shuffled = ArrayList(songs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnPlaylistPlay.setOnClickListener {
            if (songs.isNotEmpty()) {
                playSong(ArrayList(songs), 0)
            }
        }

        val playlistName = arguments?.getString("playlistName") ?: "Playlist"
        val playlistType = arguments?.getString("playlistType")
        val playlistId = arguments?.getInt("playlistId", -1) ?: -1

        binding.playlistTitle.text = playlistName

        binding.btnBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnPlaylistSearch.setOnClickListener {
            Toast.makeText(requireContext(), "Search coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnPlaylistOptions.setOnClickListener {
            Toast.makeText(requireContext(), "Options coming soon", Toast.LENGTH_SHORT).show()
        }

        when (playlistType) {
            "favourite" -> {
                binding.playlistCardBox.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_preset_fav)
                binding.playlistCardIcon.setImageResource(R.drawable.ic_neon_favourite)
                binding.playlistCardIcon.imageTintList = null

                favouriteVM.favouriteIds.observe(viewLifecycleOwner) { ids ->
                    val all = MusicService.allSongs.ifEmpty { mainVM.musicListLiveData.value ?: emptyList() }
                    songs.clear()
                    songs.addAll(all.filter { it.id in ids })
                    playlistSongsAdapter.updateList(ArrayList(songs))
                    binding.playlistSongCount.text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}"
                }
            }
            "recently_added" -> {
                binding.playlistCardBox.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_preset_recent_add)
                binding.playlistCardIcon.setImageResource(R.drawable.ic_neon_music_note)
                binding.playlistCardIcon.imageTintList = null

                mainVM.musicListLiveData.observe(viewLifecycleOwner) { all ->
                    songs.clear()
                    songs.addAll(all.sortedByDescending { it.date })
                    playlistSongsAdapter.updateList(ArrayList(songs))
                    binding.playlistSongCount.text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}"
                }
            }
            "recently_played" -> {
                binding.playlistCardBox.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_preset_recent_play)
                binding.playlistCardIcon.setImageResource(R.drawable.ic_neon_timer)
                binding.playlistCardIcon.imageTintList = null

                MusicDatabase.getDatabase(requireContext()).playHistoryDao().getRecentlyPlayed()
                    .observe(viewLifecycleOwner) { history ->
                        val all = MusicService.allSongs.ifEmpty { mainVM.musicListLiveData.value ?: emptyList() }
                        val songMap = all.associateBy { it.id }
                        songs.clear()
                        history.forEach { item ->
                            songMap[item.songId]?.let { songs.add(it) }
                        }
                        playlistSongsAdapter.updateList(ArrayList(songs))
                        binding.playlistSongCount.text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}"
                    }
            }
            "top_tracks" -> {
                binding.playlistCardBox.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_preset_top)
                binding.playlistCardIcon.setImageResource(R.drawable.ic_fire)
                binding.playlistCardIcon.imageTintList = null

                MusicDatabase.getDatabase(requireContext()).playHistoryDao().getTopPlayed()
                    .observe(viewLifecycleOwner) { history ->
                        val all = MusicService.allSongs.ifEmpty { mainVM.musicListLiveData.value ?: emptyList() }
                        val songMap = all.associateBy { it.id }
                        songs.clear()
                        history.forEach { item ->
                            songMap[item.songId]?.let { songs.add(it) }
                        }
                        playlistSongsAdapter.updateList(ArrayList(songs))
                        binding.playlistSongCount.text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}"
                    }
            }
            else -> {
                // Custom playlist
                binding.playlistCardIcon.setImageResource(R.drawable.ic_neon_playlist_card)
                binding.playlistCardIcon.imageTintList = null
                if (playlistId != -1) {
                    playlistVM.getPlaylistSongIds(playlistId).observe(viewLifecycleOwner) { ids ->
                        currentSongIds = ids
                        updateSongs()
                    }
                    mainVM.musicListLiveData.observe(viewLifecycleOwner) {
                        updateSongs()
                    }
                    updateTitle()
                    removeSong()
                }
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
        startActivity(Intent(requireContext(), PlayerActivity::class.java)
            .putExtra("source", "playlist"))
    }

    private fun updateSongs() {

        val allSongs = MusicService.allSongs   // or MusicService.playlist cache

        if (currentSongIds.isEmpty()) {
            songs.clear()
            return
        }

        songs.clear()
        songs.addAll(allSongs.filter { it.id in currentSongIds })

        playlistSongsAdapter.updateList(ArrayList(songs))

        binding.playlistSongCount.text = when (songs.size){
            0 -> "0 song"
            1 -> "1 song"
            else -> "${songs.size} songs"
        }
    }

    private fun removeSong(){
        playlistSongsAdapter.onRemoveFromPlaylist = { song: Music ->

            val playlistId =
                requireArguments().getInt("playlistId")

            playlistVM.removeSongFromPlaylist(
                playlistId = playlistId,
                songId = song.id
            )
        }
    }

    private fun updateTitle(){

        val playlistId = requireArguments().getInt("playlistId")

        playlistVM.getPlaylistById(playlistId)
            .observe(viewLifecycleOwner) { playlist ->
                binding.playlistTitle.text = playlist.name
            }

    }

}
