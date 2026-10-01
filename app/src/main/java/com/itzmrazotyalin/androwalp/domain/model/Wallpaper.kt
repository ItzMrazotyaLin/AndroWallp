package com.itzmrazotyalin.androwalp.domain.model

import java.util.UUID

enum class WallpaperTarget {
    NONE,
    HOME,
    LOCK,
    BOTH,
    ;

    companion object {

        fun fromPaths(homePath: String?, lockPath: String?, filePath: String): WallpaperTarget {
            val onHome = homePath != null && homePath == filePath
            val onLock = lockPath != null && lockPath == filePath
            return when {
                onHome && onLock -> BOTH
                onHome -> HOME
                onLock -> LOCK
                else -> NONE
            }
        }
    }
}

enum class WallpaperScaling(val storageKey: String) {
    CENTER_CROP(storageKey = "CENTER_CROP"),
    FIT_XY(storageKey = "FIT_XY"),
    STRETCH(storageKey = "STRETCH"),
    ;

    companion object {

        fun fromStorageKey(storageKey: String?): WallpaperScaling =
            entries.firstOrNull { it.storageKey == storageKey } ?: CENTER_CROP
    }
}

data class VideoMetadata(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val previewPath: String?,
)

data class Wallpaper(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val filePath: String,
    val thumbnailPath: String?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String?,
    val isActive: Boolean = false,
    val target: WallpaperTarget = WallpaperTarget.NONE,
    val importedAt: Long = System.currentTimeMillis(),
)