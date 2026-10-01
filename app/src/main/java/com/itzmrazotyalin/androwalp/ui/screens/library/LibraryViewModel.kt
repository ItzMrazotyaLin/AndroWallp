package com.itzmrazotyalin.androwalp.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.itzmrazotyalin.androwalp.R
import com.itzmrazotyalin.androwalp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwalp.data.wallpaper.WallpaperRepository
import com.itzmrazotyalin.androwalp.di.appContainer
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper
import com.itzmrazotyalin.androwalp.domain.model.WallpaperTarget
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val wallpaperRepository: WallpaperRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryEvent> = _events.receiveAsFlow()

    init {
        wallpaperRepository.wallpapers
            .onEach { wallpapers ->
                _uiState.update { state ->
                    state.copy(isLoading = false, wallpapers = wallpapers)
                }
            }
            .launchIn(viewModelScope)

        settingsRepository.wallpaperConfig
            .onEach { config ->
                _uiState.update { state -> state.copy(scalingMode = config.scalingMode) }
            }
            .launchIn(viewModelScope)
    }

    fun onApplySelected(wallpaper: Wallpaper) {
        _uiState.update { state -> state.copy(applyTarget = wallpaper) }
    }

    fun onApplyTargetDismissed() {
        _uiState.update { state -> state.copy(applyTarget = null) }
    }

    fun onApplyTargetConfirmed(target: WallpaperTarget) {
        val wallpaper = _uiState.value.applyTarget ?: return
        _uiState.update { state -> state.copy(applyTarget = null) }
        viewModelScope.launch {
            wallpaperRepository.setActive(wallpaper.id, target)
            _events.send(LibraryEvent.ApplyLiveWallpaper(target))
        }
    }

    fun onDeleteRequested(wallpaper: Wallpaper) {
        _uiState.update { state -> state.copy(pendingDelete = wallpaper) }
    }

    fun onDeleteDismissed() {
        _uiState.update { state -> state.copy(pendingDelete = null) }
    }

    fun onDeleteConfirmed() {
        val target = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            wallpaperRepository.delete(target.id)
            _uiState.update { state -> state.copy(pendingDelete = null) }
            _events.send(LibraryEvent.ShowMessage(R.string.message_deleted, target.title))
        }
    }

    fun onDetailsRequested(wallpaper: Wallpaper) {
        _uiState.update { state -> state.copy(details = wallpaper) }
    }

    fun onDetailsDismissed() {
        _uiState.update { state -> state.copy(details = null) }
    }

    companion object {

        val Factory = viewModelFactory {
            initializer {
                LibraryViewModel(
                    wallpaperRepository = appContainer.wallpaperRepository,
                    settingsRepository = appContainer.settingsRepository,
                )
            }
        }
    }
}