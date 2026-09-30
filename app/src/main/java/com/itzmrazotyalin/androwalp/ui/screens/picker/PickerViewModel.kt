package com.itzmrazotyalin.androwalp.ui.screens.picker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.itzmrazotyalin.androwalp.data.wallpaper.VideoImporter
import com.itzmrazotyalin.androwalp.data.wallpaper.WallpaperRepository
import com.itzmrazotyalin.androwalp.di.appContainer
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PickerViewModel(
    private val videoImporter: VideoImporter,
    private val wallpaperRepository: WallpaperRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PickerUiState())
    val uiState: StateFlow<PickerUiState> = _uiState.asStateFlow()

    private val _events = Channel<PickerEvent>(Channel.BUFFERED)
    val events: Flow<PickerEvent> = _events.receiveAsFlow()

    fun onVideoPicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isInspecting = true) }
            val metadata = runCatching { videoImporter.inspect(uri) }.getOrNull()
            _uiState.update { state ->
                state.copy(
                    isInspecting = false,
                    metadata = metadata,
                )
            }
            if (metadata == null) {
                _events.send(PickerEvent.ReadFailed)
            }
        }
    }

    fun onPickDismissed() {
        _uiState.update { state -> state.copy(isInspecting = false) }
    }

    fun onScalingSelected(scaling: WallpaperScaling) {
        _uiState.update { state -> state.copy(scaling = scaling) }
    }

    fun onSaveRequested() {
        val state = _uiState.value
        val metadata = state.metadata ?: return
        viewModelScope.launch {
            _uiState.update { current -> current.copy(isSaving = true) }
            val result = runCatching {
                wallpaperRepository.import(Uri.parse(metadata.uri), state.scaling)
            }
            _uiState.update { current -> current.copy(isSaving = false) }
            result
                .onSuccess { wallpaper -> _events.send(PickerEvent.Saved(wallpaper.title)) }
                .onFailure { _events.send(PickerEvent.ImportFailed) }
        }
    }

    companion object {

        val Factory = viewModelFactory {
            initializer {
                PickerViewModel(
                    videoImporter = appContainer.videoImporter,
                    wallpaperRepository = appContainer.wallpaperRepository,
                )
            }
        }
    }
}