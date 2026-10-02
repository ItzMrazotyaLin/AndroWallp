package com.itzmrazotyalin.androwallp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoThumbnail(
    filePath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val image by produceState<ImageBitmap?>(initialValue = null, filePath) {
        value = filePath?.let { withContext(Dispatchers.IO) { loadThumbnail(it) } }
    }
    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ThumbnailPlaceholder()
        }
        overlay()
    }
}

@Composable
private fun ThumbnailPlaceholder() {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primaryContainer,
                        colorScheme.tertiaryContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = colorScheme.onPrimaryContainer,
            modifier = Modifier.size(36.dp),
        )
    }
}

private fun loadThumbnail(path: String): ImageBitmap? {
    thumbnailCache[path]?.let { return it }
    val file = File(path)
    if (!file.exists()) return null
    val bitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return null
    val image = bitmap.asImageBitmap()
    if (thumbnailCache.size >= MAX_CACHED_THUMBNAILS) {
        thumbnailCache.keys.firstOrNull()?.let(thumbnailCache::remove)
    }
    thumbnailCache[path] = image
    return image
}

private val thumbnailCache = mutableMapOf<String, ImageBitmap>()
private const val MAX_CACHED_THUMBNAILS = 32