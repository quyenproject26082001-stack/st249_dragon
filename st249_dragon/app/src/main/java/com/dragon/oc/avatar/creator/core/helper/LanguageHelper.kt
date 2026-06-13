package com.dragon.oc.avatar.creator.core.helper

import android.content.Context
import android.content.res.Configuration

import java.util.Locale

object LanguageHelper {
    private var myLocale: Locale? = null

    fun wrapContext(context: Context): Context {
        val language = SharePreferenceHelper(context).getPreLanguage()
        if (language.isBlank()) return context

        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    fun setLocale(context: Context) {
        val language = SharePreferenceHelper(context).getPreLanguage()
        if (language.isEmpty()) {
            val config = Configuration()
            val locale = Locale.getDefault()
            Locale.setDefault(locale)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } else {
            changeLang(language, context)
        }
    }

    fun changeLang(lang: String, context: Context) {
        if (lang.equals("", ignoreCase = true)) return
        myLocale = Locale.forLanguageTag(lang)
        saveLocale(context, lang)
        Locale.setDefault(myLocale!!)
        val config = Configuration()
        config.setLocale(myLocale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun saveLocale(context: Context, lang: String) {
        SharePreferenceHelper(context).setPreLanguage(lang)
    }
}
