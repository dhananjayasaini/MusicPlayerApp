package com.dhananjaysaini.musicplayerapp.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.FragmentHomeBinding
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabsAndViewPager()
    }

    private fun setupTabsAndViewPager() {
        binding.homeViewPager.adapter = HomePagerAdapter(this)
        binding.homeViewPager.offscreenPageLimit = 2

        val titles = listOf("Songs", "Playlists", "Folders", "Albums", "Artists")

        TabLayoutMediator(binding.homeTabLayout, binding.homeViewPager) { tab, position ->
            val tabView = layoutInflater.inflate(R.layout.item_viewpager_tab, null)
            val text = tabView.findViewById<TextView>(R.id.tabText)
            text.text = titles[position]
            tab.customView = tabView
        }.attach()

        binding.homeTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                styleTab(tab, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                styleTab(tab, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Style initial selected tab (Songs)
        binding.homeTabLayout.post {
            refreshThemeTabs()
        }

        registerThemeReceiver()
    }

    private val themeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshThemeTabs()
        }
    }

    private fun registerThemeReceiver() {
        val filter = IntentFilter(ThemeManager.ACTION_THEME_CHANGED)
        ContextCompat.registerReceiver(
            requireContext(),
            themeReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onResume() {
        super.onResume()
        refreshThemeTabs()
    }

    private fun refreshThemeTabs() {
        if (_binding == null) return
        val currentContext = context ?: return
        val isDark = ThemeManager.isDarkTheme(currentContext)
        val indicatorColor = if (isDark) Color.WHITE else Color.parseColor("#E65100")
        binding.homeTabLayout.setSelectedTabIndicatorColor(indicatorColor)

        val selectedPos = binding.homeTabLayout.selectedTabPosition
        for (i in 0 until binding.homeTabLayout.tabCount) {
            val tab = binding.homeTabLayout.getTabAt(i) ?: continue
            styleTab(tab, i == selectedPos)
        }
    }

    private fun styleTab(tab: TabLayout.Tab, isSelected: Boolean) {
        val text = tab.customView?.findViewById<TextView>(R.id.tabText)
        val currentContext = context ?: return
        val isDark = ThemeManager.isDarkTheme(currentContext)
        val tabColor = if (isDark) Color.WHITE else Color.parseColor("#111827")

        text?.apply {
            setTextColor(tabColor)
            if (isSelected) {
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                alpha = 1.0f
                animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(150)
                    .start()
            } else {
                textSize = 16f
                setTypeface(null, Typeface.NORMAL)
                alpha = 0.65f
                animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(themeReceiver)
        } catch (_: Exception) {}
        _binding = null
    }

    private inner class HomePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AllSongsFragment()
                1 -> PlaylistFragment()
                2 -> FolderFragment()
                3 -> AlbumFragment()
                4 -> ArtistFragment()
                else -> AllSongsFragment()
            }
        }
    }
}
