package com.itzmrazotyalin.androwalp.di

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.itzmrazotyalin.androwalp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwalp.data.wallpaper.VideoImporter
import com.itzmrazotyalin.androwalp.data.wallpaper.WallpaperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AndroWalpApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {

    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }

    val videoImporter: VideoImporter by lazy { VideoImporter(application) }

    val wallpaperRepository: WallpaperRepository by lazy {
        WallpaperRepository(videoImporter, applicationScope)
    }
}

val CreationExtras.appContainer: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AndroWalpApplication)
        .container