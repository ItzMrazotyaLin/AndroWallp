package com.itzmrazotyalin.androwalp.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.itzmrazotyalin.androwalp.R

private const val IMPORT_ROUTE_PATTERN = "import?wallpaperId={wallpaperId}"

enum class AppDestination(
    val route: String,
    @get:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    LIBRARY(route = "library", labelRes = R.string.nav_library, icon = Icons.Filled.VideoLibrary),
    IMPORT(route = IMPORT_ROUTE_PATTERN, labelRes = R.string.nav_import, icon = Icons.Filled.AddPhotoAlternate),
    SETTINGS(route = "settings", labelRes = R.string.nav_settings, icon = Icons.Filled.Settings),
    ;

    companion object {

        const val WALLPAPER_ID_ARG = "wallpaperId"

        fun importRoute(wallpaperId: String? = null): String =
            if (wallpaperId.isNullOrBlank()) "import" else "import?$WALLPAPER_ID_ARG=$wallpaperId"
    }
}