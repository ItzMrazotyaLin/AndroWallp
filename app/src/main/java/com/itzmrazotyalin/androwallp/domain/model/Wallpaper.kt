package com.itzmrazotyalin.androwallp.domain.model

import java.util.UUID

enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH,
}

enum class WallpaperStatus {
    NONE,
    HOME,
    LOCK,
    BOTH,
    ;

    companion object {

        fun resolve(wallpaperId: String, homeId: String?, lockId: String?): WallpaperStatus {
            val onHome = homeId != null && homeId == wallpaperId
            val onLock = lockId != null && lockId == wallpaperId
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
    val importedAt: Long = System.currentTimeMillis(),
)