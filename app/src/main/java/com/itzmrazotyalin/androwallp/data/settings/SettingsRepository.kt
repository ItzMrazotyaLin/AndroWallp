package com.itzmrazotyalin.androwallp.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.itzmrazotyalin.androwallp.domain.model.AppLanguage
import com.itzmrazotyalin.androwallp.domain.model.AppSettings
import com.itzmrazotyalin.androwallp.domain.model.ThemeMode
import com.itzmrazotyalin.androwallp.domain.model.WallpaperConfig
import com.itzmrazotyalin.androwallp.domain.model.WallpaperScaling
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore(
    name = "andro_wallp_settings",
)

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.preferencesStore

    val settings: Flow<AppSettings> = dataStore.safeData()
        .map { preferences ->
            AppSettings(
                resetOnUnlock = preferences[Keys.RESET_ON_UNLOCK] ?: true,
                muteHome = preferences[Keys.MUTE_HOME] ?: true,
                muteLock = preferences[Keys.MUTE_LOCK] ?: true,
                scalingMode = WallpaperScaling.fromStorageKey(preferences[Keys.SCALING_MODE]),
                themeMode = preferences[Keys.THEME_MODE]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.FOLLOW_SYSTEM,
                useDynamicColor = preferences[Keys.DYNAMIC_COLOR] ?: true,
                language = preferences[Keys.LANGUAGE]
                    ?.let { stored -> AppLanguage.entries.firstOrNull { it.name == stored } }
                    ?: AppLanguage.ENGLISH,
            )
        }
        .distinctUntilChanged()

    val wallpaperConfig: Flow<WallpaperConfig> = dataStore.safeData()
        .map { preferences ->
            WallpaperConfig(
                activeHomeWallpaperId = preferences[Keys.ACTIVE_HOME_WALLPAPER_ID]?.takeIf { it.isNotBlank() },
                activeLockWallpaperId = preferences[Keys.ACTIVE_LOCK_WALLPAPER_ID]?.takeIf { it.isNotBlank() },
                resetOnUnlock = preferences[Keys.RESET_ON_UNLOCK] ?: true,
                muteHome = preferences[Keys.MUTE_HOME] ?: true,
                muteLock = preferences[Keys.MUTE_LOCK] ?: true,
                scalingMode = WallpaperScaling.fromStorageKey(preferences[Keys.SCALING_MODE]),
            )
        }
        .distinctUntilChanged()

    val activeHomeWallpaperId: Flow<String?> = wallpaperConfig
        .map { config -> config.activeHomeWallpaperId }
        .distinctUntilChanged()

    suspend fun setActiveHomeWallpaperId(wallpaperId: String?) {
        dataStore.edit { preferences -> preferences.putId(Keys.ACTIVE_HOME_WALLPAPER_ID, wallpaperId) }
    }

    suspend fun setActiveLockWallpaperId(wallpaperId: String?) {
        dataStore.edit { preferences -> preferences.putId(Keys.ACTIVE_LOCK_WALLPAPER_ID, wallpaperId) }
    }

    suspend fun setActiveForBothScreens(wallpaperId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.ACTIVE_HOME_WALLPAPER_ID] = wallpaperId
            preferences[Keys.ACTIVE_LOCK_WALLPAPER_ID] = wallpaperId
        }
    }

    suspend fun clearActiveWallpaperIds() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.ACTIVE_HOME_WALLPAPER_ID)
            preferences.remove(Keys.ACTIVE_LOCK_WALLPAPER_ID)
        }
    }

    suspend fun setResetOnUnlock(enabled: Boolean) {
        dataStore.edit { it[Keys.RESET_ON_UNLOCK] = enabled }
    }

    suspend fun setMuteHome(enabled: Boolean) {
        dataStore.edit { it[Keys.MUTE_HOME] = enabled }
    }

    suspend fun setMuteLock(enabled: Boolean) {
        dataStore.edit { it[Keys.MUTE_LOCK] = enabled }
    }

    suspend fun setScalingMode(scalingMode: WallpaperScaling) {
        dataStore.edit { it[Keys.SCALING_MODE] = scalingMode.storageKey }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    internal fun store(): DataStore<Preferences> = dataStore

    private fun MutablePreferences.putId(
        key: Preferences.Key<String>,
        value: String?,
    ) {
        if (value == null) remove(key) else set(key, value)
    }

    private fun DataStore<Preferences>.safeData(): Flow<Preferences> = data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    internal object Keys {

        val ACTIVE_HOME_WALLPAPER_ID = stringPreferencesKey("active_home_wallpaper_id")
        val ACTIVE_LOCK_WALLPAPER_ID = stringPreferencesKey("active_lock_wallpaper_id")
        val RESET_ON_UNLOCK = booleanPreferencesKey("reset_playback_on_unlock")
        val MUTE_HOME = booleanPreferencesKey("mute_home_audio")
        val MUTE_LOCK = booleanPreferencesKey("mute_lock_audio")
        val SCALING_MODE = stringPreferencesKey("scaling_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val LANGUAGE = stringPreferencesKey("app_language")
        val WALLPAPERS = stringPreferencesKey("wallpapers")
    }
}