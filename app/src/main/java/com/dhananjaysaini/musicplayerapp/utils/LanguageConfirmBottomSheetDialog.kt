package com.dhananjaysaini.musicplayerapp.utils

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetLanguageConfirmBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LanguageConfirmBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLanguageConfirmBinding? = null
    private val binding get() = _binding!!

    private var targetLanguage: LocaleHelper.LanguageItem? = null
    private var isConfirmed = false

    var onConfirm: ((LocaleHelper.LanguageItem) -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLanguageConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val langCode = arguments?.getString(ARG_LANG_CODE)
        val langNative = arguments?.getString(ARG_LANG_NATIVE) ?: ""
        val langEnglish = arguments?.getString(ARG_LANG_ENGLISH) ?: ""

        val langItem = if (!langCode.isNullOrEmpty()) {
            LocaleHelper.LanguageItem(langCode, langNative, langEnglish)
        } else {
            targetLanguage
        }

        langItem?.let { item ->
            val displayName = if (item.nativeName.equals(item.englishName, ignoreCase = true)) {
                item.nativeName
            } else {
                "${item.nativeName} (${item.englishName})"
            }
            binding.tvConfirmMessage.text = getString(R.string.change_language_prompt, displayName)
        }

        binding.btnCancelConfirm.setOnClickListener {
            dismiss()
        }

        binding.btnConfirmLanguage.setOnClickListener {
            langItem?.let { item ->
                isConfirmed = true
                dismiss()
                onConfirm?.invoke(item)
            } ?: dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
            bottomSheet.setBackgroundResource(android.R.color.transparent)
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isConfirmed) {
            onCancel?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LanguageConfirmBottomSheetDialog"

        private const val ARG_LANG_CODE = "arg_lang_code"
        private const val ARG_LANG_NATIVE = "arg_lang_native"
        private const val ARG_LANG_ENGLISH = "arg_lang_english"

        fun newInstance(
            languageItem: LocaleHelper.LanguageItem,
            onConfirm: ((LocaleHelper.LanguageItem) -> Unit)? = null,
            onCancel: (() -> Unit)? = null
        ): LanguageConfirmBottomSheetDialog {
            return LanguageConfirmBottomSheetDialog().apply {
                this.targetLanguage = languageItem
                this.onConfirm = onConfirm
                this.onCancel = onCancel
                arguments = Bundle().apply {
                    putString(ARG_LANG_CODE, languageItem.code)
                    putString(ARG_LANG_NATIVE, languageItem.nativeName)
                    putString(ARG_LANG_ENGLISH, languageItem.englishName)
                }
            }
        }
    }
}
