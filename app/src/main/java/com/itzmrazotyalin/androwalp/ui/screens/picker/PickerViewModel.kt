package com.itzmrazotyalin.androwalp.ui.screens.picker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.itzmrazotyalin.androwalp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwalp.data.wallpaper.VideoImporter
import com.itzmrazotyalin.androwalp.data.wallpaper.WallpaperRepository
import com.itzmrazotyalin.androwalp.di.appContainer
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PickerViewModel(
    private val videoImporter: VideoImporter,
    private val wallpaperRepository: WallpaperRepository,
    private val settingsRepository: SettingsRepository,
    private val wallpaperId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PickerUiState())
    val uiState: StateFlow<PickerUiState> = _uiState.asStateFlow()

    val scalingMode: StateFlow<WallpaperScaling> = settingsRepository.wallpaperConfig
        .map { config -> config.scalingMode }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = WallpaperScaling.CENTER_CROP,
        )

    private val _events = Channel<PickerEvent>(Channel.BUFFERED)
    val events: Flow<PickerEvent> = _events.receiveAsFlow()

    init {
        if (wallpaperId.isNotBlank()) {
            wallpaperRepository.wallpaperById(wallpaperId)
                .onEach { wallpaper ->
                    if (wallpaper != null && _uiState.value.video == null) {
                        _uiState.update { state ->
                            state.copy(video = EditableVideo.fromWallpaper(wallpaper))
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun onVideoPicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isInspecting = true) }
            val metadata = runCatching { videoImporter.inspect(uri) }.getOrNull()
            _uiState.update { state ->
                state.copy(
                    isInspecting = false,
                    video = metadata?.let {
                        EditableVideo(
                            wallpaperId = null,
                            uri = uri.toString(),
                            title = videoImporter.defaultTitle(it.displayName),
                            displayName = it.displayName,
                            previewPath = it.previewPath,
                            durationMs = it.durationMs,
                            width = it.width,
                            height = it.height,
                            sizeBytes = it.sizeBytes,
                        )
                    },
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

    fun onTitleChanged(title: String) {
        _uiState.update { state ->
            val video = state.video ?: return@update state
            state.copy(video = video.copy(title = title))
        }
    }

    fun onScalingSelected(scaling: WallpaperScaling) {
        viewModelScope.launch { settingsRepository.setScalingMode(scaling) }
    }

    fun onVideoCleared() {
        _uiState.update { state -> state.copy(video = null) }
    }

    fun onSaveRequested() {
        val video = _uiState.value.video ?: return
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isSaving = true) }
            val existingId = video.wallpaperId
            val uri = video.uri
            val result = runCatching {
                if (existingId != null) {
                    wallpaperRepository.update(existingId, video.title)
                    video.title
                } else {
                    val uriValue = requireNotNull(uri)
                    wallpaperRepository.import(Uri.parse(uriValue), video.title).title
                }
            }
            _uiState.update { state -> state.copy(isSaving = false, video = null) }
            result
                .onSuccess { title -> _events.send(PickerEvent.Saved(title)) }
                .onFailure { _events.send(PickerEvent.ImportFailed) }
        }
    }

    companion object {

        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(wallpaperId: String) = viewModelFactory {
            initializer {
                PickerViewModel(
                    videoImporter = appContainer.videoImporter,
                    wallpaperRepository = appContainer.wallpaperRepository,
                    settingsRepository = appContainer.settingsRepository,
                    wallpaperId = wallpaperId,
                )
            }
        }
    }
}