package com.itzmrazotyalin.androwalp.data.wallpaper

import android.net.Uri
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WallpaperRepository(
    private val videoImporter: VideoImporter,
    applicationScope: CoroutineScope,
) {

    private val _wallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())
    val wallpapers: StateFlow<List<Wallpaper>> = _wallpapers.asStateFlow()

    init {
        applicationScope.launch {
            _wallpapers.value = loadImportedWallpapers()
        }
    }

    suspend fun import(uri: Uri, scaling: WallpaperScaling): Wallpaper {
        val wallpaper = videoImporter.import(uri, scaling)
        _wallpapers.update { current -> listOf(wallpaper) + current }
        return wallpaper
    }

    fun setScaling(wallpaperId: String, scaling: WallpaperScaling) {
        _wallpapers.update { current ->
            current.map { wallpaper ->
                if (wallpaper.id == wallpaperId) wallpaper.copy(scaling = scaling) else wallpaper
            }
        }
    }

    suspend fun setActive(wallpaperId: String) {
        _wallpapers.update { current ->
            current.map { wallpaper -> wallpaper.copy(isActive = wallpaper.id == wallpaperId) }
        }
    }

    suspend fun delete(wallpaperId: String) {
        val removed = withContext(Dispatchers.IO) {
            val target = _wallpapers.value.firstOrNull { it.id == wallpaperId }
            target?.let(videoImporter::delete)
            target
        } ?: return
        _wallpapers.update { current -> current.filterNot { it.id == removed.id } }
    }

    private suspend fun loadImportedWallpapers(): List<Wallpaper> =
        withContext(Dispatchers.IO) {
            videoImporter.videoFiles().mapNotNull { file ->
                runCatching { videoImporter.describe(file) }.getOrNull()
            }
        }
}