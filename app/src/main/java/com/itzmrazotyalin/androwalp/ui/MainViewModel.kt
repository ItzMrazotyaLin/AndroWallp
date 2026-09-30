package com.itzmrazotyalin.androwalp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.itzmrazotyalin.androwalp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwalp.di.appContainer
import com.itzmrazotyalin.androwalp.domain.model.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = AppSettings(),
        )

    companion object {

        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer { MainViewModel(appContainer.settingsRepository) }
        }
    }
}