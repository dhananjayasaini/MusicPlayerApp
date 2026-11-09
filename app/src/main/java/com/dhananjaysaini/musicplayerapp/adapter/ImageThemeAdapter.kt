package com.dhananjaysaini.musicplayerapp.adapter

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dhananjaysaini.musicplayerapp.databinding.ItemImageThemeBinding
import com.dhananjaysaini.musicplayerapp.model.AppTheme
import com.dhananjaysaini.musicplayerapp.model.ThemeType

class ImageThemeAdapter(
    private var themeList: List<AppTheme>,
    private var selectedThemeId: String,
    private val onThemeSelected: (AppTheme) -> Unit,
    private val onCustomSelected: () -> Unit
) : RecyclerView.Adapter<ImageThemeAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemImageThemeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val theme = themeList[position]
        val isSelected = theme.id == selectedThemeId

        if (theme.type == ThemeType.CUSTOM) {
            holder.binding.layoutCustomContent.visibility = View.VISIBLE
            holder.binding.cardThemeContainer.setCardBackgroundColor(Color.parseColor("#1F1545"))

            // If user has an existing custom image, preview it faintly behind custom icon
            if (theme.customImageUri != null) {
                holder.binding.ivThemeWallpaper.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(Uri.parse(theme.customImageUri))
                    .centerCrop()
                    .into(holder.binding.ivThemeWallpaper)
            } else {
                holder.binding.ivThemeWallpaper.visibility = View.GONE
            }

            holder.binding.themeSelectedBorder.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.binding.themeCheckBadge.visibility = if (isSelected) View.VISIBLE else View.GONE

            holder.binding.root.setOnClickListener {
                onCustomSelected()
            }
        } else {
            holder.binding.layoutCustomContent.visibility = View.GONE
            holder.binding.ivThemeWallpaper.visibility = View.VISIBLE
            holder.binding.cardThemeContainer.setCardBackgroundColor(Color.parseColor("#1A122B"))

            holder.binding.ivThemeWallpaper.setImageResource(theme.drawableRes)

            holder.binding.themeSelectedBorder.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.binding.themeCheckBadge.visibility = if (isSelected) View.VISIBLE else View.GONE

            holder.binding.root.setOnClickListener {
                selectedThemeId = theme.id
                notifyDataSetChanged()
                onThemeSelected(theme)
            }
        }
    }

    override fun getItemCount(): Int = themeList.size

    fun updateList(newList: List<AppTheme>, selectedId: String) {
        themeList = newList
        selectedThemeId = selectedId
        notifyDataSetChanged()
    }

    fun setSelectedId(id: String) {
        selectedThemeId = id
        notifyDataSetChanged()
    }
}
