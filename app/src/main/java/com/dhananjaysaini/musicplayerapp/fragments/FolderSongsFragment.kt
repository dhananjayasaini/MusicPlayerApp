package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.FolderSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentFolderSongsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.viewmodel.FolderViewModel
import java.io.File

class FolderSongsFragment : Fragment(R.layout.fragment_folder_songs) {

    private lateinit var binding: FragmentFolderSongsBinding
    private lateinit var folderSongsAdapter: FolderSongsAdapter

    private val songs = ArrayList<Music>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFolderSongsBinding.bind(view)

        val folderPath = requireArguments().getString("folderPath") ?: return
        val folderName = requireArguments().getString("folderName")
            ?: File(folderPath).name.ifEmpty { getString(R.string.tab_folders) }

        binding.folderTitle.text = folderName

        songs.addAll(MusicService.allSongs.filter {
                it.path.startsWith(folderPath)
            }
        )

        folderSongsAdapter = FolderSongsAdapter(requireContext(), songs)

        binding.folderSongsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.folderSongsRecycler.adapter = folderSongsAdapter

        // 🔥 SAME click behaviour everywhere
        folderSongsAdapter.onItemClick = { list, position ->
            playSong(ArrayList(list), position)
        }

        binding.btnFolderShuffle.setOnClickListener {
            if (songs.isNotEmpty()) {
                val shuffled = ArrayList(songs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnFolderPlay.setOnClickListener {
            if (songs.isNotEmpty()) {
                playSong(ArrayList(songs), 0)
            }
        }

        binding.btnBack.setOnClickListener{
            requireActivity().finish()
        }
        updateSongs(folderPath)
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

    private fun updateSongs(folderPath: String) {

        val allSongs = MusicService.allSongs

        songs.clear()
        songs.addAll(
            allSongs.filter { it.path.startsWith(folderPath) }
        )

        folderSongsAdapter.updateList(ArrayList(songs))

        binding.folderSongCount.text = LocaleHelper.getSongCountString(requireContext(), songs.size)
    }

}
