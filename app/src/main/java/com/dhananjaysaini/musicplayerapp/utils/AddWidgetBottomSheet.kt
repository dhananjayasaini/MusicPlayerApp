package com.dhananjaysaini.musicplayerapp.utils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.MainActivity
import com.dhananjaysaini.musicplayerapp.databinding.BottomSheetAddWidgetBinding
import com.dhananjaysaini.musicplayerapp.model.WidgetStyleItem
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.widget.MusicAppWidgetProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddWidgetBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddWidgetBinding? = null
    private val binding get() = _binding!!

    private var widgetStyle: WidgetStyleItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddWidgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val style = widgetStyle ?: return

        // Inflate widget preview into sheet container
        binding.sheetPreviewContainer.removeAllViews()
        val previewView = layoutInflater.inflate(style.previewLayoutRes, binding.sheetPreviewContainer, false)
        
        // If music is playing, populate current song info
        try {
            if (MusicService.playlist.isNotEmpty() && MusicService.position in MusicService.playlist.indices) {
                val currentSong = MusicService.playlist[MusicService.position]
                previewView.findViewById<TextView>(R.id.tvSongTitle)?.text = currentSong.title
                previewView.findViewById<TextView>(R.id.tvSongArtist)?.text = currentSong.artist
            }
        } catch (_: Exception) {}

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER
        }
        binding.sheetPreviewContainer.addView(previewView, params)

        binding.btnCancelWidget.setOnClickListener {
            dismiss()
        }

        binding.btnAddWidgetConfirm.setOnClickListener {
            requestAddWidget(style.id)
            dismiss()
        }
    }

    private fun requestAddWidget(styleId: String) {
        val ctx = context ?: return
        try {
            // Save selected widget style preference
            val prefs = ctx.getSharedPreferences("widget_playback_state", Context.MODE_PRIVATE)
            prefs.edit().putString("selected_widget_style", styleId).apply()

            val appWidgetManager = AppWidgetManager.getInstance(ctx)
            val myProvider = ComponentName(ctx, MusicAppWidgetProvider::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                val callbackIntent = Intent(ctx, MainActivity::class.java)
                val successCallback = PendingIntent.getActivity(
                    ctx,
                    199,
                    callbackIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                Toast.makeText(ctx, getString(R.string.widget_pin_requested), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(ctx, getString(R.string.widget_pin_dialog_msg), Toast.LENGTH_LONG).show()
            }

            MusicAppWidgetProvider.updateAllWidgets(ctx)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddWidgetBottomSheet"

        fun newInstance(style: WidgetStyleItem): AddWidgetBottomSheet {
            return AddWidgetBottomSheet().apply {
                this.widgetStyle = style
            }
        }
    }
}
