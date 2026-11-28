package com.dhananjaysaini.musicplayerapp.utils

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.activities.MainActivity
import com.dhananjaysaini.musicplayerapp.adapter.LanguageAdapter
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetLanguageBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LanguageBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLanguageBinding? = null
    private val binding get() = _binding!!

    var onLanguageChanged: ((LocaleHelper.LanguageItem) -> Unit)? = null
    private var languageAdapter: LanguageAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentCode = LocaleHelper.getSavedLanguageCode(requireContext())
        val adapter = LanguageAdapter(
            LocaleHelper.supportedLanguages,
            currentCode
        ) { selectedLang ->
            val confirmDialog = LanguageConfirmBottomSheetDialog.newInstance(
                languageItem = selectedLang,
                onConfirm = { confirmedLang ->
                    LocaleHelper.setLocale(requireContext(), confirmedLang.code)
                    onLanguageChanged?.invoke(confirmedLang)
                    dismiss()

                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    activity?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                },
                onCancel = {
                    val savedCode = LocaleHelper.getSavedLanguageCode(requireContext())
                    languageAdapter?.setSelectedCode(savedCode)
                }
            )
            confirmDialog.show(parentFragmentManager, LanguageConfirmBottomSheetDialog.TAG)
        }
        languageAdapter = adapter

        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
            setHasFixedSize(true)
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            bottomSheet.setBackgroundResource(android.R.color.transparent)
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true

            // Set height to ~80% of screen height for comfortable scrolling
            val displayMetrics = resources.displayMetrics
            val targetHeight = (displayMetrics.heightPixels * 0.80).toInt()
            bottomSheet.layoutParams.height = targetHeight
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LanguageBottomSheetDialog"

        fun newInstance(onLanguageChanged: ((LocaleHelper.LanguageItem) -> Unit)? = null): LanguageBottomSheetDialog {
            return LanguageBottomSheetDialog().apply {
                this.onLanguageChanged = onLanguageChanged
            }
        }
    }
}
