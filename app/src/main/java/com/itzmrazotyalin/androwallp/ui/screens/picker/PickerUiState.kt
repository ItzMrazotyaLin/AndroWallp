package com.itzmrazotyalin.androwallp.ui.screens.picker

import com.itzmrazotyalin.androwallp.domain.model.Wallpaper

data class EditableVideo(
    val wallpaperId: String?,
    val uri: String?,
    val title: String,
    val displayName: String,
    val previewPath: String?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
) {
    val isEditMode: Boolean get() = wallpaperId != null

    companion object {

        fun fromWallpaper(wallpaper: Wallpaper): EditableVideo = EditableVideo(
            wallpaperId = wallpaper.id,
            uri = null,
            title = wallpaper.title,
            displayName = wallpaper.title,
            previewPath = wallpaper.thumbnailPath,
            durationMs = wallpaper.durationMs,
            width = wallpaper.width,
            height = wallpaper.height,
            sizeBytes = wallpaper.sizeBytes,
        )
    }
}

data class PickerUiState(
    val isInspecting: Boolean = false,
    val isSaving: Boolean = false,
    val video: EditableVideo? = null,
)

sealed interface PickerEvent {

    data class Saved(val title: String) : PickerEvent

    data object ReadFailed : PickerEvent

    data object ImportFailed : PickerEvent
}