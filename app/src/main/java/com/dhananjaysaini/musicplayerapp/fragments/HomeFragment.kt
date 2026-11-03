package com.dhananjaysaini.musicplayerapp.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.databinding.FragmentHomeBinding
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
            val tab = binding.homeTabLayout.getTabAt(0)
            if (tab != null) {
                styleTab(tab, true)
            }
        }
    }

    private fun styleTab(tab: TabLayout.Tab, isSelected: Boolean) {
        val text = tab.customView?.findViewById<TextView>(R.id.tabText)
        text?.apply {
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
