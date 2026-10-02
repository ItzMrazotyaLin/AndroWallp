package com.itzmrazotyalin.androwallp.ui.screens.library

import androidx.annotation.StringRes
import com.itzmrazotyalin.androwallp.domain.model.Wallpaper
import com.itzmrazotyalin.androwallp.domain.model.WallpaperScaling
import com.itzmrazotyalin.androwallp.domain.model.WallpaperStatus

data class LibraryUiState(
    val isLoading: Boolean = true,
    val wallpapers: List<Wallpaper> = emptyList(),
    val statuses: Map<String, WallpaperStatus> = emptyMap(),
    val scalingMode: WallpaperScaling = WallpaperScaling.CENTER_CROP,
    val applyTargetFor: Wallpaper? = null,
    val pendingDelete: Wallpaper? = null,
    val details: Wallpaper? = null,
)

sealed interface LibraryEvent {

    data class ShowMessage(
        @get:StringRes val messageRes: Int,
        val formatArg: String? = null,
    ) : LibraryEvent

    data object ApplyLiveWallpaper : LibraryEvent
}