package com.itzmrazotyalin.androwallp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.itzmrazotyalin.androwallp.domain.model.Wallpaper
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONException

class WallpaperRecordStore(context: Context) {

    private val settingsRepository = SettingsRepository(context)

    private val dataStore = settingsRepository.store()

    val wallpapers: Flow<List<Wallpaper>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            preferences[SettingsRepository.Keys.WALLPAPERS]
                ?.let { raw -> decodeSafely(raw) }
                .orEmpty()
        }

    suspend fun read(): List<Wallpaper> = wallpapers.first()

    suspend fun write(wallpapers: List<Wallpaper>) {
        dataStore.edit { preferences ->
            if (wallpapers.isEmpty()) {
                preferences.remove(SettingsRepository.Keys.WALLPAPERS)
            } else {
                preferences[SettingsRepository.Keys.WALLPAPERS] = WallpaperJson.encode(wallpapers)
            }
        }
    }

    private fun decodeSafely(raw: String): List<Wallpaper> = try {
        WallpaperJson.decode(raw)
    } catch (exception: JSONException) {
        emptyList()
    } catch (exception: IllegalArgumentException) {
        emptyList()
    }
}