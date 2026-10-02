package com.itzmrazotyalin.androwallp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.itzmrazotyalin.androwallp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwallp.di.appContainer
import com.itzmrazotyalin.androwallp.domain.model.AppLanguage
import com.itzmrazotyalin.androwallp.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState = settingsRepository.settings
        .map { settings -> SettingsUiState(settings = settings) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState(),
        )

    fun onResetOnUnlockChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setResetOnUnlock(enabled) }
    }

    fun onMuteHomeChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMuteHome(enabled) }
    }

    fun onMuteLockChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMuteLock(enabled) }
    }

    fun onUseDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUseDynamicColor(enabled) }
    }

    fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    companion object {

        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer { SettingsViewModel(appContainer.settingsRepository) }
        }
    }
}