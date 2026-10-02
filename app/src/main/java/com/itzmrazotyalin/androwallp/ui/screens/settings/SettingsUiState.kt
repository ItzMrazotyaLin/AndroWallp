package com.itzmrazotyalin.androwallp.ui.screens.settings

import com.itzmrazotyalin.androwallp.domain.model.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
)