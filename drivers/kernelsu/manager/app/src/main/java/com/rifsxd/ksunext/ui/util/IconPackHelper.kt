package com.rifsxd.ksunext.ui.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat

data class IconPack(
    val packageName: String,
    val name: String,
    val icon: Drawable?
)

object IconPackHelper {
    private const val TAG = "IconPackHelper"
    
    // Consolidated icon pack detection methods
    private val ICON_PACK_QUERIES = listOf(
        // Intent actions
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.fede.launcher.THEME_ICONPACK",
        "com.anddoes.launcher.THEME",
        "com.actionlauncher.playstore.THEME",
        "ch.deletescape.lawnchair.THEME",
        "app.lawnchair.THEME"
    )
    
    /**
     * Get all installed icon packs on the system
     */
    fun getInstalledIconPacks(context: Context): List<IconPack> {
        val iconPacksMap = mutableMapOf<String, IconPack>()
        val packageManager = context.packageManager
        
        // Search by intent actions
        for (query in ICON_PACK_QUERIES) {
            try {
                val intent = Intent(query)
                val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                
                for (resolveInfo in resolveInfos) {
                    val packageName = resolveInfo.activityInfo.packageName
                    
                    // Skip if already found
                    if (iconPacksMap.containsKey(packageName)) continue
                    
                    val appInfo = try {
                        packageManager.getApplicationInfo(packageName, 0)
                    } catch (e: PackageManager.NameNotFoundException) {
                        continue
                    }
                    
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = try {
                        packageManager.getApplicationIcon(packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                    
                    iconPacksMap[packageName] = IconPack(packageName, name, icon)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error querying $query", e)
            }
        }
        
        return iconPacksMap.values.toList().sortedBy { it.name }
    }
    
    /**
     * Get themed icon for a specific app package
     */
    fun getThemedIcon(context: Context, iconPackPackage: String, targetPackage: String): Drawable? {
        if (iconPackPackage.isEmpty() || iconPackPackage == "system_default") {
            return null // Use system default
        }
        
        try {
            val iconPackContext = context.createPackageContext(iconPackPackage, Context.CONTEXT_IGNORE_SECURITY)
            val iconPackResources = iconPackContext.resources
            
            // Try common naming patterns for icon resources
            val possibleNames = listOf(
                targetPackage,
                targetPackage.replace(".", "_"),
                "ic_${targetPackage.replace(".", "_")}",
                targetPackage.substringAfterLast(".")
            )
            
            for (name in possibleNames) {
                try {
                    val resourceId = iconPackResources.getIdentifier(name, "drawable", iconPackPackage)
                    if (resourceId != 0) {
                        return ContextCompat.getDrawable(iconPackContext, resourceId)
                    }
                } catch (e: Exception) {
                    // Continue to next name
                }
            }
            
        } catch (e: Exception) {
            // Error getting themed icon
        }
        
        return null
    }
}
