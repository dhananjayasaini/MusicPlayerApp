package com.dhananjaysaini.musicplayerapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetAlbumOptionsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream

class AlbumOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAlbumOptionsBinding? = null
    private val binding get() = _binding!!

    private var albumName: String = ""
    private var artistName: String = ""
    private var artUri: String? = null
    private var albumSongs: ArrayList<Music> = arrayListOf()

    var onAlbumUpdated: (() -> Unit)? = null
    var onAlbumCoverChanged: ((String, String) -> Unit)? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { sourceUri ->
            try {
                val ctx = context ?: return@let
                val destFile = File(ctx.filesDir, "album_cover_${albumName.hashCode()}.jpg")
                ctx.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val savedUri = Uri.fromFile(destFile).toString()
                AlbumCoverManager.setCustomCover(ctx, albumName, savedUri)
                Glide.with(this)
                    .load(destFile)
                    .apply(RequestOptions().placeholder(R.drawable.ic_neon_album).centerCrop())
                    .into(binding.ivAlbumArt)
                Toast.makeText(ctx, getString(R.string.toast_album_cover_updated), Toast.LENGTH_SHORT).show()
                onAlbumCoverChanged?.invoke(albumName, savedUri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving cover: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val ARG_ALBUM_NAME = "album_name"
        private const val ARG_ARTIST_NAME = "artist_name"
        private const val ARG_ART_URI = "art_uri"
        private const val ARG_SONGS = "album_songs"

        fun newInstance(
            albumName: String,
            artistName: String,
            artUri: String?,
            songs: List<Music>
        ): AlbumOptionsBottomSheet {
            val sheet = AlbumOptionsBottomSheet()
            val args = Bundle().apply {
                putString(ARG_ALBUM_NAME, albumName)
                putString(ARG_ARTIST_NAME, artistName)
                putString(ARG_ART_URI, artUri)
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
            albumName = args.getString(ARG_ALBUM_NAME, "")
            artistName = args.getString(ARG_ARTIST_NAME, "<unknown>")
            artUri = args.getString(ARG_ART_URI, null)
            val list = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getSerializable(ARG_SONGS, ArrayList::class.java) as? ArrayList<Music>
            } else {
                @Suppress("DEPRECATION")
                args.getSerializable(ARG_SONGS) as? ArrayList<Music>
            }
            if (list != null) {
                albumSongs = list
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_ALBUM_NAME, albumName)
        outState.putString(ARG_ARTIST_NAME, artistName)
        outState.putString(ARG_ART_URI, artUri)
        outState.putSerializable(ARG_SONGS, albumSongs)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAlbumOptionsBinding.inflate(inflater, container, false)
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

        // Header info
        binding.tvAlbumName.text = albumName
        binding.tvAlbumSubtitle.text = "$artistName | ${LocaleHelper.getSongCountString(requireContext(), albumSongs.size)}"

        val customCover = AlbumCoverManager.getCustomCover(requireContext(), albumName)
        val coverToLoad = customCover ?: artUri
        Glide.with(this)
            .load(coverToLoad)
            .apply(RequestOptions().placeholder(R.drawable.ic_neon_album).centerCrop())
            .into(binding.ivAlbumArt)

        // Close button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // 1. Play
        binding.btnPlay.setOnClickListener {
            handlePlay()
            dismiss()
        }

        // 2. Play Next
        binding.btnPlayNext.setOnClickListener {
            handlePlayNext()
            dismiss()
        }

        // 3. Add to Queue
        binding.btnAddToQueue.setOnClickListener {
            handleAddToQueue()
            dismiss()
        }

        // 4. Add to Playlist
        binding.btnAddToPlaylist.setOnClickListener {
            handleAddToPlaylist()
            dismiss()
        }

        // 5. Edit Tags
        binding.btnEditTags.setOnClickListener {
            handleEditTags()
        }

        // 6. Change Album Cover
        binding.btnChangeCover.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun handlePlay() {
        if (albumSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        val mainActivity = activity as? com.dhananjaysaini.musicplayerapp.activities.MainActivity
        if (mainActivity != null) {
            dismiss()
            mainActivity.playAudioTrack(ArrayList(albumSongs), 0)
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
        MusicService.playlist = ArrayList(albumSongs)
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
                .putExtra("source", "album")
        )
    }

    private fun handlePlayNext() {
        if (albumSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (MusicService.playlist.isEmpty()) {
            handlePlay()
        } else {
            MusicService.addSongsToPlayNext(albumSongs)
            Toast.makeText(requireContext(), getString(R.string.songs_added_play_next), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddToQueue() {
        if (albumSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (MusicService.playlist.isEmpty()) {
            handlePlay()
        } else {
            for (song in albumSongs) {
                if (!MusicService.playlist.any { it.id == song.id }) {
                    MusicService.playlist.add(song)
                }
            }
            Toast.makeText(requireContext(), getString(R.string.songs_added_to_queue), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddToPlaylist() {
        if (albumSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        PlaylistPickerBottomSheet(albumSongs).show(parentFragmentManager, "PlaylistPicker")
    }

    private fun handleEditTags() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, pad / 2)
        }

        val etAlbum = EditText(context).apply {
            hint = getString(R.string.edit_song_album_label)
            setText(albumName)
            selectAll()
        }
        layout.addView(etAlbum)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_edit_tags_title)
            .setView(layout)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val newAlbum = etAlbum.text.toString().trim()
                if (newAlbum.isNotEmpty() && newAlbum != albumName) {
                    val mainViewModel = try {
                        ViewModelProvider(requireActivity())[MainViewModel::class.java]
                    } catch (_: Exception) {
                        null
                    }
                    for (song in albumSongs) {
                        EditSongManager.saveEdit(
                            context = context.applicationContext,
                            song = song,
                            newTitle = song.title,
                            newArtist = song.artist,
                            newAlbum = newAlbum,
                            mainViewModel = mainViewModel
                        )
                    }
                    albumName = newAlbum
                    binding.tvAlbumName.text = newAlbum
                    Toast.makeText(context, getString(R.string.toast_tags_saved), Toast.LENGTH_SHORT).show()
                    onAlbumUpdated?.invoke()
                    dismiss()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
