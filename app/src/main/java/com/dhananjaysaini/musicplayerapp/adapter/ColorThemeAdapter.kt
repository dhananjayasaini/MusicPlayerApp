package com.dhananjaysaini.musicplayerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.databinding.ItemColorSwatchBinding
import com.dhananjaysaini.musicplayerapp.model.AppTheme

class ColorThemeAdapter(
    private val themeList: List<AppTheme>,
    private var selectedThemeId: String,
    private val onThemeSelected: (AppTheme) -> Unit
) : RecyclerView.Adapter<ColorThemeAdapter.ColorViewHolder>() {

    inner class ColorViewHolder(val binding: ItemColorSwatchBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorSwatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val theme = themeList[position]
        val isSelected = theme.id == selectedThemeId

        holder.binding.swatchCircle.setImageResource(theme.swatchRes)
        holder.binding.swatchCrown.visibility = if (theme.isPro) View.VISIBLE else View.GONE
        holder.binding.swatchSelectionRing.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.binding.swatchCheckBadge.visibility = if (isSelected) View.VISIBLE else View.GONE

        holder.binding.root.setOnClickListener {
            val previousId = selectedThemeId
            selectedThemeId = theme.id
            notifyDataSetChanged()
            onThemeSelected(theme)
        }
    }

    override fun getItemCount(): Int = themeList.size

    fun setSelectedId(id: String) {
        selectedThemeId = id
        notifyDataSetChanged()
    }
}
