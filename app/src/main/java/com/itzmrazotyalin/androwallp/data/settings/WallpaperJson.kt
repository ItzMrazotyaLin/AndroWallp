package com.itzmrazotyalin.androwallp.data.settings

import com.itzmrazotyalin.androwallp.domain.model.Wallpaper
import org.json.JSONArray
import org.json.JSONObject

internal object WallpaperJson {

    fun encode(wallpapers: List<Wallpaper>): String {
        val array = JSONArray()
        wallpapers.forEach { wallpaper ->
            array.put(
                JSONObject().apply {
                    put(KEY_ID, wallpaper.id)
                    put(KEY_TITLE, wallpaper.title)
                    put(KEY_FILE_PATH, wallpaper.filePath)
                    put(KEY_THUMBNAIL_PATH, wallpaper.thumbnailPath)
                    put(KEY_DURATION_MS, wallpaper.durationMs)
                    put(KEY_WIDTH, wallpaper.width)
                    put(KEY_HEIGHT, wallpaper.height)
                    put(KEY_SIZE_BYTES, wallpaper.sizeBytes)
                    put(KEY_MIME_TYPE, wallpaper.mimeType ?: JSONObject.NULL)
                    put(KEY_IMPORTED_AT, wallpaper.importedAt)
                },
            )
        }
        return array.toString()
    }

    fun decode(raw: String): List<Wallpaper> {
        if (raw.isBlank()) return emptyList()
        val array = JSONArray(raw)
        return buildList(capacity = array.length()) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString(KEY_ID).takeIf { it.isNotBlank() } ?: continue
                val filePath = item.optString(KEY_FILE_PATH).takeIf { it.isNotBlank() } ?: continue
                add(
                    Wallpaper(
                        id = id,
                        title = item.optString(KEY_TITLE, DEFAULT_TITLE),
                        filePath = filePath,
                        thumbnailPath = item.optString(KEY_THUMBNAIL_PATH).takeIf { it.isNotBlank() },
                        durationMs = item.optLong(KEY_DURATION_MS),
                        width = item.optInt(KEY_WIDTH),
                        height = item.optInt(KEY_HEIGHT),
                        sizeBytes = item.optLong(KEY_SIZE_BYTES),
                        mimeType = item.optString(KEY_MIME_TYPE).takeIf { it.isNotBlank() && it != "null" },
                        importedAt = item.optLong(KEY_IMPORTED_AT),
                    ),
                )
            }
        }
    }

    private const val KEY_ID = "id"
    private const val KEY_TITLE = "title"
    private const val KEY_FILE_PATH = "filePath"
    private const val KEY_THUMBNAIL_PATH = "thumbnailPath"
    private const val KEY_DURATION_MS = "durationMs"
    private const val KEY_WIDTH = "width"
    private const val KEY_HEIGHT = "height"
    private const val KEY_SIZE_BYTES = "sizeBytes"
    private const val KEY_MIME_TYPE = "mimeType"
    private const val KEY_IMPORTED_AT = "importedAt"
    private const val DEFAULT_TITLE = "Wallpaper"
}