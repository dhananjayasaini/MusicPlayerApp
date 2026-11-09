package com.dhananjaysaini.musicplayerapp.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.AllSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.FragmentSearchBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.FavouriteManager
import com.dhananjaysaini.musicplayerapp.utils.SongOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import java.util.Locale

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var searchAdapter: AllSongsAdapter
    private var allSongs: List<Music> = emptyList()
    private var filteredSongs: ArrayList<Music> = arrayListOf()

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                binding.etSearch.setText(spokenText)
                binding.etSearch.setSelection(spokenText.length)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        searchAdapter = AllSongsAdapter(requireContext(), arrayListOf())
        binding.searchRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
            setHasFixedSize(true)
        }

        searchAdapter.onItemClick = { list, position ->
            playSong(list, position)
        }

        searchAdapter.onMenuClick = { song, pos ->
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
                .show(parentFragmentManager, "search_song_menu")
        }
    }

    private fun setupListeners() {
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim() ?: ""
            binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            filterResults(query)
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
        }

        binding.btnVoiceSearch.setOnClickListener {
            startVoiceSearch()
        }
    }

    private fun observeData() {
        mainViewModel.musicListLiveData.observe(viewLifecycleOwner) { songs ->
            allSongs = songs
            val currentQuery = binding.etSearch.text?.toString()?.trim() ?: ""
            filterResults(currentQuery)
        }
    }

    private fun filterResults(query: String) {
        if (query.isEmpty()) {
            filteredSongs = ArrayList(allSongs)
        } else {
            val lower = query.lowercase(Locale.getDefault())
            filteredSongs = ArrayList(
                allSongs.filter { song ->
                    song.title.lowercase(Locale.getDefault()).contains(lower) ||
                    song.artist.lowercase(Locale.getDefault()).contains(lower) ||
                    song.album.lowercase(Locale.getDefault()).contains(lower)
                }
            )
        }

        searchAdapter.updateList(filteredSongs)

        if (filteredSongs.isEmpty()) {
            binding.layoutEmptySearch.visibility = View.VISIBLE
            binding.tvEmptySearchMsg.text = if (query.isEmpty()) "No songs found" else "No songs matching \"$query\""
            binding.tvSearchResultCount.visibility = View.GONE
        } else {
            binding.layoutEmptySearch.visibility = View.GONE
            binding.tvSearchResultCount.visibility = View.VISIBLE
            binding.tvSearchResultCount.text = "${filteredSongs.size} songs found"
        }
    }

    private fun startVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a song, artist, or album name...")
            }
            voiceSearchLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Voice search not available on this device", Toast.LENGTH_SHORT).show()
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

        requireContext().sendBroadcast(Intent("SHOW_MINI_PLAYER"))
        startActivity(Intent(requireContext(), PlayerActivity::class.java))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
