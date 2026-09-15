package com.opensource.netlens

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class NetLensApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(wrapLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun wrapLocale(base: Context): Context {
        val prefs = base.getSharedPreferences("netlens_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "system") ?: "system"
        if (lang == "system") return base
        val locale = when (lang) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            else -> return base
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
