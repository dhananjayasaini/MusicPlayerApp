package com.dhananjaysaini.musicplayerapp.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dhananjaysaini.musicplayerapp.R
import com.dhananjaysaini.musicplayerapp.adapter.MixSongsAdapter
import com.dhananjaysaini.musicplayerapp.constants.Constants
import com.dhananjaysaini.musicplayerapp.database.MusicDatabase
import com.dhananjaysaini.musicplayerapp.databinding.ActivityMixDetailsBinding
import com.dhananjaysaini.musicplayerapp.model.Music
import com.dhananjaysaini.musicplayerapp.model.PlaylistEntity
import com.dhananjaysaini.musicplayerapp.model.PlaylistSongEntity
import com.dhananjaysaini.musicplayerapp.model.SongMenuConfig
import com.dhananjaysaini.musicplayerapp.service.MusicService
import com.dhananjaysaini.musicplayerapp.utils.CreatePlaylistDialog
import com.dhananjaysaini.musicplayerapp.utils.FavouriteManager
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper
import com.dhananjaysaini.musicplayerapp.utils.SongOptionsBottomSheet
import com.dhananjaysaini.musicplayerapp.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MixDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMixDetailsBinding
    private lateinit var adapter: MixSongsAdapter
    private val mixSongs = ArrayList<Music>()
    private var mixType: String = "daily_discover"
    private var currentMixTitle: String = ""
    private var isRecapMode = false
    private val songPlayCounts = HashMap<String, Int>()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMixDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rootView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        val isDark = ThemeManager.isDarkTheme(this)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark

        mixType = intent.getStringExtra("mixType") ?: "daily_discover"

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveAsPlaylist()
        }

        binding.btnMixShuffle.setOnClickListener {
            if (mixSongs.isNotEmpty()) {
                val shuffled = ArrayList(mixSongs)
                shuffled.shuffle()
                playSong(shuffled, 0)
            }
        }

        binding.btnMixPlay.setOnClickListener {
            if (mixSongs.isNotEmpty()) {
                playSong(ArrayList(mixSongs), 0)
            }
        }

        loadMixData()
    }

    private fun loadMixData() {
        val allSongs = MusicService.allSongs
        val db = MusicDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val topHistory = db.playHistoryDao().getTopPlayedSync()
            val recentHistory = db.playHistoryDao().getRecentlyPlayedSync()

            val playedIds = (topHistory.map { it.songId } + recentHistory.map { it.songId }).toSet()
            for (h in topHistory) {
                songPlayCounts[h.songId] = h.playCount
            }

            val songsResult = ArrayList<Music>()

            when (mixType) {
                "daily_discover" -> {
                    currentMixTitle = getString(R.string.for_you_daily_discover)
                    val pool = ArrayList(allSongs)
                    pool.shuffle()
                    songsResult.addAll(pool.take(25))
                }
                "top_artist_mix" -> {
                    currentMixTitle = getString(R.string.for_you_top_artist_mix)
                    val songMap = allSongs.associateBy { it.id }
                    val topSongs = topHistory.mapNotNull { songMap[it.songId] }
                    val topArtist = topSongs.firstNotNullOfOrNull { it.artist?.takeIf { a -> a.isNotBlank() } }
                        ?: allSongs.firstNotNullOfOrNull { it.artist?.takeIf { a -> a.isNotBlank() } }

                    if (topArtist != null) {
                        songsResult.addAll(allSongs.filter { it.artist == topArtist })
                    }
                    if (songsResult.size < 7) {
                        val remaining = allSongs.filter { !songsResult.contains(it) }.shuffled()
                        songsResult.addAll(remaining.take(15 - songsResult.size))
                    }
                }
                "never_played" -> {
                    currentMixTitle = getString(R.string.for_you_never_played)
                    val unplayed = allSongs.filter { it.id !in playedIds }
                    if (unplayed.isNotEmpty()) {
                        songsResult.addAll(unplayed)
                    } else {
                        songsResult.addAll(allSongs.shuffled().take(20))
                    }
                }
                "high_quality" -> {
                    currentMixTitle = getString(R.string.for_you_high_quality)
                    val hq = allSongs.filter { isSongHighQuality(it) }
                    if (hq.isNotEmpty()) {
                        songsResult.addAll(hq)
                    } else {
                        songsResult.addAll(allSongs.sortedByDescending { it.duration }.take(25))
                    }
                }
                "music_recap" -> {
                    currentMixTitle = getString(R.string.for_you_music_recap)
                    isRecapMode = true
                    val songMap = allSongs.associateBy { it.id }
                    val topSongs = topHistory.mapNotNull { songMap[it.songId] }
                    val playedTopIds = topSongs.map { it.id }.toSet()
                    val recentFill = recentHistory.mapNotNull { songMap[it.songId] }.filter { it.id !in playedTopIds }
                    val remainingFill = allSongs.filter { it.id !in playedTopIds && it.id !in recentFill.map { r -> r.id } }
                    val combinedRecap = (topSongs + recentFill + remainingFill).take(20)
                    songsResult.addAll(combinedRecap)
                }
                else -> {
                    currentMixTitle = intent.getStringExtra("mixTitle") ?: "Mix"
                    songsResult.addAll(allSongs.take(20))
                }
            }

            withContext(Dispatchers.Main) {
                mixSongs.clear()
                mixSongs.addAll(songsResult)
                bindMixHeader()
                setupRecycler()
            }
        }
    }

    private fun bindMixHeader() {
        binding.tvMixTitle.text = currentMixTitle

        when (mixType) {
            "daily_discover" -> {
                binding.cardPlate1.setBackgroundResource(R.drawable.bg_mix_daily)
                binding.cardPlate2.setBackgroundResource(R.drawable.bg_mix_daily)
                binding.bgCardMain.setBackgroundResource(R.drawable.bg_mix_daily)
                binding.tvCardCenterText.text = "Daily\nDiscover"
                binding.tvCardCenterText.visibility = View.VISIBLE
                binding.tvCardRecapText.visibility = View.GONE
                binding.ivCardRecapHeadset.visibility = View.GONE
                binding.tvMixSubtitle.text = getString(R.string.mix_sub_daily_discover, mixSongs.size)
            }
            "top_artist_mix" -> {
                binding.cardPlate1.setBackgroundResource(R.drawable.bg_mix_artist)
                binding.cardPlate2.setBackgroundResource(R.drawable.bg_mix_artist)
                binding.bgCardMain.setBackgroundResource(R.drawable.bg_mix_artist)
                binding.tvCardCenterText.text = "Top Artist\nMix"
                binding.tvCardCenterText.visibility = View.VISIBLE
                binding.tvCardRecapText.visibility = View.GONE
                binding.ivCardRecapHeadset.visibility = View.GONE
                binding.tvMixSubtitle.text = getString(R.string.mix_sub_top_artist, mixSongs.size)
            }
            "never_played" -> {
                binding.cardPlate1.setBackgroundResource(R.drawable.bg_mix_never)
                binding.cardPlate2.setBackgroundResource(R.drawable.bg_mix_never)
                binding.bgCardMain.setBackgroundResource(R.drawable.bg_mix_never)
                binding.tvCardCenterText.text = "Never\nPlayed"
                binding.tvCardCenterText.visibility = View.VISIBLE
                binding.tvCardRecapText.visibility = View.GONE
                binding.ivCardRecapHeadset.visibility = View.GONE
                binding.tvMixSubtitle.text = getString(R.string.mix_sub_never_played, mixSongs.size)
            }
            "high_quality" -> {
                binding.cardPlate1.setBackgroundResource(R.drawable.bg_mix_hq)
                binding.cardPlate2.setBackgroundResource(R.drawable.bg_mix_hq)
                binding.bgCardMain.setBackgroundResource(R.drawable.bg_mix_hq)
                binding.tvCardCenterText.text = "High\nQuality"
                binding.tvCardCenterText.visibility = View.VISIBLE
                binding.tvCardRecapText.visibility = View.GONE
                binding.ivCardRecapHeadset.visibility = View.GONE
                binding.tvMixSubtitle.text = getString(R.string.mix_sub_high_quality, mixSongs.size)
            }
            "music_recap" -> {
                binding.cardPlate1.setBackgroundResource(R.drawable.bg_card_recap_sunset)
                binding.cardPlate2.setBackgroundResource(R.drawable.bg_card_recap_sunset)
                binding.bgCardMain.setBackgroundResource(R.drawable.bg_card_recap_sunset)
                binding.tvCardCenterText.visibility = View.GONE
                binding.tvCardRecapText.visibility = View.VISIBLE
                binding.ivCardRecapHeadset.visibility = View.VISIBLE

                val monthAbbr = SimpleDateFormat("MMM", Locale.ENGLISH).format(Date())
                val monthFull = SimpleDateFormat("MMMM", Locale.ENGLISH).format(Date())
                val year = Calendar.getInstance().get(Calendar.YEAR)

                binding.tvCardRecapText.text = monthAbbr
                binding.tvMixSubtitle.text = getString(R.string.mix_sub_recap, "$monthFull $year")
            }
        }
    }

    private fun setupRecycler() {
        adapter = MixSongsAdapter(
            context = this,
            songs = mixSongs,
            isRecap = isRecapMode,
            playCounts = songPlayCounts
        )
        binding.recyclerMixSongs.layoutManager = LinearLayoutManager(this)
        binding.recyclerMixSongs.adapter = adapter

        adapter.onItemClick = { list, pos ->
            playSong(ArrayList(list), pos)
        }

        adapter.onMenuClick = { song, pos ->
            val isFav = FavouriteManager.isFavourite(song)
            val config = SongMenuConfig(
                play = true,
                playNext = true,
                addToQueue = true,
                delete = true,
                edit = true,
                addToFav = !isFav,
                removeFromFav = isFav,
                addToPlaylist = true,
                setAsRingtone = true,
                goToAlbum = true,
                hideSong = true,
                share = true
            )
            val sheet = SongOptionsBottomSheet(song, pos, config)
            sheet.onSongHidden = { s ->
                mixSongs.removeAll { it.id == s.id }
                adapter.updateList(mixSongs)
            }
            sheet.show(supportFragmentManager, "mix_song_menu_${song.id}")
        }
    }

    private fun playSong(list: ArrayList<Music>, position: Int) {
        if (list.isEmpty()) return
        MusicService.playlist = ArrayList(list)
        MusicService.position = position

        startService(
            Intent(this, MusicService::class.java).apply {
                action = Constants.ACTION_PLAY_AT
                putExtra("songPosition", position)
            }
        )

        sendBroadcast(
            Intent("SHOW_MINI_PLAYER").setPackage(packageName)
        )
        startActivity(Intent(this, PlayerActivity::class.java))
    }

    private fun saveAsPlaylist() {
        if (mixSongs.isEmpty()) {
            Toast.makeText(this, "No songs to save", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = MusicDatabase.getDatabase(applicationContext)
            val playlistDao = db.PlaylistDao()
            val existingNames = playlistDao.getAllPlaylistsList().map { it.name }

            val defaultName = if (mixType == "music_recap") {
                val cal = Calendar.getInstance()
                val monthShort = SimpleDateFormat("MMM", Locale.ENGLISH).format(Date())
                val year = cal.get(Calendar.YEAR)
                "$monthShort $year - Top Songs"
            } else {
                currentMixTitle.ifBlank { "My Mix" }
            }

            withContext(Dispatchers.Main) {
                CreatePlaylistDialog.show(
                    context = this@MixDetailsActivity,
                    titleRes = R.string.save_as_playlist,
                    initialName = defaultName,
                    positiveBtnRes = R.string.btn_save,
                    existingNames = existingNames
                ) { newPlaylistName ->
                    saveSongsToPlaylist(newPlaylistName)
                }
            }
        }
    }

    private fun saveSongsToPlaylist(playlistName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = MusicDatabase.getDatabase(applicationContext)
            val playlistDao = db.PlaylistDao()
            val songDao = db.PlaylistSongDao()

            val newId = playlistDao.insertPlaylist(PlaylistEntity(name = playlistName))

            for (song in mixSongs) {
                songDao.addSong(PlaylistSongEntity(playlistId = newId.toInt(), songId = song.id))
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MixDetailsActivity, getString(R.string.toast_playlist_saved), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isSongHighQuality(song: Music): Boolean {
        val lower = song.path.lowercase()
        if (lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(".alac") || lower.endsWith(".ape")) {
            return true
        }
        try {
            if (song.duration > 0) {
                val file = File(song.path)
                if (file.exists()) {
                    val kbps = (file.length() * 8) / song.duration
                    if (kbps >= 256) return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyThemeToActivity(this)
    }
}
