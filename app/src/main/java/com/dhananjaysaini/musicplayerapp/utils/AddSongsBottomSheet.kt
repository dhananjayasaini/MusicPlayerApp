package com.dhananjaysaini.musicplayerapp.utils

import android.app.Dialog
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.SelectSongsAdapter
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetAddSongsBinding
import com.dhananjaysaini.musicplayerapp.databinding.PopupAddSongsFilterBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddSongsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddSongsBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectSongsAdapter: SelectSongsAdapter
    private val mainViewModel: MainViewModel by activityViewModels()

    var onSongsSelected: ((List<Music>) -> Unit)? = null

    private var excludedSongIds: HashSet<String> = hashSetOf()

    private var allLibrarySongs: ArrayList<Music> = arrayListOf()
    private var currentFilter: FilterType = FilterType.ALL

    enum class FilterType {
        ALL, RECENT_ADDED, RECENT_PLAYED, TOP_TRACKS
    }

    companion object {
        private const val ARG_EXCLUDED_IDS = "arg_excluded_ids"

        fun newInstance(excludedSongIds: List<String> = emptyList()): AddSongsBottomSheet {
            val fragment = AddSongsBottomSheet()
            val args = Bundle()
            args.putStringArrayList(ARG_EXCLUDED_IDS, ArrayList(excludedSongIds))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            val ids = args.getStringArrayList(ARG_EXCLUDED_IDS)
            if (ids != null) {
                excludedSongIds = HashSet(ids)
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
        _binding = BottomSheetAddSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ThemeManager.applyThemeToBottomSheet(requireContext(), binding.root)

        loadInitialSongs()
        setupRecyclerView()
        setupSearch()
        setupListeners()
        updateSelectionUI(0, false)
    }

    private fun loadInitialSongs() {
        val rawSongs = MusicService.allSongs.ifEmpty {
            mainViewModel.musicListLiveData.value ?: emptyList()
        }
        // Filter out songs that are already in the playlist/favourites
        allLibrarySongs = if (excludedSongIds.isNotEmpty()) {
            ArrayList(rawSongs.filter { it.id !in excludedSongIds })
        } else {
            ArrayList(rawSongs)
        }
    }

    private fun setupRecyclerView() {
        selectSongsAdapter = SelectSongsAdapter(requireContext(), allLibrarySongs)
        binding.rvAddSongs.apply {
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

        binding.layoutFilterDropdown.setOnClickListener {
            showFilterDropdown(it)
        }

        binding.btnOk.setOnClickListener {
            val selected = selectSongsAdapter.getSelectedSongs()
            if (selected.isNotEmpty()) {
                onSongsSelected?.invoke(selected)
                dismiss()
            }
        }
    }

    private fun updateSelectionUI(count: Int, isAllSelected: Boolean) {
        if (_binding == null) return

        if (count == 0) {
            binding.tvAddSongsTitle.text = getString(R.string.btn_add_songs)
            binding.btnOk.visibility = View.GONE
        } else {
            binding.tvAddSongsTitle.text = getString(R.string.selected_count, count)
            binding.btnOk.visibility = View.VISIBLE
        }

        if (isAllSelected) {
            binding.ivSelectAll.setImageResource(R.drawable.ic_circle_checked)
        } else {
            binding.ivSelectAll.setImageResource(R.drawable.ic_circle_unchecked)
        }
    }

    private fun showFilterDropdown(anchor: View) {
        val popupBinding = PopupAddSongsFilterBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 16f

        val accentColor = Color.parseColor("#FFA000")
        val normalColor = Color.WHITE

        popupBinding.menuFilterAllSongs.setTextColor(if (currentFilter == FilterType.ALL) accentColor else normalColor)
        popupBinding.menuFilterRecentlyAdded.setTextColor(if (currentFilter == FilterType.RECENT_ADDED) accentColor else normalColor)
        popupBinding.menuFilterRecentlyPlayed.setTextColor(if (currentFilter == FilterType.RECENT_PLAYED) accentColor else normalColor)
        popupBinding.menuFilterTopTracks.setTextColor(if (currentFilter == FilterType.TOP_TRACKS) accentColor else normalColor)

        popupBinding.menuFilterAllSongs.setOnClickListener {
            popupWindow.dismiss()
            applyFilter(FilterType.ALL, getString(R.string.filter_all_songs))
        }

        popupBinding.menuFilterRecentlyAdded.setOnClickListener {
            popupWindow.dismiss()
            applyFilter(FilterType.RECENT_ADDED, getString(R.string.filter_recently_added))
        }

        popupBinding.menuFilterRecentlyPlayed.setOnClickListener {
            popupWindow.dismiss()
            applyFilter(FilterType.RECENT_PLAYED, getString(R.string.filter_recently_played))
        }

        popupBinding.menuFilterTopTracks.setOnClickListener {
            popupWindow.dismiss()
            applyFilter(FilterType.TOP_TRACKS, getString(R.string.filter_top_tracks))
        }

        popupWindow.showAsDropDown(anchor, 0, 4, Gravity.END)
    }

    private fun applyFilter(filter: FilterType, filterDisplayName: String) {
        currentFilter = filter
        binding.tvFilterName.text = filterDisplayName

        viewLifecycleOwner.lifecycleScope.launch {
            val filteredSongs = when (filter) {
                FilterType.ALL -> allLibrarySongs
                FilterType.RECENT_ADDED -> ArrayList(allLibrarySongs.sortedByDescending { it.date })
                FilterType.RECENT_PLAYED -> {
                    withContext(Dispatchers.IO) {
                        val history = MusicDatabase.getDatabase(requireContext()).playHistoryDao().getRecentlyPlayedSync()
                        val map = allLibrarySongs.associateBy { it.id }
                        val result = ArrayList<Music>()
                        history.forEach { item -> map[item.songId]?.let { result.add(it) } }
                        result
                    }
                }
                FilterType.TOP_TRACKS -> {
                    withContext(Dispatchers.IO) {
                        val history = MusicDatabase.getDatabase(requireContext()).playHistoryDao().getTopPlayedSync()
                        val map = allLibrarySongs.associateBy { it.id }
                        val result = ArrayList<Music>()
                        history.forEach { item -> map[item.songId]?.let { result.add(it) } }
                        result
                    }
                }
            }

            selectSongsAdapter.updateSongs(filteredSongs)
            val currentQuery = binding.etSearchSongs.text?.toString() ?: ""
            if (currentQuery.isNotEmpty()) {
                selectSongsAdapter.filter(currentQuery)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
