package com.itzmrazotyalin.androwalp.data.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.itzmrazotyalin.androwalp.domain.model.VideoMetadata
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoImporter(context: Context) {

    private val appContext = context.applicationContext

    private val libraryDir: File
        get() = File(appContext.filesDir, LIBRARY_DIR_NAME).apply { mkdirs() }

    private val thumbnailDir: File
        get() = File(appContext.filesDir, THUMBNAIL_DIR_NAME).apply { mkdirs() }

    private val previewDir: File
        get() = File(appContext.cacheDir, PREVIEW_DIR_NAME).apply { mkdirs() }

    suspend fun inspect(uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, uri)
            val track = readTrack(retriever)
            val preview = writeThumbnail(
                bitmap = extractFrame(retriever),
                target = File(previewDir, "preview-${System.currentTimeMillis()}.jpg"),
            )
            VideoMetadata(
                uri = uri.toString(),
                displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: FALLBACK_NAME,
                mimeType = appContext.contentResolver.getType(uri),
                durationMs = track.durationMs,
                width = track.width,
                height = track.height,
                sizeBytes = querySize(uri),
                previewPath = preview?.absolutePath,
            )
        } catch (throwable: RuntimeException) {
            throw IOException("Unable to read video metadata", throwable)
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun import(uri: Uri, title: String): Wallpaper = withContext(Dispatchers.IO) {
        val metadata = inspect(uri)
        val id = UUID.randomUUID().toString()
        val target = File(libraryDir, "$id.${extensionForMimeType(metadata.mimeType)}")
        copyToInternalStorage(uri, target)
        val thumbnailPath = extractThumbnail(target)?.let { bitmap ->
            writeThumbnail(
                bitmap = bitmap,
                target = File(thumbnailDir, "$id.jpg"),
            )?.absolutePath
        }
        Wallpaper(
            id = id,
            title = title.trim().ifBlank { defaultTitle(metadata.displayName) },
            filePath = target.absolutePath,
            thumbnailPath = thumbnailPath,
            durationMs = metadata.durationMs,
            width = metadata.width,
            height = metadata.height,
            sizeBytes = target.length(),
            mimeType = metadata.mimeType,
        )
    }

    fun defaultTitle(displayName: String): String =
        displayName.substringBeforeLast('.').trim().ifBlank { FALLBACK_TITLE }

    fun describe(file: File, thumbnailFile: File?): Wallpaper {
        val retriever = MediaMetadataRetriever()
        val track = try {
            retriever.setDataSource(file.absolutePath)
            readTrack(retriever)
        } catch (throwable: RuntimeException) {
            Log.w(TAG, "Unable to read metadata of ${file.name}", throwable)
            VideoTrack(durationMs = 0L, width = 0, height = 0)
        } finally {
            runCatching { retriever.release() }
        }
        return Wallpaper(
            id = file.nameWithoutExtension,
            title = file.nameWithoutExtension,
            filePath = file.absolutePath,
            thumbnailPath = thumbnailFile?.takeIf { it.exists() }?.absolutePath,
            durationMs = track.durationMs,
            width = track.width,
            height = track.height,
            sizeBytes = file.length(),
            mimeType = mimeTypeForExtension(file.extension),
        )
    }

    fun videoFiles(): List<File> = libraryDir
        .listFiles { file -> file.isFile && isSupportedExtension(file.extension) }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

    fun thumbnailFileFor(id: String): File = File(thumbnailDir, "$id.jpg")

    fun delete(wallpaper: Wallpaper) {
        File(wallpaper.filePath).delete()
        wallpaper.thumbnailPath?.let { File(it).delete() }
        thumbnailFileFor(wallpaper.id).delete()
    }

    private fun copyToInternalStorage(uri: Uri, target: File) {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IOException("Content stream for $uri is unavailable")
        var copied = false
        try {
            input.use { source ->
                FileOutputStream(target).use { output -> source.copyTo(output) }
            }
            copied = true
        } finally {
            if (!copied) {
                target.delete()
            }
        }
    }

    private fun extractThumbnail(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            extractFrame(retriever)
        } catch (throwable: RuntimeException) {
            Log.w(TAG, "Unable to read thumbnail of ${file.name}", throwable)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractFrame(retriever: MediaMetadataRetriever): Bitmap? {
        val scaled = runCatching {
            retriever.getScaledFrameAtTime(
                THUMBNAIL_TIME_US,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                THUMBNAIL_WIDTH,
                THUMBNAIL_HEIGHT,
            )
        }.getOrNull()
        if (scaled != null) return scaled
        val full = runCatching {
            retriever.getFrameAtTime(THUMBNAIL_TIME_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }.getOrNull() ?: return null
        val width = full.width.coerceAtMost(THUMBNAIL_WIDTH)
        val height = (full.height * (width.toFloat() / full.width)).toInt().coerceAtLeast(1)
        return runCatching { Bitmap.createScaledBitmap(full, width, height, true) }
            .getOrNull()
            .also { scaledBitmap -> if (scaledBitmap != null && scaledBitmap !== full) full.recycle() }
    }

    private fun writeThumbnail(bitmap: Bitmap?, target: File): File? {
        if (bitmap == null) return null
        return try {
            FileOutputStream(target).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            target
        } catch (throwable: IOException) {
            Log.w(TAG, "Unable to write thumbnail to ${target.name}", throwable)
            target.delete()
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun readTrack(retriever: MediaMetadataRetriever): VideoTrack = VideoTrack(
        durationMs = retriever.readLong(MediaMetadataRetriever.METADATA_KEY_DURATION),
        width = retriever.readInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),
        height = retriever.readInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT),
    )

    private fun MediaMetadataRetriever.readLong(key: Int): Long =
        extractMetadata(key)?.toLongOrNull() ?: 0L

    private fun MediaMetadataRetriever.readInt(key: Int): Int =
        extractMetadata(key)?.toIntOrNull() ?: 0

    private fun querySize(uri: Uri): Long =
        query(uri, OpenableColumns.SIZE)?.toLongOrNull() ?: 0L

    private fun queryDisplayName(uri: Uri): String? {
        val direct = query(uri, MediaStore.MediaColumns.DISPLAY_NAME)
        if (direct != null && !isSyntheticName(direct)) return direct
        val resolved = queryMediaStoreDisplayName(uri)
        if (resolved != null) return resolved
        return direct ?: uri.lastPathSegment
    }

    private fun queryMediaStoreDisplayName(uri: Uri): String? {
        if (uri.authority?.contains(AUTHORITY_MEDIA) != true) return null
        val mediaId = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: return null
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        return runCatching {
            appContext.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.MediaColumns._ID} = ?",
                arrayOf(mediaId),
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (index < 0 || cursor.isNull(index)) null else cursor.getString(index)
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun isSyntheticName(name: String): Boolean = SYNTHETIC_NAME.matches(name)

    private fun query(uri: Uri, column: String): String? = runCatching {
        appContext.contentResolver.query(
            uri,
            arrayOf(column),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(column)
            if (index < 0 || cursor.isNull(index)) null else cursor.getString(index)
        }
    }.getOrNull()

    private fun extensionForMimeType(mimeType: String?): String {
        val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return extension?.takeIf { isSupportedExtension(it) } ?: DEFAULT_EXTENSION
    }

    private fun mimeTypeForExtension(extension: String): String? =
        extension.takeIf { isSupportedExtension(it) }
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase()) }

    private fun isSupportedExtension(extension: String): Boolean =
        extension.lowercase() in SUPPORTED_EXTENSIONS

    private data class VideoTrack(
        val durationMs: Long,
        val width: Int,
        val height: Int,
    )

    private companion object {

        const val TAG = "VideoImporter"
        const val AUTHORITY_MEDIA = "media"
        val SYNTHETIC_NAME = Regex("^\\d{1,9}\\.[A-Za-z0-9]{1,5}$")
        const val LIBRARY_DIR_NAME = "wallpapers"
        const val THUMBNAIL_DIR_NAME = "thumbnails"
        const val PREVIEW_DIR_NAME = "preview"
        const val DEFAULT_EXTENSION = "mp4"
        const val FALLBACK_NAME = "wallpaper.mp4"
        const val FALLBACK_TITLE = "Wallpaper"
        const val THUMBNAIL_TIME_US = 1_000_000L
        const val THUMBNAIL_WIDTH = 640
        const val THUMBNAIL_HEIGHT = 360
        const val JPEG_QUALITY = 85
        val SUPPORTED_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov", "m4v")
    }
}