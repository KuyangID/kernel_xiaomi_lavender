package com.rifsxd.ksunext.ui.component

import android.content.Context
import android.net.Uri

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun BackgroundImageWrapper(
    backgroundImageUri: String?,
    backgroundFitMode: String,
    backgroundTransparency: Float = 1.0f,
    backgroundBlur: Float = 0.0f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    

    

    
    Box(modifier = Modifier.fillMaxSize()) {
        // Display background image if available
        backgroundImageUri?.let { uriString ->
            if (uriString.isNotEmpty()) {

                
                // Validate and parse URI - memoized to prevent continuous parsing
                val validatedUri = remember(uriString) {
                    try {
                        val uri = Uri.parse(uriString)
                        uri
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (validatedUri == null) return@let
                
                val originalPainter = rememberAsyncImagePainter(
                    model = remember(uriString) {
                        ImageRequest.Builder(context)
                            .data(validatedUri)
                            .crossfade(false)

                            .build()
                    }
                )
                
                // Use original painter - blur will be applied via Compose modifier
                val painter = originalPainter
                
                // Load transform settings from SharedPreferences - remember to prevent continuous reads
                val scale = remember { prefs.getFloat("background_scale_x", 1f) }
                val offsetX = remember { prefs.getFloat("background_pos_x", 0f) }
                val offsetY = remember { prefs.getFloat("background_pos_y", 0f) }
                val rotation = remember { prefs.getFloat("background_rotation", 0f) }
                val flipHorizontal = remember { prefs.getBoolean("background_flip_horizontal", false) }
                val flipVertical = remember { prefs.getBoolean("background_flip_vertical", false) }
                
                // Apply transformations with improved blur modifier
                val imageModifier = Modifier
                    .fillMaxSize()
                    .blur(
                        radius = (backgroundBlur * 0.5f).dp, // Scale down blur for better quality
                        edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded
                    )
                    .graphicsLayer {
                        scaleX = scale * (if (flipHorizontal) -1f else 1f)
                        scaleY = scale * (if (flipVertical) -1f else 1f)
                        translationX = offsetX
                        translationY = offsetY
                        rotationZ = rotation
                    }
                
                // Use ContentScale based on fit mode
                val contentScale = when (backgroundFitMode) {
                    "fill" -> ContentScale.FillBounds
                    "crop" -> ContentScale.Crop
                    "center" -> ContentScale.None
                    "fit" -> ContentScale.Fit
                    else -> ContentScale.Fit
                }
                
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = imageModifier,
                    contentScale = contentScale,
                    alignment = androidx.compose.ui.Alignment.Center
                )
            }
        }
        
        // Overlay with darkness control for content readability - only apply when background image exists
        backgroundImageUri?.let { uriString ->
            if (uriString.isNotEmpty()) {
                val overlayAlpha = backgroundTransparency
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = overlayAlpha)
                        )
                )
            }
        }
        
        // Content on top of background
        content()
    }
}
