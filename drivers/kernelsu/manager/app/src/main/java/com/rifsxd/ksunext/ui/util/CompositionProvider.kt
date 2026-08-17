package com.rifsxd.ksunext.ui.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf

val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("CompositionLocal LocalSnackbarController not present")
}

// Photo Editor CompositionLocal providers
val LocalPhotoEditorSaveCallback = compositionLocalOf<(() -> Unit)?> { null }
val LocalPhotoEditorSaveCallbackSetter = compositionLocalOf<(((() -> Unit)?) -> Unit)?> { null }
val LocalPhotoEditorResetCallback = compositionLocalOf<(() -> Unit)?> { null }
val LocalPhotoEditorScreenRotationCallback = compositionLocalOf<(() -> Unit)?> { null }
val LocalPhotoEditorScreenRotationLocked = compositionLocalOf<Boolean> { false }
