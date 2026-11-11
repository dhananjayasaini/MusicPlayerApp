package com.dhananjaysaini.musicplayerapp.adapter

import android.content.res.ColorStateList
import android.graphics.Color
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
        val isDark = theme.isDark

        // 1. Mockup background
        holder.binding.cardPhoneMockup.setCardBackgroundColor(Color.TRANSPARENT)
        holder.binding.cardPhoneMockup.clipToOutline = true
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

        // 3. Dynamic styling based on theme darkness (Dark vs White circle theme)
        val primaryTextColor = if (isDark) Color.WHITE else Color.parseColor("#111827")
        val secondaryTextColor = if (isDark) Color.parseColor("#99FFFFFF") else Color.parseColor("#4B5563")
        val subheaderTextColor = if (isDark) Color.parseColor("#CCFFFFFF") else Color.parseColor("#374151")
        val iconTint = ColorStateList.valueOf(if (isDark) Color.WHITE else Color.parseColor("#111827"))
        val dotsTint = ColorStateList.valueOf(if (isDark) Color.parseColor("#80FFFFFF") else Color.parseColor("#6B7280"))
        val badgeTextColor = if (isDark) Color.parseColor("#80FFFFFF") else Color.parseColor("#4B5563")
        val badgeBorderTint = ColorStateList.valueOf(if (isDark) Color.parseColor("#4DFFFFFF") else Color.parseColor("#4B5563"))

        // Top Toolbar
        holder.binding.mockMenuBtn.imageTintList = iconTint
        holder.binding.mockSearchBtn.imageTintList = iconTint
        holder.binding.mockSettingsBtn.imageTintList = iconTint

        // Tabs
        val activeTabColor = if (isDark) Color.WHITE else Color.parseColor("#111827")
        val inactiveTabColor = if (isDark) Color.parseColor("#99FFFFFF") else Color.parseColor("#6B7280")
        holder.binding.tabForYou.setTextColor(inactiveTabColor)
        holder.binding.tabSongs.setTextColor(activeTabColor)
        holder.binding.tabIndicator.setBackgroundColor(activeTabColor)
        holder.binding.tabPlaylist.setTextColor(inactiveTabColor)
        holder.binding.tabFolders.setTextColor(inactiveTabColor)
        holder.binding.tabAlbums.setTextColor(inactiveTabColor)

        // Subheader (10 songs + Sort & View icons)
        holder.binding.tvMockSongCount.setTextColor(subheaderTextColor)
        holder.binding.mockSortBtn.imageTintList = iconTint
        holder.binding.mockViewBtn.imageTintList = iconTint

        // 10 Songs List
        val titles = listOf(
            holder.binding.title1, holder.binding.title2, holder.binding.title3,
            holder.binding.title4, holder.binding.title5, holder.binding.title6,
            holder.binding.title7, holder.binding.title8, holder.binding.title9,
            holder.binding.title10
        )
        val subtitles = listOf(
            holder.binding.sub1, holder.binding.sub2, holder.binding.sub3,
            holder.binding.sub4, holder.binding.sub5, holder.binding.sub6,
            holder.binding.sub7, holder.binding.sub8, holder.binding.sub9,
            holder.binding.sub10
        )
        val dots = listOf(
            holder.binding.dots1, holder.binding.dots2, holder.binding.dots3,
            holder.binding.dots4, holder.binding.dots5, holder.binding.dots6,
            holder.binding.dots7, holder.binding.dots8, holder.binding.dots9,
            holder.binding.dots10
        )
        val badges = listOf(
            holder.binding.hdBadge1, holder.binding.hdBadge2, holder.binding.hdBadge3,
            holder.binding.hdBadge4, holder.binding.hdBadge5, holder.binding.hdBadge6,
            holder.binding.hdBadge7, holder.binding.hdBadge8, holder.binding.hdBadge9,
            holder.binding.hdBadge10
        )

        val activeSongTitleColor = if (isDark) Color.parseColor("#FFB800") else Color.parseColor("#D97706")
        val activeSongSubColor = if (isDark) Color.parseColor("#CCFFFFFF") else Color.parseColor("#4B5563")

        titles.forEachIndexed { index, tv ->
            tv.setTextColor(if (index == 2) activeSongTitleColor else primaryTextColor)
        }
        subtitles.forEachIndexed { index, tv ->
            tv.setTextColor(if (index == 2) activeSongSubColor else secondaryTextColor)
        }
        dots.forEach { it.imageTintList = dotsTint }
        badges.forEach {
            it.setTextColor(badgeTextColor)
            it.backgroundTintList = badgeBorderTint
        }

        // Mini Player
        val miniBg = if (isDark) Color.parseColor("#D9181128") else Color.parseColor("#E6FFFFFF")
        val miniStroke = if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#E5E7EB")
        val miniTitleColor = if (isDark) Color.WHITE else Color.parseColor("#111827")
        val miniSubColor = if (isDark) Color.parseColor("#B3FFFFFF") else Color.parseColor("#4B5563")
        val miniControlTint = ColorStateList.valueOf(if (isDark) Color.WHITE else Color.parseColor("#111827"))

        holder.binding.mockMiniPlayer.setCardBackgroundColor(miniBg)
        holder.binding.mockMiniPlayer.strokeColor = miniStroke
        holder.binding.tvMockMiniTitle.setTextColor(miniTitleColor)
        holder.binding.tvMockMiniSub.setTextColor(miniSubColor)
        holder.binding.mockPauseBtn.imageTintList = miniControlTint
        holder.binding.mockQueueBtn.imageTintList = miniControlTint
    }

    override fun getItemCount(): Int = themeList.size

    fun getThemeAt(position: Int): AppTheme? {
        return themeList.getOrNull(position)
    }
}
