package com.dhananjaysaini.musicplayerapp.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dhananjaysaini.musicplayerapp.databinding.ItemThemePreviewCardBinding
import com.dhananjaysaini.musicplayerapp.model.AppTheme
import com.dhananjaysaini.musicplayerapp.model.ThemeType

class ThemePreviewAdapter(
    private val themeList: List<AppTheme>
) : RecyclerView.Adapter<ThemePreviewAdapter.PreviewViewHolder>() {

    inner class PreviewViewHolder(val binding: ItemThemePreviewCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val binding = ItemThemePreviewCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        val theme = themeList[position]

        // 1. Mockup background
        if (theme.type == ThemeType.CUSTOM && theme.customImageUri != null) {
            Glide.with(holder.itemView.context)
                .load(Uri.parse(theme.customImageUri))
                .centerCrop()
                .into(holder.binding.ivMockupBg)
        } else if (theme.drawableRes != 0) {
            holder.binding.ivMockupBg.setImageResource(theme.drawableRes)
        }

        // 2. VIP Crown Ribbon
        holder.binding.layoutProRibbon.visibility =
            if (theme.isPro) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = themeList.size

    fun getThemeAt(position: Int): AppTheme? {
        return themeList.getOrNull(position)
    }
}
