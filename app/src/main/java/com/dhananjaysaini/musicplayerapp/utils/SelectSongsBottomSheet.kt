package com.dhananjaysaini.musicplayerapp.utils

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.MainActivity
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.adapter.SelectSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetSelectSongsBinding
import com.dhananjaysaini.musicplayerapp.databinding.PopupSelectSongsOverflowBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SelectSongsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSelectSongsBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectSongsAdapter: SelectSongsAdapter
    private val mainViewModel: MainViewModel by activityViewModels()

    private var initialSongs: ArrayList<Music> = arrayListOf()

    companion object {
        private const val ARG_SONGS = "arg_songs"

        fun newInstance(songs: ArrayList<Music>): SelectSongsBottomSheet {
            val fragment = SelectSongsBottomSheet()
            val args = Bundle()
            args.putSerializable(ARG_SONGS, songs)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                args.getSerializable(ARG_SONGS, ArrayList::class.java) as? ArrayList<Music>
            } else {
                @Suppress("DEPRECATION")
                args.getSerializable(ARG_SONGS) as? ArrayList<Music>
            }
            if (list != null) {
                initialSongs = list
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.setBackgroundResource(android.R.color.transparent)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                sheet.requestLayout()
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ThemeManager.applyThemeToBottomSheet(requireContext(), binding.root)

        setupRecyclerView()
        setupSearch()
        setupListeners()
        updateSelectionUI(0, false)
    }

    private fun setupRecyclerView() {
        if (initialSongs.isEmpty()) {
            val currentList = mainViewModel.musicListLiveData.value ?: emptyList()
            initialSongs = ArrayList(currentList)
        }

        selectSongsAdapter = SelectSongsAdapter(requireContext(), initialSongs)
        binding.rvSelectSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectSongsAdapter
            setHasFixedSize(true)
        }

        selectSongsAdapter.onSelectionChanged = { count, isAllSelected ->
            updateSelectionUI(count, isAllSelected)
        }
    }

    private fun setupSearch() {
        binding.etSearchSongs.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                selectSongsAdapter.filter(query)
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchSongs.setText("")
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.rowSelectAll.setOnClickListener {
            selectSongsAdapter.toggleSelectAll()
        }

        binding.btnMore.setOnClickListener {
            showOverflowMenu(it)
        }

        binding.btnBottomPlay.setOnClickListener {
            val selected = selectSongsAdapter.getSelectedSongs()
            if (selected.isNotEmpty()) {
                playSongs(ArrayList(selected))
                dismiss()
            }
        }

        binding.btnBottomAddPlaylist.setOnClickListener {
            val selected = selectSongsAdapter.getSelectedSongs()
            if (selected.isNotEmpty()) {
                PlaylistPickerBottomSheet(ArrayList(selected)).show(parentFragmentManager, "PlaylistPicker")
                dismiss()
            }
        }

        binding.btnBottomDelete.setOnClickListener {
            val selected = selectSongsAdapter.getSelectedSongs()
            if (selected.isNotEmpty()) {
                showDeleteConfirmDialog(selected)
            }
        }
    }

    private fun updateSelectionUI(count: Int, isAllSelected: Boolean) {
        if (_binding == null) return

        if (count == 0) {
            binding.tvSelectSongsTitle.text = getString(R.string.select_songs)
            setBottomButtonsEnabled(false)
        } else {
            binding.tvSelectSongsTitle.text = getString(R.string.selected_count, count)
            setBottomButtonsEnabled(true)
        }

        if (isAllSelected) {
            binding.ivSelectAll.setImageResource(R.drawable.ic_circle_checked)
        } else {
            binding.ivSelectAll.setImageResource(R.drawable.ic_circle_unchecked)
        }
    }

    private fun setBottomButtonsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.4f

        binding.btnBottomPlay.isEnabled = enabled
        binding.btnBottomPlay.alpha = alpha

        binding.btnBottomAddPlaylist.isEnabled = enabled
        binding.btnBottomAddPlaylist.alpha = alpha

        binding.btnBottomDelete.isEnabled = enabled
        binding.btnBottomDelete.alpha = alpha
    }

    private fun showOverflowMenu(anchor: View) {
        val popupBinding = PopupSelectSongsOverflowBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 16f

        val selectedSongs = selectSongsAdapter.getSelectedSongs()
        val hasSelection = selectedSongs.isNotEmpty()
        val itemAlpha = if (hasSelection) 1.0f else 0.45f

        popupBinding.menuPlayNext.isEnabled = hasSelection
        popupBinding.menuPlayNext.alpha = itemAlpha

        popupBinding.menuAddToQueue.isEnabled = hasSelection
        popupBinding.menuAddToQueue.alpha = itemAlpha

        popupBinding.menuHideSong.isEnabled = hasSelection
        popupBinding.menuHideSong.alpha = itemAlpha
        if (selectedSongs.size > 1) {
            popupBinding.menuHideSong.text = getString(R.string.action_hide_songs)
        } else {
            popupBinding.menuHideSong.text = getString(R.string.action_hide_song)
        }

        popupBinding.menuPlayNext.setOnClickListener {
            popupWindow.dismiss()
            if (selectedSongs.isNotEmpty()) {
                handlePlayNext(selectedSongs)
                dismiss()
            }
        }

        popupBinding.menuAddToQueue.setOnClickListener {
            popupWindow.dismiss()
            if (selectedSongs.isNotEmpty()) {
                handleAddToQueue(selectedSongs)
                dismiss()
            }
        }

        popupBinding.menuHideSong.setOnClickListener {
            popupWindow.dismiss()
            if (selectedSongs.isNotEmpty()) {
                handleHideSongs(selectedSongs)
                dismiss()
            }
        }

        popupWindow.showAsDropDown(anchor, -140, 0, Gravity.END)
    }

    private fun playSongs(songs: ArrayList<Music>) {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            mainActivity.playAudioTrack(songs, 0)
            return
        }

        MusicService.playlist = ArrayList(songs)
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

        startActivity(Intent(requireContext(), PlayerActivity::class.java))
    }

    private fun handlePlayNext(songs: List<Music>) {
        if (MusicService.playlist.isEmpty()) {
            MusicService.playlist.addAll(songs)
        } else {
            val insertIndex = (MusicService.position + 1).coerceIn(0, MusicService.playlist.size)
            MusicService.playlist.addAll(insertIndex, songs)
        }
        for (song in songs) {
            QueueManager.playNext(song)
        }
        Toast.makeText(requireContext(), getString(R.string.added_to_play_next), Toast.LENGTH_SHORT).show()
    }

    private fun handleAddToQueue(songs: List<Music>) {
        MusicService.playlist.addAll(songs)
        for (song in songs) {
            QueueManager.addToQueue(song)
        }
        Toast.makeText(requireContext(), getString(R.string.added_to_queue), Toast.LENGTH_SHORT).show()
    }

    private fun handleHideSongs(songs: List<Music>) {
        val ctx = requireContext()
        for (song in songs) {
            HiddenSongManager.hideSong(ctx, song.id)
        }
        val current = mainViewModel.musicListLiveData.value ?: arrayListOf()
        val idsToHide = songs.map { it.id }.toSet()
        current.removeAll { idsToHide.contains(it.id) }
        mainViewModel.musicListLiveData.value = current

        Toast.makeText(ctx, getString(R.string.hide_songs_toast), Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmDialog(songs: List<Music>) {
        val currentActivity = activity ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_songs_dialog_title)
            .setMessage(getString(R.string.delete_songs_dialog_message, songs.size))
            .setPositiveButton(R.string.btn_ok) { _, _ ->
                DeleteManager.deleteSongs(currentActivity, songs)
                dismiss()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
