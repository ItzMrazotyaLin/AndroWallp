package com.itzmrazotyalin.androwallp.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.itzmrazotyalin.androwallp.R
import com.itzmrazotyalin.androwallp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwallp.data.system.SystemWallpaperInspector
import com.itzmrazotyalin.androwallp.data.wallpaper.WallpaperRepository
import com.itzmrazotyalin.androwallp.di.appContainer
import com.itzmrazotyalin.androwallp.domain.model.Wallpaper
import com.itzmrazotyalin.androwallp.domain.model.WallpaperTarget
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext

class LibraryViewModel(
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository,
    private val systemWallpaperInspector: SystemWallpaperInspector,
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

        wallpaperRepository.statuses
            .onEach { statuses ->
                _uiState.update { state -> state.copy(statuses = statuses) }
            }
            .launchIn(viewModelScope)

        settingsRepository.wallpaperConfig
            .onEach { config ->
                _uiState.update { state -> state.copy(scalingMode = config.scalingMode) }
            }
            .launchIn(viewModelScope)
    }

    fun refreshSystemState() {
        viewModelScope.launch {
            val isServiceActive = withContext(Dispatchers.IO) {
                systemWallpaperInspector.isOurLiveWallpaperActive()
            }
            if (!isServiceActive) {
                settingsRepository.clearActiveWallpaperIds()
            }
        }
    }

    fun onApplySelected(wallpaper: Wallpaper) {
        _uiState.update { state -> state.copy(applyTargetFor = wallpaper) }
    }

    fun onApplyTargetDismissed() {
        _uiState.update { state -> state.copy(applyTargetFor = null) }
    }

    fun onApplyTargetSelected(target: WallpaperTarget) {
        val wallpaper = _uiState.value.applyTargetFor ?: return
        _uiState.update { state -> state.copy(applyTargetFor = null) }
        viewModelScope.launch {
            when (target) {
                WallpaperTarget.HOME -> wallpaperRepository.setActiveHome(wallpaper.id)
                WallpaperTarget.LOCK -> wallpaperRepository.setActiveLock(wallpaper.id)
                WallpaperTarget.BOTH -> wallpaperRepository.setActiveForBothScreens(wallpaper.id)
            }
            _events.send(LibraryEvent.ApplyLiveWallpaper)
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
                    systemWallpaperInspector = appContainer.systemWallpaperInspector,
                )
            }
        }
    }
}