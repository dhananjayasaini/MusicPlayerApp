package com.dhananjaysaini.musicplayerapp.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.DialogCreatePlaylistBinding

object CreatePlaylistDialog {

    fun show(
        context: Context,
        titleRes: Int = R.string.create_playlist,
        initialName: String = "",
        positiveBtnRes: Int = R.string.btn_create,
        existingNames: List<String> = emptyList(),
        onConfirm: (String) -> Unit
    ): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val binding = DialogCreatePlaylistBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.88).toInt()
                .coerceAtMost((400 * context.resources.displayMetrics.density).toInt())
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }

        binding.tvDialogTitle.setText(titleRes)
        binding.btnConfirm.setText(positiveBtnRes)

        binding.tvCharCount.text = "${initialName.length}/40"

        if (initialName.isNotEmpty()) {
            binding.etPlaylistName.setText(initialName)
            binding.etPlaylistName.setSelection(initialName.length)
            binding.btnClearInput.visibility = View.VISIBLE
        }

        binding.etPlaylistName.doAfterTextChanged { text ->
            binding.tvInputError.visibility = View.GONE
            binding.btnClearInput.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.tvCharCount.text = "${text?.length ?: 0}/40"
        }

        binding.btnClearInput.setOnClickListener {
            binding.etPlaylistName.setText("")
        }

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        val validateAndConfirm = {
            val name = binding.etPlaylistName.text.toString().trim()
            if (name.isEmpty()) {
                binding.tvInputError.text = context.getString(R.string.error_enter_playlist_name)
                binding.tvInputError.visibility = View.VISIBLE
            } else if (existingNames.any { it.equals(name, ignoreCase = true) && !it.equals(initialName, ignoreCase = true) }) {
                binding.tvInputError.text = context.getString(R.string.error_playlist_exists)
                binding.tvInputError.visibility = View.VISIBLE
            } else {
                onConfirm(name)
                dialog.dismiss()
            }
        }

        binding.btnConfirm.setOnClickListener {
            validateAndConfirm()
        }

        binding.etPlaylistName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                validateAndConfirm()
                true
            } else {
                false
            }
        }

        dialog.show()

        binding.etPlaylistName.postDelayed({
            binding.etPlaylistName.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etPlaylistName, InputMethodManager.SHOW_IMPLICIT)
        }, 150)

        return dialog
    }
}
