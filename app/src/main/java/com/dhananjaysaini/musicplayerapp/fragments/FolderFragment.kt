package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.FolderSongsActivity
import com.dhananjaysaini.musicplayerapp.activities.PlaylistSongsActivity
import com.dhananjaysaini.musicplayerapp.adapter.FolderAdapter
import com.dhananjaysaini.musicplayerapp.databinding.FragmentFolderBinding
import com.dhananjaysaini.musicplayerapp.databinding.FragmentFolderSongsBinding
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.viewmodel.FolderViewModel

class FolderFragment : Fragment(R.layout.fragment_folder) {

    private lateinit var binding: FragmentFolderBinding
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var folderViewModel: FolderViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFolderBinding.bind(view)

        folderViewModel = ViewModelProvider(this)
            .get(FolderViewModel::class.java)

        folderAdapter = FolderAdapter(mutableListOf())

        val recycler = binding.folderRecycler
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = folderAdapter

            folderViewModel.folders.observe(viewLifecycleOwner) {
                folderAdapter.update(it)

                binding.totalFolder.text = LocaleHelper.getFolderCountString(requireContext(), it.size)
            }
            folderViewModel.loadFolders(requireContext())

        openFolderSongs()
    }

    private fun openFolderSongs() {

        folderAdapter.onItemClick = { folder ->
            val intent = Intent(requireContext(), FolderSongsActivity::class.java)
            intent.putExtra("folderPath", folder.path)
            intent.putExtra("folderName", folder.name)
            intent.putExtra("playlistName", folder.name)

            startActivity(intent)
        }
    }

}