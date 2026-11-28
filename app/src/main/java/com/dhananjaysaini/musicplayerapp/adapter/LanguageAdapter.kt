package com.dhananjaysaini.musicplayerapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.databinding.ItemLanguageOptionBinding
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper

class LanguageAdapter(
    private val languages: List<LocaleHelper.LanguageItem>,
    private var selectedCode: String,
    private val onLanguageSelected: (LocaleHelper.LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(val binding: ItemLanguageOptionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageOptionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val item = languages[position]
        val isSelected = item.code.equals(selectedCode, ignoreCase = true)

        holder.binding.tvNativeName.text = item.nativeName
        
        // Show English name as subtitle (like in device settings)
        if (item.nativeName.equals(item.englishName, ignoreCase = true)) {
            holder.binding.tvEnglishName.visibility = View.GONE
        } else {
            holder.binding.tvEnglishName.visibility = View.VISIBLE
            holder.binding.tvEnglishName.text = item.englishName
        }

        holder.binding.rbLanguage.isChecked = isSelected

        holder.itemView.setOnClickListener {
            if (!selectedCode.equals(item.code, ignoreCase = true)) {
                setSelectedCode(item.code)
                onLanguageSelected(item)
            }
        }
    }

    override fun getItemCount(): Int = languages.size

    fun getSelectedCode(): String = selectedCode

    fun setSelectedCode(newCode: String) {
        if (!selectedCode.equals(newCode, ignoreCase = true)) {
            val oldIndex = languages.indexOfFirst { it.code.equals(selectedCode, ignoreCase = true) }
            val newIndex = languages.indexOfFirst { it.code.equals(newCode, ignoreCase = true) }
            selectedCode = newCode
            if (oldIndex != -1) notifyItemChanged(oldIndex)
            if (newIndex != -1) notifyItemChanged(newIndex)
        }
    }
}
