package com.dhananjaysaini.musicplayerapp.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetSortSongsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SortSongsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSortSongsBinding? = null
    private val binding get() = _binding!!

    var onSortApplied: (() -> Unit)? = null

    private var selectedType: SongSortManager.SortType = SongSortManager.SortType.SONG_NAME
    private var selectedOrder: SongSortManager.SortOrder = SongSortManager.SortOrder.ASCENDING

    private val accentColor: Int = Color.parseColor("#FFA000")
    private val defaultTextColor: Int = Color.WHITE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSortSongsBinding.inflate(inflater, container, false)
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

        // Load current sort state
        selectedType = SongSortManager.getSortType(requireContext())
        selectedOrder = SongSortManager.getSortOrder(requireContext())

        setupListeners()
        updateUI()
    }

    private fun setupListeners() {
        binding.rowSongName.setOnClickListener {
            selectedType = SongSortManager.SortType.SONG_NAME
            updateUI()
        }
        binding.rowArtistName.setOnClickListener {
            selectedType = SongSortManager.SortType.ARTIST_NAME
            updateUI()
        }
        binding.rowAlbumName.setOnClickListener {
            selectedType = SongSortManager.SortType.ALBUM_NAME
            updateUI()
        }
        binding.rowFolderName.setOnClickListener {
            selectedType = SongSortManager.SortType.FOLDER_NAME
            updateUI()
        }
        binding.rowAddedTime.setOnClickListener {
            selectedType = SongSortManager.SortType.ADDED_TIME
            updateUI()
        }
        binding.rowPlayCount.setOnClickListener {
            selectedType = SongSortManager.SortType.PLAY_COUNT
            updateUI()
        }
        binding.rowYear.setOnClickListener {
            selectedType = SongSortManager.SortType.YEAR
            updateUI()
        }
        binding.rowDuration.setOnClickListener {
            selectedType = SongSortManager.SortType.DURATION
            updateUI()
        }
        binding.rowSize.setOnClickListener {
            selectedType = SongSortManager.SortType.SIZE
            updateUI()
        }
        binding.rowRandom.setOnClickListener {
            selectedType = SongSortManager.SortType.RANDOM
            updateUI()
        }

        binding.rowFromAtoZ.setOnClickListener {
            selectedOrder = SongSortManager.SortOrder.ASCENDING
            updateUI()
        }
        binding.rowFromZtoA.setOnClickListener {
            selectedOrder = SongSortManager.SortOrder.DESCENDING
            updateUI()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnOk.setOnClickListener {
            SongSortManager.saveSort(requireContext(), selectedType, selectedOrder)
            onSortApplied?.invoke()
            dismiss()
        }
    }

    private fun updateUI() {
        val typeRows = listOf(
            Triple(SongSortManager.SortType.SONG_NAME, binding.tvSongName, binding.ivSongName),
            Triple(SongSortManager.SortType.ARTIST_NAME, binding.tvArtistName, binding.ivArtistName),
            Triple(SongSortManager.SortType.ALBUM_NAME, binding.tvAlbumName, binding.ivAlbumName),
            Triple(SongSortManager.SortType.FOLDER_NAME, binding.tvFolderName, binding.ivFolderName),
            Triple(SongSortManager.SortType.ADDED_TIME, binding.tvAddedTime, binding.ivAddedTime),
            Triple(SongSortManager.SortType.PLAY_COUNT, binding.tvPlayCount, binding.ivPlayCount),
            Triple(SongSortManager.SortType.YEAR, binding.tvYear, binding.ivYear),
            Triple(SongSortManager.SortType.DURATION, binding.tvDuration, binding.ivDuration),
            Triple(SongSortManager.SortType.SIZE, binding.tvSize, binding.ivSize),
            Triple(SongSortManager.SortType.RANDOM, binding.tvRandom, binding.ivRandom)
        )

        for ((type, textView, imageView) in typeRows) {
            val isSelected = (type == selectedType)
            applyOptionStyle(textView, imageView, isSelected)
        }

        val isAscending = (selectedOrder == SongSortManager.SortOrder.ASCENDING)
        applyOptionStyle(binding.tvFromAtoZ, binding.ivFromAtoZ, isAscending)
        applyOptionStyle(binding.tvFromZtoA, binding.ivFromZtoA, !isAscending)
    }

    private fun applyOptionStyle(textView: TextView, imageView: ImageView, isSelected: Boolean) {
        if (isSelected) {
            textView.setTextColor(accentColor)
            imageView.setImageResource(R.drawable.ic_sort_radio_checked)
        } else {
            textView.setTextColor(defaultTextColor)
            imageView.setImageResource(R.drawable.ic_sort_radio_unchecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
