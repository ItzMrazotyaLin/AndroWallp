package com.itzmrazotyalin.androwalp.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.itzmrazotyalin.androwalp.domain.model.AppLanguage
import com.itzmrazotyalin.androwalp.domain.model.AppSettings
import com.itzmrazotyalin.androwalp.domain.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "andro_walp_settings",
)

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppSettings(
                resetPlaybackOnUnlock = preferences[Keys.RESET_ON_UNLOCK] ?: true,
                muteAudioOnHomeScreen = preferences[Keys.MUTE_AUDIO] ?: false,
                themeMode = preferences[Keys.THEME_MODE]
                    ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                    ?: ThemeMode.FOLLOW_SYSTEM,
                useDynamicColor = preferences[Keys.DYNAMIC_COLOR] ?: true,
                language = preferences[Keys.LANGUAGE]
                    ?.let { stored -> AppLanguage.entries.firstOrNull { it.name == stored } }
                    ?: AppLanguage.ENGLISH,
            )
        }

    suspend fun setResetPlaybackOnUnlock(enabled: Boolean) {
        dataStore.edit { it[Keys.RESET_ON_UNLOCK] = enabled }
    }

    suspend fun setMuteAudioOnHomeScreen(enabled: Boolean) {
        dataStore.edit { it[Keys.MUTE_AUDIO] = enabled }
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

    private object Keys {
        val RESET_ON_UNLOCK = booleanPreferencesKey("reset_playback_on_unlock")
        val MUTE_AUDIO = booleanPreferencesKey("mute_audio_on_home_screen")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val LANGUAGE = stringPreferencesKey("app_language")
    }
}