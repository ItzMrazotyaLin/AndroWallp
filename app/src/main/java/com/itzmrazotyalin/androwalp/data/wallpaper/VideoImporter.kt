package com.itzmrazotyalin.androwalp.data.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.itzmrazotyalin.androwalp.domain.model.VideoMetadata
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling
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

    private val previewDir: File
        get() = File(appContext.cacheDir, PREVIEW_DIR_NAME).apply { mkdirs() }

    suspend fun inspect(uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, uri)
            val track = readTrack(retriever)
            val preview = writeThumbnail(
                bitmap = retriever.getScaledFrameAtTime(
                    THUMBNAIL_TIME_US,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    THUMBNAIL_WIDTH,
                    THUMBNAIL_HEIGHT,
                ),
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
        } finally {
            retriever.release()
        }
    }

    suspend fun import(uri: Uri, scaling: WallpaperScaling): Wallpaper =
        withContext(Dispatchers.IO) {
            val metadata = inspect(uri)
            val extension = extensionForMimeType(metadata.mimeType)
            val target = File(libraryDir, "${UUID.randomUUID()}.$extension")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: throw IOException("Unable to open $uri")

            val thumbnailPath = extractThumbnail(target)?.let { bitmap ->
                writeThumbnail(
                    bitmap = bitmap,
                    target = File(libraryDir, "${target.nameWithoutExtension}.jpg"),
                )?.absolutePath
            }

            Wallpaper(
                title = metadata.displayName.substringBeforeLast('.'),
                filePath = target.absolutePath,
                thumbnailPath = thumbnailPath,
                durationMs = metadata.durationMs,
                width = metadata.width,
                height = metadata.height,
                sizeBytes = target.length(),
                mimeType = metadata.mimeType,
                scaling = scaling,
            )
        }

    fun describe(file: File): Wallpaper {
        val retriever = MediaMetadataRetriever()
        val track = try {
            retriever.setDataSource(file.absolutePath)
            readTrack(retriever)
        } finally {
            retriever.release()
        }
        val thumbnailPath = extractThumbnail(file)?.let { bitmap ->
            writeThumbnail(
                bitmap = bitmap,
                target = File(libraryDir, "${file.nameWithoutExtension}.jpg"),
            )?.absolutePath
        }
        return Wallpaper(
            title = file.nameWithoutExtension,
            filePath = file.absolutePath,
            thumbnailPath = thumbnailPath,
            durationMs = track.durationMs,
            width = track.width,
            height = track.height,
            sizeBytes = file.length(),
            mimeType = mimeTypeForExtension(file.extension),
            scaling = WallpaperScaling.CENTER_CROP,
        )
    }

    fun videoFiles(): List<File> = libraryDir
        .listFiles { file -> file.isFile && isSupportedExtension(file.extension) }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

    fun delete(wallpaper: Wallpaper) {
        File(wallpaper.filePath).delete()
        wallpaper.thumbnailPath?.let { File(it).delete() }
    }

    private fun extractThumbnail(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.getScaledFrameAtTime(
                THUMBNAIL_TIME_US,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                THUMBNAIL_WIDTH,
                THUMBNAIL_HEIGHT,
            )
        } catch (throwable: RuntimeException) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun writeThumbnail(bitmap: Bitmap?, target: File): File? {
        if (bitmap == null) return null
        return try {
            FileOutputStream(target).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            target
        } catch (throwable: IOException) {
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

    private fun queryDisplayName(uri: Uri): String? = query(uri, OpenableColumns.DISPLAY_NAME)

    private fun querySize(uri: Uri): Long =
        query(uri, OpenableColumns.SIZE)?.toLongOrNull() ?: 0L

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
        const val LIBRARY_DIR_NAME = "wallpapers"
        const val PREVIEW_DIR_NAME = "preview"
        const val DEFAULT_EXTENSION = "mp4"
        const val FALLBACK_NAME = "wallpaper.mp4"
        const val THUMBNAIL_TIME_US = 1_000_000L
        const val THUMBNAIL_WIDTH = 640
        const val THUMBNAIL_HEIGHT = 360
        const val JPEG_QUALITY = 85
        val SUPPORTED_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov", "m4v")
    }
}