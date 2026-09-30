package com.itzmrazotyalin.androwalp.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.itzmrazotyalin.androwalp.R
import com.itzmrazotyalin.androwalp.data.wallpaper.WallpaperRepository
import com.itzmrazotyalin.androwalp.di.appContainer
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val wallpaperRepository: WallpaperRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            wallpaperRepository.wallpapers.collect { wallpapers ->
                _uiState.update { state ->
                    state.copy(isLoading = false, wallpapers = wallpapers)
                }
            }
        }
    }

    fun onApplySelected(wallpaper: Wallpaper) {
        viewModelScope.launch {
            wallpaperRepository.setActive(wallpaper.id)
            _events.send(LibraryEvent.ShowMessage(R.string.message_engine_unavailable))
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
            initializer { LibraryViewModel(appContainer.wallpaperRepository) }
        }
    }
}