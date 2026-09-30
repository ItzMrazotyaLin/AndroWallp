package com.itzmrazotyalin.androwalp.ui.screens.picker

import com.itzmrazotyalin.androwalp.domain.model.VideoMetadata
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling

data class PickerUiState(
    val isInspecting: Boolean = false,
    val isSaving: Boolean = false,
    val metadata: VideoMetadata? = null,
    val scaling: WallpaperScaling = WallpaperScaling.CENTER_CROP,
)

sealed interface PickerEvent {

    data class Saved(val title: String) : PickerEvent

    data object ReadFailed : PickerEvent

    data object ImportFailed : PickerEvent
}