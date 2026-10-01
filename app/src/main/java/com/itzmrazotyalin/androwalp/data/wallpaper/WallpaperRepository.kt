package com.itzmrazotyalin.androwalp.data.wallpaper

import android.net.Uri
import com.itzmrazotyalin.androwalp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwalp.data.settings.WallpaperRecordStore
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper
import com.itzmrazotyalin.androwalp.domain.model.WallpaperTarget
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
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
            .map { record ->
                val target = WallpaperTarget.fromPaths(
                    homePath = config.homeWallpaperPath,
                    lockPath = config.lockWallpaperPath,
                    filePath = record.filePath,
                )
                record.copy(isActive = target != WallpaperTarget.NONE, target = target)
            }
            .sortedByDescending { record -> record.importedAt }
    }.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
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

    suspend fun setActive(wallpaperId: String, target: WallpaperTarget) {
        val records = recordStore.read()
        val wallpaper = records.firstOrNull { record -> record.id == wallpaperId } ?: return
        val homePath = when (target) {
            WallpaperTarget.HOME, WallpaperTarget.BOTH -> wallpaper.filePath
            else -> settingsRepository.wallpaperConfig.first().homeWallpaperPath
        }
        val lockPath = when (target) {
            WallpaperTarget.LOCK, WallpaperTarget.BOTH -> wallpaper.filePath
            else -> settingsRepository.wallpaperConfig.first().lockWallpaperPath
        }
        recordStore.write(
            records.map { record ->
                val isActive = when (target) {
                    WallpaperTarget.HOME, WallpaperTarget.LOCK -> record.id == wallpaperId
                    WallpaperTarget.BOTH, WallpaperTarget.NONE -> false
                }
                record.copy(isActive = isActive)
            },
        )
        settingsRepository.setHomeWallpaperPath(homePath)
        settingsRepository.setLockWallpaperPath(lockPath)
    }

    suspend fun deactivate() {
        recordStore.write(recordStore.read().map { record -> record.copy(isActive = false) })
        settingsRepository.setHomeWallpaperPath(null)
        settingsRepository.setLockWallpaperPath(null)
    }

    suspend fun delete(wallpaperId: String) {
        val records = recordStore.read()
        val target = records.firstOrNull { record -> record.id == wallpaperId } ?: return
        withContext(Dispatchers.IO) { videoImporter.delete(target) }
        recordStore.write(records.filterNot { record -> record.id == wallpaperId })
        val config = settingsRepository.wallpaperConfig.first()
        if (config.homeWallpaperPath == target.filePath) {
            settingsRepository.setHomeWallpaperPath(null)
        }
        if (config.lockWallpaperPath == target.filePath) {
            settingsRepository.setLockWallpaperPath(null)
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