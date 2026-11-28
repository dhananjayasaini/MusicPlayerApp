package com.dhananjaysaini.musicplayerapp.utils

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetPlaylistFolderOptionsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PlaylistFolderOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPlaylistFolderOptionsBinding? = null
    private val binding get() = _binding!!

    private var folderTitle: String = ""
    private var iconRes: Int = R.drawable.ic_neon_music_note
    private var bgDrawableRes: Int = R.drawable.bg_preset_recent_add
    private var songList: ArrayList<Music> = arrayListOf()
    private var isCustomPlaylist: Boolean = false
    private var showHideFolder: Boolean = false

    var onRenameClick: (() -> Unit)? = null
    var onDeleteClick: (() -> Unit)? = null
    var onHideFolderClick: (() -> Unit)? = null
    var onAddSongsClick: (() -> Unit)? = null

    companion object {
        private const val ARG_TITLE = "folder_title"
        private const val ARG_ICON = "icon_res"
        private const val ARG_BG = "bg_res"
        private const val ARG_CUSTOM = "is_custom"
        private const val ARG_SHOW_HIDE = "show_hide_folder"
        private const val ARG_SONGS = "song_list"

        fun newInstance(
            title: String,
            iconRes: Int,
            bgDrawableRes: Int,
            songs: List<Music>,
            isCustomPlaylist: Boolean = false,
            showHideFolder: Boolean = false
        ): PlaylistFolderOptionsBottomSheet {
            val sheet = PlaylistFolderOptionsBottomSheet()
            val args = Bundle().apply {
                putString(ARG_TITLE, title)
                putInt(ARG_ICON, iconRes)
                putInt(ARG_BG, bgDrawableRes)
                putBoolean(ARG_CUSTOM, isCustomPlaylist)
                putBoolean(ARG_SHOW_HIDE, showHideFolder)
                putSerializable(ARG_SONGS, ArrayList(songs))
            }
            sheet.arguments = args
            return sheet
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = savedInstanceState ?: arguments
        if (args != null) {
            folderTitle = args.getString(ARG_TITLE, "")
            iconRes = args.getInt(ARG_ICON, R.drawable.ic_neon_music_note)
            bgDrawableRes = args.getInt(ARG_BG, R.drawable.bg_preset_recent_add)
            isCustomPlaylist = args.getBoolean(ARG_CUSTOM, false)
            showHideFolder = args.getBoolean(ARG_SHOW_HIDE, false)
            val list = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getSerializable(ARG_SONGS, ArrayList::class.java) as? ArrayList<Music>
            } else {
                @Suppress("DEPRECATION")
                args.getSerializable(ARG_SONGS) as? ArrayList<Music>
            }
            if (list != null) {
                songList = list
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_TITLE, folderTitle)
        outState.putInt(ARG_ICON, iconRes)
        outState.putInt(ARG_BG, bgDrawableRes)
        outState.putBoolean(ARG_CUSTOM, isCustomPlaylist)
        outState.putBoolean(ARG_SHOW_HIDE, showHideFolder)
        outState.putSerializable(ARG_SONGS, songList)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPlaylistFolderOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ThemeManager.applyThemeToBottomSheet(requireContext(), binding.root)

        binding.tvFolderName.text = folderTitle
        binding.tvFolderSongCount.text = LocaleHelper.getSongCountString(requireContext(), songList.size)
        binding.folderIconContainer.setBackgroundResource(bgDrawableRes)
        binding.ivFolderIcon.setImageResource(iconRes)
        if (iconRes == R.drawable.ic_music) {
            binding.ivFolderIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        } else {
            binding.ivFolderIcon.imageTintList = null
        }

        binding.btnHideFolder.visibility = if (showHideFolder) View.VISIBLE else View.GONE

        binding.btnAddSongsToFolder.visibility = if (isCustomPlaylist || onAddSongsClick != null) View.VISIBLE else View.GONE
        binding.btnAddSongsToFolder.setOnClickListener {
            dismiss()
            onAddSongsClick?.invoke()
        }

        if (isCustomPlaylist) {
            binding.btnRenamePlaylist.visibility = View.VISIBLE
            binding.btnDeletePlaylist.visibility = View.VISIBLE
        } else {
            binding.btnRenamePlaylist.visibility = View.GONE
            binding.btnDeletePlaylist.visibility = View.GONE
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnPlay.setOnClickListener {
            handlePlay()
            dismiss()
        }

        binding.btnPlayNext.setOnClickListener {
            handlePlayNext()
            dismiss()
        }

        binding.btnAddToQueue.setOnClickListener {
            handleAddToQueue()
            dismiss()
        }

        binding.btnAddToPlaylist.setOnClickListener {
            handleAddToPlaylist()
            dismiss()
        }

        binding.btnHideFolder.setOnClickListener {
            dismiss()
            onHideFolderClick?.invoke()
        }

        binding.btnRenamePlaylist.setOnClickListener {
            dismiss()
            onRenameClick?.invoke()
        }

        binding.btnDeletePlaylist.setOnClickListener {
            dismiss()
            onDeleteClick?.invoke()
        }
    }

    private fun handlePlay() {
        if (songList.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        val mainActivity = activity as? com.dhananjaysaini.musicplayerapp.activities.MainActivity
        if (mainActivity != null) {
            dismiss()
            mainActivity.playAudioTrack(ArrayList(songList), 0)
            return
        }
        val act = activity ?: return
        if (!com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper.isPermissionGranted(act)) {
            com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper.checkAndRequestOnAudioClick(act) {
                dismiss()
                executeDirectPlay()
            }
            return
        }
        dismiss()
        executeDirectPlay()
    }

    private fun executeDirectPlay() {
        MusicService.playlist = ArrayList(songList)
        MusicService.position = 0

        requireContext().startService(
            Intent(requireContext(), MusicService::class.java).apply {
                action = Constants.ACTION_PLAY_AT
                putExtra("songPosition", 0)
            }
        )

        requireContext().sendBroadcast(
            Intent("SHOW_MINI_PLAYER").setPackage(requireContext().packageName)
        )
        startActivity(
            Intent(requireContext(), PlayerActivity::class.java)
                .putExtra("source", "playlist")
        )
    }

    private fun handlePlayNext() {
        if (songList.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (MusicService.playlist.isEmpty()) {
            handlePlay()
        } else {
            MusicService.addSongsToPlayNext(songList)
            Toast.makeText(requireContext(), getString(R.string.songs_added_play_next), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddToQueue() {
        if (songList.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (MusicService.playlist.isEmpty()) {
            handlePlay()
        } else {
            var added = 0
            for (song in songList) {
                if (!MusicService.playlist.any { it.id == song.id }) {
                    MusicService.playlist.add(song)
                    added++
                }
            }
            Toast.makeText(requireContext(), getString(R.string.songs_added_to_queue), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddToPlaylist() {
        if (songList.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        PlaylistPickerBottomSheet(songList).show(parentFragmentManager, "PlaylistPicker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
