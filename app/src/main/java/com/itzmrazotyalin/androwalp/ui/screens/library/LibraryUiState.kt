package com.itzmrazotyalin.androwalp.ui.screens.library

import androidx.annotation.StringRes
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling
import com.itzmrazotyalin.androwalp.domain.model.WallpaperTarget

data class LibraryUiState(
    val isLoading: Boolean = true,
    val wallpapers: List<Wallpaper> = emptyList(),
    val scalingMode: WallpaperScaling = WallpaperScaling.CENTER_CROP,
    val pendingDelete: Wallpaper? = null,
    val details: Wallpaper? = null,
    val applyTarget: Wallpaper? = null,
)

sealed interface LibraryEvent {

    data class ShowMessage(
        @get:StringRes val messageRes: Int,
        val formatArg: String? = null,
    ) : LibraryEvent

    data class ApplyLiveWallpaper(val target: WallpaperTarget) : LibraryEvent
}