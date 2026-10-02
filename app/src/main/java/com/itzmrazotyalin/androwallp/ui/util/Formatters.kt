package com.itzmrazotyalin.androwallp.ui.util

import java.util.Locale
import kotlin.math.abs

fun aspectRatioLabel(width: Int, height: Int): String? {
    if (width <= 0 || height <= 0) return null
    val divisor = greatestCommonDivisor(width, height)
    val ratioWidth = width / divisor
    val ratioHeight = height / divisor
    return if (ratioWidth <= 32 && ratioHeight <= 32) {
        "$ratioWidth:$ratioHeight"
    } else {
        String.format(Locale.US, "%.1f", width.toFloat() / height.toFloat())
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = abs(durationMs) / 1_000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3_600L
    return when {
        hours > 0L -> String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun formatFileSize(sizeBytes: Long): String {
    if (sizeBytes <= 0L) return "0 MB"
    val megabytes = sizeBytes / (1024f * 1024f)
    val kilobytes = sizeBytes / 1024f
    return when {
        megabytes >= 1f -> String.format(Locale.US, "%.1f MB", megabytes)
        else -> String.format(Locale.US, "%.0f KB", kilobytes)
    }
}

fun formatResolution(width: Int, height: Int): String? =
    if (width > 0 && height > 0) "$width × $height" else null

private fun greatestCommonDivisor(first: Int, second: Int): Int {
    var dividend = first
    var divisor = second
    while (divisor != 0) {
        val remainder = dividend % divisor
        dividend = divisor
        divisor = remainder
    }
    return dividend
}