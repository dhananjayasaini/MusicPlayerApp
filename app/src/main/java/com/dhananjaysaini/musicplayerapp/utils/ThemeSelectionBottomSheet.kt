package com.dhananjaysaini.musicplayerapp.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.ThemeAdapter
import com.dhananjaysaini.musicplayerapp.databinding.FragmentThemeSelectionBinding
import com.dhananjaysaini.musicplayerapp.model.AppTheme
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ThemeSelectionBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentThemeSelectionBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        binding = FragmentThemeSelectionBinding.inflate(inflater, container, false)

        val themeList = com.dhananjaysaini.musicplayerapp.repository.ThemeRepository.getColorThemes()

        val themeAdapter = ThemeAdapter(requireContext(), themeList)

        binding.themeRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.themeRecycler.adapter = themeAdapter

        themeAdapter.onThemeClick = { theme ->
            ThemeManager.saveTheme(requireContext(), theme)
            ThemeManager.applyThemeToActivity(requireActivity())
            dismiss()
        }

        return binding.root
    }
}