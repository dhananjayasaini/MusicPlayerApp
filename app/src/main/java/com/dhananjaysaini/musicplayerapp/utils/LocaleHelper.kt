package com.dhananjaysaini.musicplayerapp.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dhananjaysaini.musicplayerapp.R
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANG_CODE = "selected_language_code"
    const val DEFAULT_LANG_CODE = "en"

    data class LanguageItem(
        val code: String,          // ISO language code, e.g. "en", "es", "hi"
        val nativeName: String,    // Native display name, e.g. "Español", "हिन्दी"
        val englishName: String    // English display name, e.g. "Spanish", "Hindi"
    )

    // All 24 languages from the Settings language selector in exact order
    val supportedLanguages = listOf(
        LanguageItem("en", "English", "English"),
        LanguageItem("es", "Español", "Spanish"),
        LanguageItem("id", "Bahasa Indonesia", "Indonesian"),
        LanguageItem("pt", "Português do Brasil", "Portuguese (Brazil)"),
        LanguageItem("ru", "Русский", "Russian"),
        LanguageItem("tr", "Türkçe", "Turkish"),
        LanguageItem("de", "Deutsch", "German"),
        LanguageItem("fr", "Français", "French"),
        LanguageItem("it", "Italiano", "Italian"),
        LanguageItem("ja", "日本語", "Japanese"),
        LanguageItem("pl", "Polski", "Polish"),
        LanguageItem("ar", "العربية", "Arabic"),
        LanguageItem("cs", "čeština", "Czech"),
        LanguageItem("th", "ไทย", "Thai"),
        LanguageItem("ko", "한국어", "Korean"),
        LanguageItem("zh-CN", "简体中文", "Chinese (Simplified)"),
        LanguageItem("zh-TW", "繁體中文", "Chinese (Traditional)"),
        LanguageItem("hi", "हिन्दी", "Hindi"),
        LanguageItem("vi", "Tiếng Việt", "Vietnamese"),
        LanguageItem("ms", "Bahasa Melayu", "Malay"),
        LanguageItem("fa", "فارسی", "Persian"),
        LanguageItem("uk", "українська", "Ukrainian"),
        LanguageItem("sv", "Svenska", "Swedish"),
        LanguageItem("bn", "বাংলা", "Bengali")
    )

    fun getSavedLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG_CODE, DEFAULT_LANG_CODE) ?: DEFAULT_LANG_CODE
    }

    fun getCurrentLanguage(context: Context): LanguageItem {
        val code = getSavedLanguageCode(context)
        return supportedLanguages.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: supportedLanguages[0]
    }

    fun setLocale(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG_CODE, languageCode).commit()

        // Sync with settings_pref so any existing read is consistent
        val langItem = supportedLanguages.firstOrNull { it.code.equals(languageCode, ignoreCase = true) }
            ?: supportedLanguages[0]
        context.getSharedPreferences("settings_pref", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", langItem.nativeName)
            .putString("app_language_code", languageCode)
            .commit()

        // Official Android Jetpack AppCompatDelegate application locales
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun applyLocale(context: Context): Context {
        val langCode = getSavedLanguageCode(context)
        return updateResources(context, langCode)
    }

    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = parseLocale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        return context.createConfigurationContext(config)
    }

    private fun parseLocale(languageCode: String): Locale {
        return Locale.forLanguageTag(languageCode)
    }

    // -------------------------------------------------------------------------
    // Localized Quantity / Count Helpers
    // -------------------------------------------------------------------------

    fun getSongCountString(context: Context, count: Int): String {
        return when (count) {
            0 -> context.getString(R.string.song_count_zero)
            1 -> context.getString(R.string.song_count_one, 1)
            else -> context.getString(R.string.song_count_other, count)
        }
    }

    fun getPlaylistCountString(context: Context, count: Int): String {
        return when (count) {
            1 -> context.getString(R.string.playlist_count_one, 1)
            else -> context.getString(R.string.playlist_count_other, count)
        }
    }

    fun getAlbumCountString(context: Context, count: Int): String {
        return when (count) {
            0 -> context.getString(R.string.album_count_zero)
            1 -> context.getString(R.string.album_count_one, 1)
            else -> context.getString(R.string.album_count_other, count)
        }
    }

    fun getArtistCountString(context: Context, count: Int): String {
        return when (count) {
            0 -> context.getString(R.string.artist_count_zero)
            1 -> context.getString(R.string.artist_count_one, 1)
            else -> context.getString(R.string.artist_count_other, count)
        }
    }

    fun getGenreCountString(context: Context, count: Int): String {
        return when (count) {
            0 -> context.getString(R.string.genre_count_zero)
            1 -> context.getString(R.string.genre_count_one, 1)
            else -> context.getString(R.string.genre_count_other, count)
        }
    }

    fun getFolderCountString(context: Context, count: Int): String {
        return when (count) {
            0 -> context.getString(R.string.folder_count_zero)
            1 -> context.getString(R.string.folder_count_one, 1)
            else -> context.getString(R.string.folder_count_other, count)
        }
    }
}
