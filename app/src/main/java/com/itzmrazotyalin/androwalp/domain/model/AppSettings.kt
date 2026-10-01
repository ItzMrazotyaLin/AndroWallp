package com.itzmrazotyalin.androwalp.domain.model

enum class ThemeMode {
    FOLLOW_SYSTEM,
    FORCE_DARK,
    FORCE_LIGHT,
}

data class AppSettings(
    val resetOnUnlock: Boolean = true,
    val muteHome: Boolean = true,
    val muteLock: Boolean = true,
    val scalingMode: WallpaperScaling = WallpaperScaling.CENTER_CROP,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val useDynamicColor: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
)

data class WallpaperConfig(
    val homeWallpaperPath: String?,
    val lockWallpaperPath: String?,
    val resetOnUnlock: Boolean,
    val muteHome: Boolean,
    val muteLock: Boolean,
    val scalingMode: WallpaperScaling,
)