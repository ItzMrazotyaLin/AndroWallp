package com.itzmrazotyalin.androwalp.ui.screens.library

import androidx.annotation.StringRes
import com.itzmrazotyalin.androwalp.R
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper

data class LibraryUiState(
    val isLoading: Boolean = true,
    val wallpapers: List<Wallpaper> = emptyList(),
    val pendingDelete: Wallpaper? = null,
    val details: Wallpaper? = null,
)

sealed interface LibraryEvent {

    data class ShowMessage(
        @get:StringRes val messageRes: Int,
        val formatArg: String? = null,
    ) : LibraryEvent
}