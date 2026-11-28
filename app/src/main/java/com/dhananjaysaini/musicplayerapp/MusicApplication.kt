package com.dhananjaysaini.musicplayerapp

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dhananjaysaini.musicplayerapp.utils.LocaleHelper

class MusicApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }

    override fun onCreate() {
        super.onCreate()
        com.dhananjaysaini.musicplayerapp.utils.FavouriteManager.init(this)
        val savedLang = LocaleHelper.getSavedLanguageCode(this)
        val appLocale = LocaleListCompat.forLanguageTags(savedLang)
        if (AppCompatDelegate.getApplicationLocales() != appLocale) {
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
        LocaleHelper.applyLocale(this)
    }
}
