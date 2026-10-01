package com.itzmrazotyalin.androwalp.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.itzmrazotyalin.androwalp.domain.model.AppLanguage
import com.itzmrazotyalin.androwalp.domain.model.AppSettings
import com.itzmrazotyalin.androwalp.domain.model.ThemeMode
import com.itzmrazotyalin.androwalp.domain.model.WallpaperConfig
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore(
    name = "andro_walp_settings",
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
                homeWallpaperPath = preferences[Keys.HOME_WALLPAPER_PATH]?.takeIf { it.isNotBlank() },
                lockWallpaperPath = preferences[Keys.LOCK_WALLPAPER_PATH]?.takeIf { it.isNotBlank() },
                resetOnUnlock = preferences[Keys.RESET_ON_UNLOCK] ?: true,
                muteHome = preferences[Keys.MUTE_HOME] ?: true,
                muteLock = preferences[Keys.MUTE_LOCK] ?: true,
                scalingMode = WallpaperScaling.fromStorageKey(preferences[Keys.SCALING_MODE]),
            )
        }
        .distinctUntilChanged()

    suspend fun setHomeWallpaperPath(path: String?) {
        dataStore.edit { preferences -> preferences.putPath(Keys.HOME_WALLPAPER_PATH, path) }
    }

    suspend fun setLockWallpaperPath(path: String?) {
        dataStore.edit { preferences -> preferences.putPath(Keys.LOCK_WALLPAPER_PATH, path) }
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

    private fun MutablePreferences.putPath(
        key: Preferences.Key<String>,
        path: String?,
    ) {
        if (path == null) remove(key) else set(key, path)
    }

    private fun DataStore<Preferences>.safeData(): Flow<Preferences> = data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    internal object Keys {

        val HOME_WALLPAPER_PATH = stringPreferencesKey("home_wallpaper_path")
        val LOCK_WALLPAPER_PATH = stringPreferencesKey("lock_wallpaper_path")
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