package com.dhananjaysaini.musicplayerapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dhananjaysaini.musicplayerapp.fragments.FavouriteFragment
import com.dhananjaysaini.musicplayerapp.fragments.HomeFragment
import com.dhananjaysaini.musicplayerapp.fragments.SettingsFragment
import com.dhananjaysaini.musicplayerapp.fragments.VoiceRecorderFragment

class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> FavouriteFragment()
            2 -> VoiceRecorderFragment()
            3 -> SettingsFragment()
            else -> HomeFragment()
        }
    }
}
