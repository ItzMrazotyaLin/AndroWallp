package com.itzmrazotyalin.androwallp.data.wallpaper

import android.net.Uri
import com.itzmrazotyalin.androwallp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwallp.data.settings.WallpaperRecordStore
import com.itzmrazotyalin.androwallp.domain.model.Wallpaper
import com.itzmrazotyalin.androwallp.domain.model.WallpaperStatus
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WallpaperRepository(
    private val videoImporter: VideoImporter,
    private val settingsRepository: SettingsRepository,
    private val recordStore: WallpaperRecordStore,
    applicationScope: CoroutineScope,
) {

    val wallpapers: StateFlow<List<Wallpaper>> = combine(
        recordStore.wallpapers,
        settingsRepository.wallpaperConfig,
    ) { records, config ->
        records
            .filter { record -> File(record.filePath).exists() }
            .sortedByDescending { record -> record.importedAt }
    }.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    val statuses: StateFlow<Map<String, WallpaperStatus>> = combine(
        recordStore.wallpapers,
        settingsRepository.wallpaperConfig,
    ) { records, config ->
        records.associate { record ->
            record.id to WallpaperStatus.resolve(
                wallpaperId = record.id,
                homeId = config.activeHomeWallpaperId,
                lockId = config.activeLockWallpaperId,
            )
        }
    }.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap(),
    )

    fun wallpaperById(wallpaperId: String): Flow<Wallpaper?> = wallpapers
        .map { list -> list.firstOrNull { wallpaper -> wallpaper.id == wallpaperId } }

    init {
        applicationScope.launch { adoptOrphanedFiles() }
    }

    suspend fun import(uri: Uri, title: String): Wallpaper {
        val wallpaper = videoImporter.import(uri, title)
        val existing = recordStore.read()
        recordStore.write(listOf(wallpaper) + existing)
        return wallpaper
    }

    suspend fun update(wallpaperId: String, title: String) {
        val records = recordStore.read()
        recordStore.write(
            records.map { record ->
                if (record.id != wallpaperId) {
                    record
                } else {
                    record.copy(title = title.trim().ifBlank { record.title })
                }
            },
        )
    }

    suspend fun setActiveForBothScreens(wallpaperId: String) {
        val record = recordStore.read().firstOrNull { it.id == wallpaperId } ?: return
        settingsRepository.setActiveForBothScreens(record.id)
    }

    suspend fun setActiveHome(wallpaperId: String) {
        settingsRepository.setActiveHomeWallpaperId(wallpaperId)
    }

    suspend fun setActiveLock(wallpaperId: String) {
        settingsRepository.setActiveLockWallpaperId(wallpaperId)
    }

    suspend fun clearActive() {
        settingsRepository.clearActiveWallpaperIds()
    }

    suspend fun delete(wallpaperId: String) {
        val records = recordStore.read()
        val target = records.firstOrNull { record -> record.id == wallpaperId } ?: return
        withContext(Dispatchers.IO) { videoImporter.delete(target) }
        recordStore.write(records.filterNot { record -> record.id == wallpaperId })
        val config = settingsRepository.wallpaperConfig.first()
        if (config.activeHomeWallpaperId == wallpaperId) {
            settingsRepository.setActiveHomeWallpaperId(null)
        }
        if (config.activeLockWallpaperId == wallpaperId) {
            settingsRepository.setActiveLockWallpaperId(null)
        }
    }

    private suspend fun adoptOrphanedFiles() {
        val stored = recordStore.read()
        val healthy = stored.filter { record -> File(record.filePath).exists() }
        if (healthy.size != stored.size) {
            recordStore.write(healthy)
        }
        if (healthy.isNotEmpty()) return
        val adopted = withContext(Dispatchers.IO) {
            videoImporter.videoFiles().mapNotNull { file ->
                runCatching {
                    videoImporter.describe(file, videoImporter.thumbnailFileFor(file.nameWithoutExtension))
                }.getOrNull()
            }
        }
        if (adopted.isNotEmpty()) {
            recordStore.write(adopted)
        }
    }
}