package com.rifsxd.ksunext.ui.util

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.*

object LocaleHelper {
    
    /**
     * Check if should use system language settings (Android 13+)
     */
    val useSystemLanguageSettings: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    
    /**
     * Launch system app locale settings (Android 13+)
     */
    fun launchSystemLanguageSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback to app language settings if system settings not available
            }
        }
    }
    
    /**
     * Apply saved language setting to context (for Android < 13)
     */
    fun applyLanguage(context: Context): Context {
        // On Android 13+, language is handled by system
        if (useSystemLanguageSettings) {
            return context
        }
        
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val localeTag = prefs.getString("app_locale", "system") ?: "system"
        
        return if (localeTag == "system") {
            context
        } else {
            val locale = parseLocaleTag(localeTag)
            setLocale(context, locale)
        }
    }
    
    /**
     * Set locale for context (Android < 13)
     */
    private fun setLocale(context: Context, locale: Locale): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, locale)
        } else {
            updateResourcesLegacy(context, locale)
        }
    }
    
    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResources(context: Context, locale: Locale): Context {
        val configuration = Configuration()
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
    
    @Suppress("DEPRECATION")
    @SuppressWarnings("deprecation")
    private fun updateResourcesLegacy(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        configuration.setLayoutDirection(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
    
    /**
     * Parse locale tag to Locale object
     */
    private fun parseLocaleTag(tag: String): Locale {
        return try {
            if (tag.contains("_")) {
                val parts = tag.split("_")
                Locale.Builder()
                    .setLanguage(parts[0])
                    .setRegion(parts.getOrNull(1) ?: "")
                    .build()
            } else {
                Locale.Builder()
                    .setLanguage(tag)
                    .build()
            }
        } catch (_: Exception) {
            Locale.getDefault()
        }
    }
    
    /**
     * Restart activity to apply language change (Android < 13)
     */
    fun restartActivity(context: Context) {
        if (context is Activity && !useSystemLanguageSettings) {
            context.recreate()
        }
    }
    
    /**
     * Get current app locale
     */
    fun getCurrentAppLocale(context: Context): Locale? {
        return if (useSystemLanguageSettings) {
            // Android 13+ - get from system app locale settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? android.app.LocaleManager
                    val locales = localeManager?.applicationLocales
                    if (locales != null && !locales.isEmpty) {
                        locales.get(0)
                    } else {
                        null // System default
                    }
                } catch (_: Exception) {
                    null // System default
                }
            } else {
                null // System default
            }
        } else {
            // Android < 13 - get from SharedPreferences
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val localeTag = prefs.getString("app_locale", "system") ?: "system"
            if (localeTag == "system") {
                null // System default
            } else {
                parseLocaleTag(localeTag)
            }
        }
    }
    
    /**
     * Dynamically detect available locales by scanning resource directories
     * This works with Crowdin translations and automatically picks up new languages
     */
    fun getAvailableLocales(context: Context): List<Locale> {
        val locales = mutableListOf<Locale>()
        
        // Add system default first
        locales.add(Locale.ROOT) // This will represent "System Default"
        
        try {
            val availableLocales = mutableSetOf<String>()
            
            // Check for values-* directories in resources
            context.resources.configuration.locales.let { localeList ->
                for (i in 0 until localeList.size()) {
                    val locale = localeList[i]
                    availableLocales.add(locale.language)
                    if (locale.country.isNotEmpty()) {
                        availableLocales.add("${locale.language}-r${locale.country}")
                    }
                }
            }
            
            // Always include English as fallback
            availableLocales.add("en")
            
            availableLocales.forEach { dir ->
                try {
                    val locale = when {
                        dir.contains("-r") -> {
                            val parts = dir.split("-r")
                            Locale.Builder()
                                .setLanguage(parts[0])
                                .setRegion(parts[1])
                                .build()
                        }
                        else -> Locale.Builder()
                            .setLanguage(dir)
                            .build()
                    }
                    
                    // Test if this locale has translated resources
                    val config = android.content.res.Configuration()
                    config.setLocale(locale)
                    val localizedContext = context.createConfigurationContext(config)
                    
                    // Try to get a translated string to verify the locale is supported
                    val testString = try {
                        localizedContext.getString(android.R.string.ok) // Use system string as test
                    } catch (e: Exception) {
                        context.getString(android.R.string.ok)
                    }
                    val defaultString = context.getString(android.R.string.ok)
                    
                    // If the string is different or it's English, it's supported
                    if (testString != defaultString || locale.language == "en") {
                        locales.add(locale)
                    }
                } catch (e: Exception) {
                    // Skip unsupported locales
                }
            }
        } catch (e: Exception) {
            // Fallback to English only if detection fails
            locales.add(Locale.ENGLISH)
        }
        
        // Sort by display name (excluding system default)
        val sortedLocales = locales.drop(1).sortedBy { it.getDisplayName(it) }
        return mutableListOf<Locale>().apply {
            add(locales.first()) // System default first
            addAll(sortedLocales)
        }
    }
}
