package com.dhananjaysaini.musicplayerapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.activities.AlbumSongsActivity
import com.dhananjaysaini.musicplayerapp.activities.ArtistSongsActivity
import com.dhananjaysaini.musicplayerapp.activities.MixDetailsActivity
import com.dhananjaysaini.musicplayerapp.activities.PlayerActivity
import com.dhananjaysaini.musicplayerapp.activities.PlaylistSongsActivity
import com.dhananjaysaini.musicplayerapp.adapter.ForYouAlbumItem
import com.dhananjaysaini.musicplayerapp.adapter.ForYouAlbumsAdapter
import com.dhananjaysaini.musicplayerapp.adapter.ForYouArtistItem
import com.dhananjaysaini.musicplayerapp.adapter.ForYouArtistsAdapter
import com.dhananjaysaini.musicplayerapp.adapter.ForYouExploreAdapter
import com.dhananjaysaini.musicplayerapp.adapter.ForYouExploreMix
import com.dhananjaysaini.musicplayerapp.adapter.ForYouPlaylistItem
import com.dhananjaysaini.musicplayerapp.adapter.ForYouPlaylistsAdapter
import com.dhananjaysaini.musicplayerapp.adapter.ForYouRecentAdapter
import com.dhananjaysaini.musicplayerapp.adapter.ForYouRecentItem
import com.dhananjaysaini.musicplayerapp.adapter.ForYouTopPick
import com.dhananjaysaini.musicplayerapp.adapter.ForYouTopPicksAdapter
import com.dhananjaysaini.musicplayerapp.adapter.RecapSongsCardAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.databinding.FragmentForYouBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.PlayHistoryEntity
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.AlbumCoverManager
import com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper
import com.dhananjaysaini.musicplayerapp.utils.CreatePlaylistDialog
import com.dhananjaysaini.musicplayerapp.utils.PlaylistFolderOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import com.dhananjaysaini.musicplayerapp.viewmodel.FavouriteViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.MainViewModel
import com.dhananjaysaini.musicplayerapp.viewmodel.PlaylistViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ForYouFragment : Fragment(R.layout.fragment_for_you) {

    private var _binding: FragmentForYouBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainVM: MainViewModel
    private lateinit var playlistVM: PlaylistViewModel
    private lateinit var favouriteVM: FavouriteViewModel

    private lateinit var topPicksAdapter: ForYouTopPicksAdapter
    private lateinit var recentAdapter: ForYouRecentAdapter
    private lateinit var exploreAdapter: ForYouExploreAdapter
    private lateinit var playlistsAdapter: ForYouPlaylistsAdapter
    private lateinit var albumsAdapter: ForYouAlbumsAdapter
    private lateinit var artistsAdapter: ForYouArtistsAdapter
    private lateinit var recapSongsAdapter: RecapSongsCardAdapter

    private var allSongs: List<Music> = emptyList()
    private var recentHistory: List<PlayHistoryEntity> = emptyList()
    private var topHistory: List<PlayHistoryEntity> = emptyList()
    private var recapAllTopSongs: List<Music> = emptyList()
    private var recapSongList: List<Music> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentForYouBinding.bind(view)

        mainVM = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        playlistVM = ViewModelProvider(requireActivity())[PlaylistViewModel::class.java]
        favouriteVM = ViewModelProvider(requireActivity())[FavouriteViewModel::class.java]

        setupRecyclerViews()
        setupClickListeners()
        observeData()
    }

    private fun setupRecyclerViews() {
        topPicksAdapter = ForYouTopPicksAdapter(emptyList()) { songs ->
            playSong(ArrayList(songs), 0)
        }
        binding.recyclerTopPicks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerTopPicks.adapter = topPicksAdapter

        recentAdapter = ForYouRecentAdapter(emptyList()) { songs ->
            playSong(ArrayList(songs), 0)
        }
        binding.recyclerRecentlyPlayed.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerRecentlyPlayed.adapter = recentAdapter

        exploreAdapter = ForYouExploreAdapter(emptyList()) { songs ->
            playSong(ArrayList(songs), 0)
        }
        binding.recyclerExplore.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerExplore.adapter = exploreAdapter

        playlistsAdapter = ForYouPlaylistsAdapter(emptyList()) { item ->
            if (item.songs.isNotEmpty()) {
                playSong(ArrayList(item.songs), 0)
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    val pl = playlistVM.playlists.value?.firstOrNull { it.name == item.title }
                    if (pl != null) {
                        val songIds = playlistVM.getSongIdsList(pl.songId)
                        val pSongs = allSongs.filter { it.id in songIds }
                        if (pSongs.isNotEmpty()) {
                            playSong(ArrayList(pSongs), 0)
                        }
                    }
                }
            }
        }
        binding.recyclerPlaylistsYouLove.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerPlaylistsYouLove.adapter = playlistsAdapter

        albumsAdapter = ForYouAlbumsAdapter(emptyList()) { songs ->
            playSong(ArrayList(songs), 0)
        }
        binding.recyclerAlbumsYouLove.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerAlbumsYouLove.adapter = albumsAdapter

        artistsAdapter = ForYouArtistsAdapter(emptyList())
        binding.recyclerArtistsYouLove.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerArtistsYouLove.adapter = artistsAdapter

        recapSongsAdapter = RecapSongsCardAdapter(emptyList()) { position ->
            val song = recapSongList.getOrNull(position)
            if (song != null) {
                val all = if (recapAllTopSongs.isNotEmpty()) recapAllTopSongs else allSongs
                val realIndex = all.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                playSong(ArrayList(all), realIndex)
            }
        }
        binding.recyclerRecapSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecapSongs.adapter = recapSongsAdapter
    }

    private fun setupClickListeners() {
        // Top Shuffle & Play
        binding.btnShuffle.setOnClickListener {
            if (allSongs.isNotEmpty()) {
                val shuffled = ArrayList(allSongs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnPlay.setOnClickListener {
            if (allSongs.isNotEmpty()) {
                playSong(ArrayList(allSongs), 0)
            }
        }

        // Section Headers
        binding.layoutRecentlyPlayedHeader.setOnClickListener {
            openPlaylist(getString(R.string.recently_played), "recently_played")
        }

        binding.layoutRecentlyAddedHeader.setOnClickListener {
            openPlaylist(getString(R.string.recently_added), "recently_added")
        }

        binding.btnRecentlyAddedPlay.setOnClickListener {
            val recentAdded = getRecentAddedSongs()
            if (recentAdded.isNotEmpty()) {
                playSong(ArrayList(recentAdded), 0)
            }
        }

        binding.layoutPlaylistsYouLoveHeader.setOnClickListener {
            switchTab(2) // Playlists tab
        }

        binding.layoutAlbumsYouLoveHeader.setOnClickListener {
            switchTab(4) // Albums tab
        }

        binding.layoutArtistsYouLoveHeader.setOnClickListener {
            switchTab(5) // Artists tab
        }

        binding.layoutMusicRecapHeader.setOnClickListener {
            openMix("music_recap")
        }

        binding.cardMusicRecap.setOnClickListener {
            openMix("music_recap")
        }
    }

    private fun observeData() {
        mainVM.musicListLiveData.observe(viewLifecycleOwner) { songs ->
            allSongs = songs
            refreshAllSections()
        }

        val db = MusicDatabase.getDatabase(requireContext())
        db.playHistoryDao().getRecentlyPlayed().observe(viewLifecycleOwner) { history ->
            recentHistory = history ?: emptyList()
            refreshAllSections()
        }

        db.playHistoryDao().getTopPlayed().observe(viewLifecycleOwner) { history ->
            topHistory = history ?: emptyList()
            refreshAllSections()
        }

        favouriteVM.favouriteIds.observe(viewLifecycleOwner) {
            refreshAllSections()
        }

        playlistVM.playlists.observe(viewLifecycleOwner) {
            refreshAllSections()
        }
    }

    private fun refreshAllSections() {
        if (_binding == null || allSongs.isEmpty()) return

        val songMap = allSongs.associateBy { it.id }
        val recentPlayedSongs = recentHistory.mapNotNull { songMap[it.songId] }

        // Top played songs: ordered by play count (descending), then recent plays, then library
        val topPlayedList = topHistory.mapNotNull { songMap[it.songId] }
        val playedTopIds = topPlayedList.map { it.id }.toSet()
        val recentFill = recentPlayedSongs.filter { it.id !in playedTopIds }
        val remainingFill = allSongs.filter { it.id !in playedTopIds && it.id !in recentFill.map { r -> r.id } }
        val topPlayedSongs = (topPlayedList + recentFill + remainingFill).take(20)
        val recentAddedSongs = getRecentAddedSongs()

        // 1. Top Picks
        val topPicks = mutableListOf<ForYouTopPick>()
        // Card 1: Your Hits / Most Played -> Show folder icon (ic_neon_fire on bg_preset_top)
        topPicks.add(
            ForYouTopPick(
                tag = getString(R.string.for_you_most_played),
                title = getString(R.string.for_you_your_hits),
                artUri = null,
                iconRes = R.drawable.ic_neon_fire,
                bgRes = R.drawable.bg_preset_top,
                songs = topPlayedSongs,
                onCardClick = { openPlaylist(getString(R.string.my_top_tracks), "top_tracks") }
            )
        )
        // Card 2: Top Album -> Show folder icon (ic_neon_album on bg_playlist_card) or customCover if set
        val topAlbumName = topPlayedSongs.firstNotNullOfOrNull { it.album?.takeIf { a -> a.isNotBlank() } }
            ?: allSongs.firstNotNullOfOrNull { it.album?.takeIf { a -> a.isNotBlank() } }
        if (topAlbumName != null) {
            val albumSongs = allSongs.filter { it.album == topAlbumName }
            val customCover = AlbumCoverManager.getCustomCover(requireContext(), topAlbumName)
            topPicks.add(
                ForYouTopPick(
                    tag = getString(R.string.for_you_top_album),
                    title = topAlbumName,
                    artUri = customCover,
                    iconRes = if (customCover == null) R.drawable.ic_neon_album else null,
                    bgRes = R.drawable.bg_playlist_card,
                    songs = albumSongs,
                    onCardClick = { openAlbum(topAlbumName) }
                )
            )
        }
        // Card 3: Top Artist -> Show folder icon (ic_neon_artist on bg_playlist_card)
        val topArtistName = topPlayedSongs.firstNotNullOfOrNull { it.artist?.takeIf { a -> a.isNotBlank() } }
            ?: allSongs.firstNotNullOfOrNull { it.artist?.takeIf { a -> a.isNotBlank() } }
        if (topArtistName != null) {
            val artistSongs = allSongs.filter { it.artist == topArtistName }
            topPicks.add(
                ForYouTopPick(
                    tag = getString(R.string.for_you_top_artist),
                    title = topArtistName,
                    artUri = null,
                    iconRes = R.drawable.ic_neon_artist,
                    bgRes = R.drawable.bg_playlist_card,
                    songs = artistSongs,
                    onCardClick = { openArtist(topArtistName) }
                )
            )
        }
        topPicksAdapter.updateList(topPicks)

        // 2. Recently Played Horizontal
        val recentItems = mutableListOf<ForYouRecentItem>()
        // Item 1: Recently added playlist card
        recentItems.add(
            ForYouRecentItem(
                title = getString(R.string.recently_added),
                subtitle = getString(R.string.tab_playlists),
                artUri = null,
                iconRes = R.drawable.ic_neon_recent_add,
                bgRes = R.drawable.bg_preset_recent_add,
                songs = recentAddedSongs,
                onCardClick = { openPlaylist(getString(R.string.recently_added), "recently_added") }
            )
        )
        // Item 2: Top recent album
        val recentAlbum = recentPlayedSongs.firstNotNullOfOrNull { it.album?.takeIf { a -> a.isNotBlank() } }
        if (recentAlbum != null) {
            val rAlbumSongs = allSongs.filter { it.album == recentAlbum }
            val customCover = AlbumCoverManager.getCustomCover(requireContext(), recentAlbum)
            val rAlbumArt = customCover ?: rAlbumSongs.firstOrNull()?.artUri
            recentItems.add(
                ForYouRecentItem(
                    title = recentAlbum,
                    subtitle = getString(R.string.tab_albums),
                    artUri = rAlbumArt,
                    iconRes = R.drawable.ic_neon_album,
                    bgRes = R.drawable.bg_playlist_card,
                    songs = rAlbumSongs,
                    onCardClick = { openAlbum(recentAlbum) }
                )
            )
        }
        // Item 3+: Recent songs
        for (song in recentPlayedSongs.take(8)) {
            recentItems.add(
                ForYouRecentItem(
                    title = song.title,
                    subtitle = song.artist ?: "<unknown>",
                    artUri = song.artUri,
                    songs = listOf(song),
                    onCardClick = {
                        val index = recentPlayedSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                        playSong(ArrayList(recentPlayedSongs), index)
                    }
                )
            )
        }
        recentAdapter.updateList(recentItems)

        // 3. Explore Mixes
        val exploreMixes = mutableListOf<ForYouExploreMix>()

        // 1) Daily Discover
        val discoverSongs = allSongs.shuffled().take(25)
        val discoverSub = discoverSongs.take(2).joinToString("\n") { it.title }
        exploreMixes.add(
            ForYouExploreMix(
                title = getString(R.string.for_you_daily_discover),
                subtitle = discoverSub.ifEmpty { "Daily curated tracks" },
                bgDrawableRes = R.drawable.bg_mix_daily,
                songs = discoverSongs,
                onCardClick = { openMix("daily_discover") }
            )
        )

        // 2) Top Artist Mix
        val topArtistSongs = if (topArtistName != null) {
            allSongs.filter { it.artist == topArtistName }
        } else {
            allSongs.take(15)
        }
        val topArtistSub = topArtistSongs.take(2).joinToString("\n") { "${it.artist} - ${it.title}" }
        exploreMixes.add(
            ForYouExploreMix(
                title = getString(R.string.for_you_top_artist_mix),
                subtitle = topArtistSub.ifEmpty { "Your favorite artists" },
                bgDrawableRes = R.drawable.bg_mix_artist,
                songs = topArtistSongs,
                onCardClick = { openMix("top_artist_mix") }
            )
        )

        // 3) Never Played (songs not in play history)
        val playedIds = (recentHistory.map { it.songId } + topHistory.map { it.songId }).toSet()
        val neverPlayedSongs = allSongs.filter { it.id !in playedIds }.ifEmpty { allSongs.shuffled().take(20) }
        val neverSub = neverPlayedSongs.take(2).joinToString("\n") { it.title }
        exploreMixes.add(
            ForYouExploreMix(
                title = getString(R.string.for_you_never_played),
                subtitle = neverSub.ifEmpty { "Tracks you haven't played yet" },
                bgDrawableRes = R.drawable.bg_mix_never,
                songs = neverPlayedSongs,
                onCardClick = { openMix("never_played") }
            )
        )

        // 4) High Quality (lossless or high bitrate audio)
        val hqSongs = allSongs.filter { isSongHighQuality(it) }.ifEmpty { allSongs.sortedByDescending { it.duration }.take(25) }
        val hqSub = hqSongs.take(2).joinToString("\n") { it.title }
        exploreMixes.add(
            ForYouExploreMix(
                title = getString(R.string.for_you_high_quality),
                subtitle = hqSub.ifEmpty { "Lossless & high bitrate audio" },
                bgDrawableRes = R.drawable.bg_mix_hq,
                songs = hqSongs,
                onCardClick = { openMix("high_quality") }
            )
        )

        // 5) New Music Mix
        val newMixSongs = recentAddedSongs.take(20)
        val newMixSub = newMixSongs.take(2).joinToString("\n") { it.title }
        exploreMixes.add(
            ForYouExploreMix(
                title = getString(R.string.for_you_new_music_mix),
                subtitle = newMixSub.ifEmpty { "Freshly added music" },
                bgDrawableRes = R.drawable.bg_mix_new,
                songs = newMixSongs,
                onCardClick = { openMix("daily_discover") }
            )
        )
        exploreAdapter.updateList(exploreMixes)

        // 4. Recently Added Vertical (Top 3 songs)
        populateRecentlyAddedSongs(recentAddedSongs)

        // 5. Playlists You Love
        val favIds = favouriteVM.favouriteIds.value ?: emptyList()
        val favSongs = allSongs.filter { it.id in favIds }
        val playlistItems = mutableListOf<ForYouPlaylistItem>()
        // My favourite
        playlistItems.add(
            ForYouPlaylistItem(
                title = getString(R.string.my_favourite),
                subtitle = favSongs.firstOrNull()?.title ?: "${favSongs.size} songs",
                artUri = null,
                isFav = true,
                songs = favSongs,
                onCardClick = { openPlaylist(getString(R.string.my_favourite), "favourite") },
                onMenuClick = {
                    val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                        title = getString(R.string.my_favourite),
                        iconRes = R.drawable.ic_neon_favourite,
                        bgDrawableRes = R.drawable.bg_preset_fav,
                        songs = favSongs,
                        isCustomPlaylist = false
                    )
                    sheet.show(parentFragmentManager, "for_you_fav_menu")
                }
            )
        )
        // Custom playlists
        val customPlaylists = playlistVM.playlists.value ?: emptyList()
        for (pl in customPlaylists) {
            playlistItems.add(
                ForYouPlaylistItem(
                    title = pl.name,
                    subtitle = getString(R.string.tab_playlists),
                    artUri = null,
                    isFav = false,
                    songs = emptyList(),
                    onCardClick = {
                        val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
                            putExtra("playlistId", pl.songId)
                            putExtra("playlistName", pl.name)
                        }
                        startActivity(intent)
                    },
                    onMenuClick = {
                        viewLifecycleOwner.lifecycleScope.launch {
                            val songIds = playlistVM.getSongIdsList(pl.songId)
                            val pSongs = allSongs.filter { it.id in songIds }
                            val sheet = PlaylistFolderOptionsBottomSheet.newInstance(
                                title = pl.name,
                                iconRes = R.drawable.ic_neon_playlist_card,
                                bgDrawableRes = R.drawable.bg_dialog_playlist_icon,
                                songs = pSongs,
                                isCustomPlaylist = true
                            )
                            sheet.onRenameClick = {
                                val existingNames = playlistVM.playlists.value?.map { it.name } ?: emptyList()
                                CreatePlaylistDialog.show(
                                    context = requireContext(),
                                    titleRes = R.string.rename_playlist,
                                    initialName = pl.name,
                                    positiveBtnRes = R.string.btn_rename,
                                    existingNames = existingNames
                                ) { newName ->
                                    if (newName != pl.name) {
                                        playlistVM.renamePlaylist(pl.songId, newName)
                                    }
                                }
                            }
                            sheet.onDeleteClick = {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.delete_playlist_title)
                                    .setMessage(getString(R.string.delete_playlist_prompt, pl.name))
                                    .setPositiveButton(R.string.btn_delete) { _, _ ->
                                        playlistVM.deletePlaylist(pl.songId)
                                    }
                                    .setNegativeButton(R.string.btn_cancel, null)
                                    .show()
                            }
                            sheet.show(parentFragmentManager, "for_you_pl_menu_${pl.songId}")
                        }
                    }
                )
            )
        }
        playlistsAdapter.updateList(playlistItems)

        // 6. Albums You Love
        val unknownAlbum = getString(R.string.unknown_album)
        val albums = allSongs.groupBy { it.album ?: unknownAlbum }
            .map { (name, list) ->
                val customCover = AlbumCoverManager.getCustomCover(requireContext(), name)
                ForYouAlbumItem(
                    name = name,
                    artist = list.firstOrNull()?.artist ?: "<unknown>",
                    artUri = customCover ?: list.firstOrNull()?.artUri,
                    songs = list,
                    onCardClick = { openAlbum(name) }
                )
            }.take(15)
        albumsAdapter.updateList(albums)

        // 7. Artists You Love
        val unknownArtist = getString(R.string.unknown_artist)
        val artists = allSongs.groupBy { it.artist ?: unknownArtist }
            .map { (name, list) ->
                ForYouArtistItem(
                    name = name,
                    artUri = list.firstOrNull()?.artUri,
                    songs = list,
                    onCardClick = { openArtist(name) }
                )
            }.take(15)
        artistsAdapter.updateList(artists)

        // 8. Music Recap Card
        populateMusicRecap(topPlayedSongs)
    }

    private fun populateRecentlyAddedSongs(recentAdded: List<Music>) {
        val container = binding.layoutRecentlyAddedSongs
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        val displaySongs = recentAdded.take(3)
        for (song in displaySongs) {
            val row = inflater.inflate(R.layout.item_for_you_recent_song, container, false)
            val ivArt = row.findViewById<ImageView>(R.id.ivArt)
            val tvTitle = row.findViewById<TextView>(R.id.tvTitle)
            val tvSubtitle = row.findViewById<TextView>(R.id.tvSubtitle)
            val btnPlayArrow = row.findViewById<ImageView>(R.id.btnPlayArrow)

            tvTitle.text = song.title
            tvSubtitle.text = song.artist ?: song.album ?: "<unknown>"

            Glide.with(this)
                .load(song.artUri)
                .apply(RequestOptions().placeholder(R.drawable.ic_neon_music_note).centerCrop())
                .into(ivArt)

            val playAction = {
                val index = recentAdded.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                playSong(ArrayList(recentAdded), index)
            }

            row.setOnClickListener { playAction() }
            btnPlayArrow.setOnClickListener { playAction() }

            container.addView(row)
        }
    }

    private fun populateMusicRecap(topSongs: List<Music>) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val monthName = SimpleDateFormat("MMMM", Locale.ENGLISH).format(Date()).uppercase() + " ✨"

        binding.tvRecapYear.text = year
        binding.tvRecapMonth.text = monthName

        binding.btnRecapPlay.setOnClickListener {
            if (topSongs.isNotEmpty()) {
                playSongDirectly(ArrayList(topSongs), 0)
            }
        }

        recapAllTopSongs = topSongs
        recapSongList = topSongs.take(3)
        recapSongsAdapter.updateList(recapSongList)
    }

    private fun getRecentAddedSongs(): List<Music> {
        return allSongs.sortedByDescending { it.date }
    }

    private fun playSong(list: ArrayList<Music>, position: Int) {
        if (list.isEmpty()) return
        val mainActivity = activity as? com.dhananjaysaini.musicplayerapp.activities.MainActivity
        if (mainActivity != null) {
            mainActivity.playAudioTrack(list, position, openPlayer = true)
            return
        }
        val act = activity ?: return
        if (!com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper.isPermissionGranted(act)) {
            com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper.checkAndRequestOnAudioClick(act) {
                executeDirectPlay(list, position, openPlayer = true)
            }
            return
        }
        executeDirectPlay(list, position, openPlayer = true)
    }

    private fun playSongDirectly(list: ArrayList<Music>, position: Int) {
        if (list.isEmpty()) return
        val mainActivity = activity as? com.dhananjaysaini.musicplayerapp.activities.MainActivity
        if (mainActivity != null) {
            mainActivity.playAudioTrack(list, position, openPlayer = false)
            return
        }
        val act = activity ?: return
        if (!com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper.isPermissionGranted(act)) {
            com.dhananjaysaini.musicplayerapp.utils.BackgroundPlaybackPermissionHelper.checkAndRequestOnAudioClick(act) {
                executeDirectPlay(list, position, openPlayer = false)
            }
            return
        }
        executeDirectPlay(list, position, openPlayer = false)
    }

    private fun executeDirectPlay(list: ArrayList<Music>, position: Int, openPlayer: Boolean) {
        MusicService.playlist = ArrayList(list)
        MusicService.position = position

        requireContext().startService(
            Intent(requireContext(), MusicService::class.java).apply {
                action = Constants.ACTION_PLAY_AT
                putExtra("songPosition", position)
            }
        )

        requireContext().sendBroadcast(
            Intent("SHOW_MINI_PLAYER").setPackage(requireContext().packageName)
        )
        if (openPlayer) {
            startActivity(Intent(requireContext(), PlayerActivity::class.java))
        }
    }

    private fun openPlaylist(name: String, type: String) {
        val intent = Intent(requireContext(), PlaylistSongsActivity::class.java).apply {
            putExtra("playlistName", name)
            putExtra("playlistType", type)
        }
        startActivity(intent)
    }

    private fun openAlbum(name: String) {
        val intent = Intent(requireContext(), AlbumSongsActivity::class.java).apply {
            putExtra("albumName", name)
        }
        startActivity(intent)
    }

    private fun openArtist(name: String) {
        val intent = Intent(requireContext(), ArtistSongsActivity::class.java).apply {
            putExtra("artistName", name)
        }
        startActivity(intent)
    }

    private fun openMix(mixType: String) {
        val intent = Intent(requireContext(), MixDetailsActivity::class.java).apply {
            putExtra("mixType", mixType)
        }
        startActivity(intent)
    }

    private fun isSongHighQuality(song: Music): Boolean {
        val lower = song.path.lowercase()
        if (lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".alac") || lower.endsWith(".ape")) {
            return true
        }
        try {
            if (song.duration > 0) {
                val file = java.io.File(song.path)
                if (file.exists()) {
                    val kbps = (file.length() * 8) / song.duration
                    if (kbps >= 256) return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private fun switchTab(tabIndex: Int) {
        val pager = requireActivity().findViewById<ViewPager2>(R.id.homeViewPager)
        pager?.currentItem = tabIndex
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(requireActivity())
        refreshAllSections()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
