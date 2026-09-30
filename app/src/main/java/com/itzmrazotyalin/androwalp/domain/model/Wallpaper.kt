package com.itzmrazotyalin.androwalp.domain.model

import java.util.UUID

enum class WallpaperScaling {
    CENTER_CROP,
    FIT_SCREEN,
    STRETCH,
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
    val scaling: WallpaperScaling,
    val isActive: Boolean = false,
    val importedAt: Long = System.currentTimeMillis(),
)