package com.itzmrazotyalin.androwalp.domain.model

enum class ThemeMode {
    FOLLOW_SYSTEM,
    FORCE_DARK,
    FORCE_LIGHT,
}

data class AppSettings(
    val resetPlaybackOnUnlock: Boolean = true,
    val muteAudioOnHomeScreen: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val useDynamicColor: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
)