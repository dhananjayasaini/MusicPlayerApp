package com.dhananjaysaini.musicplayerapp.utils

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.HiddenFolderAdapter
import com.dhananjaysaini.musicplayerapp.adapter.HiddenSongAdapter
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetHiddenMusicBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.SongFolder
import com.dhananjaysaini.musicplayerapp.repository.MusicRepository
import com.dhananjaysaini.musicplayerapp.viewmodel.FolderViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HiddenMusicBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHiddenMusicBinding? = null
    private val binding get() = _binding!!

    private val allFolders = mutableListOf<SongFolder>()
    private val hiddenFolders = mutableListOf<SongFolder>()
    private val unhiddenFolders = mutableListOf<SongFolder>()

    private val allSongs = mutableListOf<Music>()
    private val hiddenSongs = mutableListOf<Music>()
    private val unhiddenSongs = mutableListOf<Music>()

    private lateinit var hiddenFolderAdapter: HiddenFolderAdapter
    private lateinit var unhiddenFolderAdapter: HiddenFolderAdapter
    private lateinit var hiddenSongAdapter: HiddenSongAdapter
    private lateinit var unhiddenSongAdapter: HiddenSongAdapter

    private var isHiddenFoldersExpanded = true
    private var isUnhiddenFoldersExpanded = true
    private var isHiddenSongsExpanded = true
    private var isUnhiddenSongsExpanded = true

    private var dataChanged = false

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
        _binding = BottomSheetHiddenMusicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ThemeManager.applyThemeToBottomSheet(requireContext(), binding.root)

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        setupTabs()
        setupRecyclers()
        setupChevrons()
        loadData()
    }

    private fun setupTabs() {
        binding.tabFolders.setOnClickListener {
            switchTab(isFolders = true)
        }

        binding.tabSongs.setOnClickListener {
            switchTab(isFolders = false)
        }
    }

    private fun switchTab(isFolders: Boolean) {
        if (isFolders) {
            binding.tvTabFolders.setTextColor(Color.WHITE)
            binding.indicatorFolders.visibility = View.VISIBLE

            binding.tvTabSongs.setTextColor(Color.parseColor("#8E8E93"))
            binding.indicatorSongs.visibility = View.INVISIBLE

            binding.layoutFoldersScroll.visibility = View.VISIBLE
            binding.layoutSongsScroll.visibility = View.GONE
        } else {
            binding.tvTabSongs.setTextColor(Color.WHITE)
            binding.indicatorSongs.visibility = View.VISIBLE

            binding.tvTabFolders.setTextColor(Color.parseColor("#8E8E93"))
            binding.indicatorFolders.visibility = View.INVISIBLE

            binding.layoutSongsScroll.visibility = View.VISIBLE
            binding.layoutFoldersScroll.visibility = View.GONE
        }
    }

    private fun setupRecyclers() {
        // Folders - Hidden
        hiddenFolderAdapter = HiddenFolderAdapter(hiddenFolders, isHiddenSection = true) { folder ->
            onUnhideFolderClicked(folder)
        }
        binding.rvHiddenFolders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHiddenFolders.adapter = hiddenFolderAdapter

        // Folders - Unhidden
        unhiddenFolderAdapter = HiddenFolderAdapter(unhiddenFolders, isHiddenSection = false) { folder ->
            onHideFolderClicked(folder)
        }
        binding.rvUnhiddenFolders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUnhiddenFolders.adapter = unhiddenFolderAdapter

        // Songs - Hidden
        hiddenSongAdapter = HiddenSongAdapter(hiddenSongs, isHiddenSection = true) { song ->
            onUnhideSongClicked(song)
        }
        binding.rvHiddenSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHiddenSongs.adapter = hiddenSongAdapter

        // Songs - Unhidden
        unhiddenSongAdapter = HiddenSongAdapter(unhiddenSongs, isHiddenSection = false) { song ->
            onHideSongClicked(song)
        }
        binding.rvUnhiddenSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUnhiddenSongs.adapter = unhiddenSongAdapter
    }

    private fun setupChevrons() {
        // Hidden Folders
        binding.headerHiddenFolders.setOnClickListener {
            isHiddenFoldersExpanded = !isHiddenFoldersExpanded
            binding.rvHiddenFolders.visibility =
                if (isHiddenFoldersExpanded && hiddenFolders.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvEmptyHiddenFolders.visibility =
                if (isHiddenFoldersExpanded && hiddenFolders.isEmpty()) View.VISIBLE else View.GONE
            binding.ivChevronHiddenFolders.setImageResource(
                if (isHiddenFoldersExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
        }

        // Unhidden Folders
        binding.headerUnhiddenFolders.setOnClickListener {
            isUnhiddenFoldersExpanded = !isUnhiddenFoldersExpanded
            binding.rvUnhiddenFolders.visibility =
                if (isUnhiddenFoldersExpanded && unhiddenFolders.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvEmptyUnhiddenFolders.visibility =
                if (isUnhiddenFoldersExpanded && unhiddenFolders.isEmpty()) View.VISIBLE else View.GONE
            binding.ivChevronUnhiddenFolders.setImageResource(
                if (isUnhiddenFoldersExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
        }

        // Hidden Songs
        binding.headerHiddenSongs.setOnClickListener {
            isHiddenSongsExpanded = !isHiddenSongsExpanded
            binding.rvHiddenSongs.visibility =
                if (isHiddenSongsExpanded && hiddenSongs.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvEmptyHiddenSongs.visibility =
                if (isHiddenSongsExpanded && hiddenSongs.isEmpty()) View.VISIBLE else View.GONE
            binding.ivChevronHiddenSongs.setImageResource(
                if (isHiddenSongsExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
        }

        // Unhidden Songs
        binding.headerUnhiddenSongs.setOnClickListener {
            isUnhiddenSongsExpanded = !isUnhiddenSongsExpanded
            binding.rvUnhiddenSongs.visibility =
                if (isUnhiddenSongsExpanded && unhiddenSongs.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvEmptyUnhiddenSongs.visibility =
                if (isUnhiddenSongsExpanded && unhiddenSongs.isEmpty()) View.VISIBLE else View.GONE
            binding.ivChevronUnhiddenSongs.setImageResource(
                if (isUnhiddenSongsExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
        }
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val context = requireContext().applicationContext
            HiddenSongManager.init(context)

            val rawFolders = getMusicFolders(context)
            val repo = MusicRepository(context)
            val rawSongs = repo.fetchAllSongsRaw()

            withContext(Dispatchers.Main) {
                if (_binding == null) return@withContext
                binding.progressBar.visibility = View.GONE

                allFolders.clear()
                allFolders.addAll(rawFolders)

                allSongs.clear()
                allSongs.addAll(rawSongs)

                refreshFolderLists()
                refreshSongLists()
            }
        }
    }

    private fun refreshFolderLists() {
        hiddenFolders.clear()
        unhiddenFolders.clear()

        allFolders.forEach { folder ->
            if (HiddenSongManager.isFolderHidden(folder.path)) {
                hiddenFolders.add(folder)
            } else {
                unhiddenFolders.add(folder)
            }
        }

        hiddenFolders.sortBy { it.name.lowercase() }
        unhiddenFolders.sortBy { it.name.lowercase() }

        binding.tvHeaderHiddenFoldersCount.text = "(${hiddenFolders.size})"
        binding.tvHeaderUnhiddenFoldersCount.text = "(${unhiddenFolders.size})"

        hiddenFolderAdapter.notifyDataSetChanged()
        unhiddenFolderAdapter.notifyDataSetChanged()

        binding.rvHiddenFolders.visibility =
            if (isHiddenFoldersExpanded && hiddenFolders.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvEmptyHiddenFolders.visibility =
            if (isHiddenFoldersExpanded && hiddenFolders.isEmpty()) View.VISIBLE else View.GONE

        binding.rvUnhiddenFolders.visibility =
            if (isUnhiddenFoldersExpanded && unhiddenFolders.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvEmptyUnhiddenFolders.visibility =
            if (isUnhiddenFoldersExpanded && unhiddenFolders.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun refreshSongLists() {
        hiddenSongs.clear()
        unhiddenSongs.clear()

        allSongs.forEach { song ->
            if (HiddenSongManager.isSongDirectlyHidden(song.id)) {
                hiddenSongs.add(song)
            } else {
                unhiddenSongs.add(song)
            }
        }

        hiddenSongs.sortBy { it.title.lowercase() }
        unhiddenSongs.sortBy { it.title.lowercase() }

        binding.tvHeaderHiddenSongsCount.text = "(${hiddenSongs.size})"
        binding.tvHeaderUnhiddenSongsCount.text = "(${unhiddenSongs.size})"

        hiddenSongAdapter.notifyDataSetChanged()
        unhiddenSongAdapter.notifyDataSetChanged()

        binding.rvHiddenSongs.visibility =
            if (isHiddenSongsExpanded && hiddenSongs.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvEmptyHiddenSongs.visibility =
            if (isHiddenSongsExpanded && hiddenSongs.isEmpty()) View.VISIBLE else View.GONE

        binding.rvUnhiddenSongs.visibility =
            if (isUnhiddenSongsExpanded && unhiddenSongs.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvEmptyUnhiddenSongs.visibility =
            if (isUnhiddenSongsExpanded && unhiddenSongs.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onUnhideFolderClicked(folder: SongFolder) {
        val context = requireContext()
        HiddenSongManager.unhideFolder(context, folder.path)
        dataChanged = true
        refreshFolderLists()
    }

    private fun onHideFolderClicked(folder: SongFolder) {
        val context = requireContext()
        HiddenSongManager.hideFolder(context, folder.path)
        dataChanged = true
        refreshFolderLists()
    }

    private fun onUnhideSongClicked(song: Music) {
        val context = requireContext()
        HiddenSongManager.unhideSong(context, song.id)
        dataChanged = true
        refreshSongLists()
    }

    private fun onHideSongClicked(song: Music) {
        val context = requireContext()
        HiddenSongManager.hideSong(context, song.id)
        dataChanged = true
        refreshSongLists()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (dataChanged && isAdded) {
            try {
                val mainVM = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
                mainVM.loadMusic(requireContext(), force = true)
            } catch (e: Exception) {
                // Ignore if activity is finishing
            }
            try {
                val folderVM = ViewModelProvider(requireActivity()).get(FolderViewModel::class.java)
                folderVM.loadFolders(requireContext())
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
