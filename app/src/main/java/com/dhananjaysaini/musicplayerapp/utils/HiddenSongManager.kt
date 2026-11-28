package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object HiddenSongManager {

    private const val PREF_NAME = "hidden_songs_pref"
    private const val KEY_HIDDEN_IDS = "hidden_song_ids"
    private const val KEY_HIDDEN_FOLDERS = "hidden_folder_paths"

    private var prefs: SharedPreferences? = null
    private val hiddenIds = mutableSetOf<String>()
    private val hiddenFolders = mutableSetOf<String>()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            loadHiddenData()
        }
    }

    private fun loadHiddenData() {
        val songSet = prefs?.getStringSet(KEY_HIDDEN_IDS, emptySet()) ?: emptySet()
        val folderSet = prefs?.getStringSet(KEY_HIDDEN_FOLDERS, emptySet()) ?: emptySet()
        synchronized(hiddenIds) {
            hiddenIds.clear()
            hiddenIds.addAll(songSet)
        }
        synchronized(hiddenFolders) {
            hiddenFolders.clear()
            hiddenFolders.addAll(folderSet.map { normalizePath(it) })
        }
    }

    private fun normalizePath(path: String): String {
        return try {
            File(path).absolutePath.trimEnd('/')
        } catch (e: Exception) {
            path.trimEnd('/')
        }
    }

    fun isSongHidden(songId: String, songPath: String? = null): Boolean {
        synchronized(hiddenIds) {
            if (hiddenIds.contains(songId)) return true
        }
        if (!songPath.isNullOrEmpty()) {
            return isPathInHiddenFolder(songPath)
        }
        return false
    }

    fun isSongDirectlyHidden(songId: String): Boolean {
        return synchronized(hiddenIds) {
            hiddenIds.contains(songId)
        }
    }

    fun isPathInHiddenFolder(filePath: String): Boolean {
        val parent = try {
            File(filePath).parent?.let { normalizePath(it) }
        } catch (e: Exception) {
            null
        } ?: return false

        return synchronized(hiddenFolders) {
            hiddenFolders.any { hidden ->
                parent.equals(hidden, ignoreCase = true) ||
                        parent.startsWith("$hidden/", ignoreCase = true)
            }
        }
    }

    fun isFolderHidden(folderPath: String): Boolean {
        val norm = normalizePath(folderPath)
        return synchronized(hiddenFolders) {
            hiddenFolders.contains(norm)
        }
    }

    fun getHiddenCount(context: Context? = null): Int {
        if (context != null) init(context)
        val songs = synchronized(hiddenIds) { hiddenIds.size }
        val folders = synchronized(hiddenFolders) { hiddenFolders.size }
        return songs + folders
    }

    fun getHiddenSongCount(context: Context? = null): Int {
        if (context != null) init(context)
        return synchronized(hiddenIds) { hiddenIds.size }
    }

    fun getHiddenFolderCount(context: Context? = null): Int {
        if (context != null) init(context)
        return synchronized(hiddenFolders) { hiddenFolders.size }
    }

    fun getHiddenSongIds(): Set<String> {
        return synchronized(hiddenIds) {
            hiddenIds.toSet()
        }
    }

    fun getHiddenFolderPaths(): Set<String> {
        return synchronized(hiddenFolders) {
            hiddenFolders.toSet()
        }
    }

    fun hideSong(context: Context, songId: String) {
        init(context)
        synchronized(hiddenIds) {
            hiddenIds.add(songId)
            prefs?.edit()?.putStringSet(KEY_HIDDEN_IDS, HashSet(hiddenIds))?.apply()
        }
    }

    fun unhideSong(context: Context, songId: String) {
        init(context)
        synchronized(hiddenIds) {
            hiddenIds.remove(songId)
            prefs?.edit()?.putStringSet(KEY_HIDDEN_IDS, HashSet(hiddenIds))?.apply()
        }
    }

    fun hideFolder(context: Context, folderPath: String) {
        init(context)
        val norm = normalizePath(folderPath)
        synchronized(hiddenFolders) {
            hiddenFolders.add(norm)
            prefs?.edit()?.putStringSet(KEY_HIDDEN_FOLDERS, HashSet(hiddenFolders))?.apply()
        }
    }

    fun unhideFolder(context: Context, folderPath: String) {
        init(context)
        val norm = normalizePath(folderPath)
        synchronized(hiddenFolders) {
            hiddenFolders.remove(norm)
            prefs?.edit()?.putStringSet(KEY_HIDDEN_FOLDERS, HashSet(hiddenFolders))?.apply()
        }
    }

    fun unhideAll(context: Context) {
        init(context)
        synchronized(hiddenIds) {
            hiddenIds.clear()
        }
        synchronized(hiddenFolders) {
            hiddenFolders.clear()
        }
        prefs?.edit()?.remove(KEY_HIDDEN_IDS)?.remove(KEY_HIDDEN_FOLDERS)?.apply()
    }
}
