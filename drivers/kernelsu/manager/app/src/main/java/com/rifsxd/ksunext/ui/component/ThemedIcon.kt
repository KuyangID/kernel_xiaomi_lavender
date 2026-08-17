package com.rifsxd.ksunext.ui.component

import android.content.pm.PackageInfo
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rifsxd.ksunext.ksuApp
import com.rifsxd.ksunext.ui.util.IconPackHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ThemedIcon(
    packageName: String,
    packageInfo: PackageInfo,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { ksuApp.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    
    var themedIcon by remember(packageName) { mutableStateOf<BitmapPainter?>(null) }
    var useSystemIcon by remember(packageName) { mutableStateOf(true) }
    var enabledThemes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var themePriorities by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Load themed icon based on priority system
    LaunchedEffect(packageName) {
        enabledThemes = prefs.getStringSet("enabled_icon_themes", setOf("default")) ?: setOf("default")
        val priorityString = prefs.getString("icon_theme_priorities", "default") ?: "default"
        themePriorities = priorityString.split(",").filter { it.isNotEmpty() }
        
        withContext(Dispatchers.IO) {
            // Try to get themed icon from enabled themes in priority order
            for (themeId in themePriorities) {
                if (enabledThemes.contains(themeId) && themeId != "default") {
                    val drawable = IconPackHelper.getThemedIcon(context, themeId, packageName)
                    if (drawable != null) {
                        val bitmap = drawable.toBitmap()
                        themedIcon = BitmapPainter(bitmap.asImageBitmap())
                        useSystemIcon = false
                        break
                    }
                }
            }
        }
    }
    
    if (useSystemIcon || themedIcon == null) {
        // Use system default icon
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(packageInfo)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        // Use themed icon
        themedIcon?.let { painter ->
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = modifier
            )
        }
    }
}
