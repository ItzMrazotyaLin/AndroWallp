package com.itzmrazotyalin.androwalp.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.itzmrazotyalin.androwalp.R

enum class AppDestination(
    val route: String,
    @get:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    LIBRARY(route = "library", labelRes = R.string.nav_library, icon = Icons.Filled.VideoLibrary),
    IMPORT(route = "import", labelRes = R.string.nav_import, icon = Icons.Filled.AddPhotoAlternate),
    SETTINGS(route = "settings", labelRes = R.string.nav_settings, icon = Icons.Filled.Settings),
}