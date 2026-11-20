package com.dhananjaysaini.musicplayerapp.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.PlaylistAdapter
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetPlaylistPickerBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.viewmodel.PlaylistViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaylistPickerBottomSheet : BottomSheetDialogFragment {

    private var songs: ArrayList<Music> = arrayListOf()
    private lateinit var binding: BottomSheetPlaylistPickerBinding
    private lateinit var playListAdapter: PlaylistAdapter

    private val playlistViewModel: PlaylistViewModel by activityViewModels()

    constructor() : super()

    constructor(song: Music) : super() {
        this.songs = arrayListOf(song)
    }

    constructor(songs: List<Music>) : super() {
        this.songs = ArrayList(songs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = savedInstanceState ?: arguments
        if (state != null) {
            val list = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                state.getSerializable("songs", ArrayList::class.java) as? ArrayList<Music>
            } else {
                @Suppress("DEPRECATION")
                state.getSerializable("songs") as? ArrayList<Music>
            }
            if (list != null) {
                songs = list
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("songs", songs)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetPlaylistPickerBinding.inflate(inflater, container, false)
        ThemeManager.applyThemeToBottomSheet(requireContext(), binding.root)

        setupRecycler()
        observePlaylists()

        binding.btnCreatePlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }

        return binding.root
    }

    private fun setupRecycler() {
        playListAdapter = PlaylistAdapter(arrayListOf(), showMenu = false)

        binding.playlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.playlistRecycler.adapter = playListAdapter

        playListAdapter.onItemClick = { playlist ->
            songs.forEach { song ->
                playlistViewModel.addSongToPlaylist(
                    playlist.songId,
                    song.id
                )
            }

            val msg = if (songs.size > 1) {
                getString(R.string.songs_added_to_playlist, songs.size, playlist.name)
            } else {
                getString(R.string.toast_added_to_playlist, playlist.name)
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

            dismiss()
        }

        playListAdapter.onDeleteClick = {}
        playListAdapter.onRenameClick = {}
    }

    private fun observePlaylists() {
        playlistViewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            playListAdapter.update(playlists)
        }
    }

    private fun showCreatePlaylistDialog() {
        val existingNames = playlistViewModel.playlists.value?.map { it.name } ?: emptyList()
        CreatePlaylistDialog.show(
            context = requireContext(),
            titleRes = R.string.create_playlist,
            positiveBtnRes = R.string.btn_create,
            existingNames = existingNames
        ) { name ->
            playlistViewModel.createPlaylist(name)
            Toast.makeText(
                requireContext(),
                getString(R.string.playlist_created_msg),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
