package com.dhananjaysaini.musicplayerapp.utils

    import android.content.Context
    import android.content.SharedPreferences
    import com.dhananjaysaini.musicplayerapp.model.Music
    import com.google.gson.Gson
    import com.google.gson.reflect.TypeToken

    /**
     * Handles adding/removing songs to Favourites and persists them in SharedPreferences.
     */
    object FavouriteManager {

        private const val PREF_NAME = "Favourites_pref"
        private const val KEY_FavouriteS = "Favourite_songs"

        private val FavouriteSongs = mutableListOf<Music>()
        private var prefs: SharedPreferences? = null
        private val gson = Gson()

        /** Initialize in Application or first activity before using FavouriteManager */
        fun init(context: Context) {
            if (prefs == null) {
                prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                loadFavourites()
            }
        }

        /** Returns an immutable copy of Favourites */
        fun getFavourites(): List<Music> = synchronized(FavouriteSongs) { FavouriteSongs.toList() }

        /** Check if a song is marked as Favourite */
        fun isFavourite(song: Music): Boolean = synchronized(FavouriteSongs) {
            FavouriteSongs.any { it.id == song.id }
        }

        /** Check if a song id is marked as Favourite */
        fun isFavouriteById(songId: String): Boolean = synchronized(FavouriteSongs) {
            FavouriteSongs.any { it.id == songId }
        }

        /**
         * Toggle Favourite state for a song.
         * @return true if added, false if removed
         */
        fun toggleFavourite(song: Music): Boolean = synchronized(FavouriteSongs) {
            val isFav = FavouriteSongs.any { it.id == song.id }
            if (isFav) {
                FavouriteSongs.removeAll { it.id == song.id }
            } else {
                FavouriteSongs.add(song)
            }
            saveFavourites()
            !isFav
        }

        /** Add song explicitly */
        fun addFavourite(song: Music) = synchronized(FavouriteSongs) {
            if (FavouriteSongs.none { it.id == song.id }) {
                FavouriteSongs.add(song)
                saveFavourites()
            }
        }

        /** Remove song explicitly */
        fun removeFavourite(song: Music) = synchronized(FavouriteSongs) {
            FavouriteSongs.removeAll { it.id == song.id }
            saveFavourites()
        }

        /** Load Favourites from SharedPreferences */
        private fun loadFavourites() {
            val sp = prefs ?: return
            val json = sp.getString(KEY_FavouriteS, null) ?: return
            try {
                val type = object : TypeToken<MutableList<Music>>() {}.type
                val list: MutableList<Music> = gson.fromJson(json, type)
                synchronized(FavouriteSongs) {
                    FavouriteSongs.clear()
                    FavouriteSongs.addAll(list)
                }
            } catch (_: Exception) {}
        }

        /** Save Favourites to SharedPreferences */
        private fun saveFavourites() {
            val sp = prefs ?: return
            val json = synchronized(FavouriteSongs) { gson.toJson(FavouriteSongs) }
            sp.edit().putString(KEY_FavouriteS, json).apply()
        }


}