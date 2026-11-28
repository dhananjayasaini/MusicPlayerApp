package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetArtistOptionsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ArtistOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetArtistOptionsBinding? = null
    private val binding get() = _binding!!

    private var artistName: String = ""
    private var albumCount: Int = 1
    private var artUri: String? = null
    private var artistSongs: ArrayList<Music> = ArrayList()

    var onArtistUpdated: (() -> Unit)? = null

    companion object {
        private const val ARG_ARTIST_NAME = "artist_name"
        private const val ARG_ALBUM_COUNT = "album_count"
        private const val ARG_ART_URI = "art_uri"
        private const val ARG_SONGS = "artist_songs"

        fun newInstance(
            artistName: String,
            albumCount: Int,
            artUri: String?,
            songs: List<Music>
        ): ArtistOptionsBottomSheet {
            val sheet = ArtistOptionsBottomSheet()
            val args = Bundle().apply {
                putString(ARG_ARTIST_NAME, artistName)
                putInt(ARG_ALBUM_COUNT, albumCount)
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
            artistName = args.getString(ARG_ARTIST_NAME, "")
            albumCount = args.getInt(ARG_ALBUM_COUNT, 1)
            artUri = args.getString(ARG_ART_URI, null)
            val list = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getSerializable(ARG_SONGS, ArrayList::class.java) as? ArrayList<Music>
            } else {
                @Suppress("DEPRECATION")
                args.getSerializable(ARG_SONGS) as? ArrayList<Music>
            }
            if (list != null) {
                artistSongs = list
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_ARTIST_NAME, artistName)
        outState.putInt(ARG_ALBUM_COUNT, albumCount)
        outState.putString(ARG_ART_URI, artUri)
        outState.putSerializable(ARG_SONGS, artistSongs)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetArtistOptionsBinding.inflate(inflater, container, false)
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
        binding.tvArtistName.text = artistName

        val albumStr = LocaleHelper.getAlbumCountString(requireContext(), albumCount).lowercase()
        val songStr = LocaleHelper.getSongCountString(requireContext(), artistSongs.size).lowercase()
        binding.tvArtistSubtitle.text = "$albumStr | $songStr"

        val initial = ArtistAvatarHelper.getInitial(artistName)
        if (initial != null) {
            binding.tvArtistInitial.visibility = View.VISIBLE
            binding.tvArtistInitial.text = initial
            binding.tvArtistInitial.setTextColor(ArtistAvatarHelper.getColorForName(artistName))
            binding.ivArtistIconFallback.visibility = View.GONE
        } else {
            binding.tvArtistInitial.visibility = View.GONE
            binding.ivArtistIconFallback.visibility = View.VISIBLE
        }

        if (!artUri.isNullOrEmpty()) {
            binding.ivArtistArt.visibility = View.VISIBLE
            Glide.with(this)
                .load(artUri)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.ivArtistArt.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(binding.ivArtistArt)
        } else {
            binding.ivArtistArt.visibility = View.GONE
        }

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
    }

    private fun handlePlay() {
        if (artistSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        val mainActivity = activity as? com.dhananjaysaini.musicplayerapp.activities.MainActivity
        if (mainActivity != null) {
            dismiss()
            mainActivity.playAudioTrack(ArrayList(artistSongs), 0)
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
        MusicService.playlist = ArrayList(artistSongs)
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
                .putExtra("source", "artist")
        )
    }

    private fun handlePlayNext() {
        if (artistSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (MusicService.playlist.isEmpty()) {
            handlePlay()
        } else {
            MusicService.addSongsToPlayNext(artistSongs)
            Toast.makeText(requireContext(), getString(R.string.songs_added_play_next), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddToQueue() {
        if (artistSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (MusicService.playlist.isEmpty()) {
            handlePlay()
        } else {
            for (song in artistSongs) {
                if (!MusicService.playlist.any { it.id == song.id }) {
                    MusicService.playlist.add(song)
                }
            }
            Toast.makeText(requireContext(), getString(R.string.songs_added_to_queue), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddToPlaylist() {
        if (artistSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        PlaylistPickerBottomSheet(artistSongs).show(parentFragmentManager, "PlaylistPicker")
    }

    private fun handleEditTags() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, pad / 2)
        }

        val etArtist = EditText(context).apply {
            hint = getString(R.string.edit_song_artist_label)
            setText(artistName)
            selectAll()
        }
        layout.addView(etArtist)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_edit_artist_tags_title)
            .setView(layout)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val newArtist = etArtist.text.toString().trim()
                if (newArtist.isNotEmpty() && newArtist != artistName) {
                    val mainViewModel = try {
                        ViewModelProvider(requireActivity())[MainViewModel::class.java]
                    } catch (_: Exception) {
                        null
                    }
                    for (song in artistSongs) {
                        EditSongManager.saveEdit(
                            context = context.applicationContext,
                            song = song,
                            newTitle = song.title,
                            newArtist = newArtist,
                            newAlbum = song.album,
                            mainViewModel = mainViewModel
                        )
                    }
                    artistName = newArtist
                    binding.tvArtistName.text = newArtist
                    Toast.makeText(context, getString(R.string.toast_tags_saved), Toast.LENGTH_SHORT).show()
                    onArtistUpdated?.invoke()
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
