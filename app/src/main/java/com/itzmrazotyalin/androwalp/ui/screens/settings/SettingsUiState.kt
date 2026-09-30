package com.itzmrazotyalin.androwalp.ui.screens.settings

import com.itzmrazotyalin.androwalp.domain.model.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
)