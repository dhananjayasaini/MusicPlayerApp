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
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetGenreOptionsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GenreOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGenreOptionsBinding? = null
    private val binding get() = _binding!!

    private var genreName: String = ""
    private var artUri: String? = null
    private var genreSongs: ArrayList<Music> = ArrayList()

    var onGenreUpdated: (() -> Unit)? = null

    companion object {
        private const val ARG_GENRE_NAME = "genre_name"
        private const val ARG_ART_URI = "art_uri"
        private const val ARG_SONGS = "genre_songs"

        fun newInstance(
            genreName: String,
            artUri: String?,
            songs: List<Music>
        ): GenreOptionsBottomSheet {
            val sheet = GenreOptionsBottomSheet()
            val args = Bundle().apply {
                putString(ARG_GENRE_NAME, genreName)
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
            genreName = args.getString(ARG_GENRE_NAME, "")
            artUri = args.getString(ARG_ART_URI, null)
            val list = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                args.getSerializable(ARG_SONGS, ArrayList::class.java) as? ArrayList<Music>
            } else {
                @Suppress("DEPRECATION")
                args.getSerializable(ARG_SONGS) as? ArrayList<Music>
            }
            if (list != null) {
                genreSongs = list
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_GENRE_NAME, genreName)
        outState.putString(ARG_ART_URI, artUri)
        outState.putSerializable(ARG_SONGS, genreSongs)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetGenreOptionsBinding.inflate(inflater, container, false)
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
        binding.tvGenreName.text = genreName
        binding.tvGenreSubtitle.text = LocaleHelper.getSongCountString(requireContext(), genreSongs.size)

        val initial = ArtistAvatarHelper.getInitial(genreName)
        if (initial != null) {
            binding.tvGenreInitial.visibility = View.VISIBLE
            binding.tvGenreInitial.text = initial
            binding.tvGenreInitial.setTextColor(ArtistAvatarHelper.getColorForName(genreName))
            binding.ivGenreIconFallback.visibility = View.GONE
        } else {
            binding.tvGenreInitial.visibility = View.GONE
            binding.ivGenreIconFallback.visibility = View.VISIBLE
        }

        if (!artUri.isNullOrEmpty()) {
            binding.ivGenreArt.visibility = View.VISIBLE
            Glide.with(this)
                .load(artUri)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.ivGenreArt.visibility = View.GONE
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
                .into(binding.ivGenreArt)
        } else {
            binding.ivGenreArt.visibility = View.GONE
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
        if (genreSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        val mainActivity = activity as? com.dhananjaysaini.musicplayerapp.activities.MainActivity
        if (mainActivity != null) {
            dismiss()
            mainActivity.playAudioTrack(ArrayList(genreSongs), 0)
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
        MusicService.playlist = ArrayList(genreSongs)
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
                .putExtra("source", "genre")
        )
    }

    private fun handlePlayNext() {
        if (genreSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (MusicService.playlist.isEmpty()) {
            handlePlay()
        } else {
            MusicService.addSongsToPlayNext(genreSongs)
            Toast.makeText(requireContext(), getString(R.string.songs_added_play_next), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddToQueue() {
        if (genreSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        if (MusicService.playlist.isEmpty()) {
            handlePlay()
        } else {
            for (song in genreSongs) {
                if (!MusicService.playlist.any { it.id == song.id }) {
                    MusicService.playlist.add(song)
                }
            }
            Toast.makeText(requireContext(), getString(R.string.songs_added_to_queue), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddToPlaylist() {
        if (genreSongs.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.playlist_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }
        PlaylistPickerBottomSheet(genreSongs).show(parentFragmentManager, "PlaylistPicker")
    }

    private fun handleEditTags() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, pad / 2)
        }

        val etGenre = EditText(context).apply {
            hint = getString(R.string.edit_song_genre_label)
            setText(genreName)
            selectAll()
        }
        layout.addView(etGenre)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_edit_genre_tags_title)
            .setView(layout)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val newGenre = etGenre.text.toString().trim()
                if (newGenre.isNotEmpty() && newGenre != genreName) {
                    val mainViewModel = try {
                        ViewModelProvider(requireActivity())[MainViewModel::class.java]
                    } catch (_: Exception) {
                        null
                    }
                    for (song in genreSongs) {
                        EditSongManager.saveEdit(
                            context = context.applicationContext,
                            song = song,
                            newTitle = song.title,
                            newArtist = song.artist,
                            newAlbum = song.album,
                            mainViewModel = mainViewModel,
                            newGenre = newGenre
                        )
                    }
                    genreName = newGenre
                    binding.tvGenreName.text = newGenre
                    Toast.makeText(context, getString(R.string.toast_tags_saved), Toast.LENGTH_SHORT).show()
                    onGenreUpdated?.invoke()
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
