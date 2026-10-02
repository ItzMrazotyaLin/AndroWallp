package com.itzmrazotyalin.androwallp.di

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.itzmrazotyalin.androwallp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwallp.data.settings.WallpaperRecordStore
import com.itzmrazotyalin.androwallp.data.system.SystemWallpaperInspector
import com.itzmrazotyalin.androwallp.data.wallpaper.VideoImporter
import com.itzmrazotyalin.androwallp.data.wallpaper.WallpaperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AndroWallpApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {

    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }

    val systemWallpaperInspector: SystemWallpaperInspector by lazy {
        SystemWallpaperInspector(application)
    }

    val wallpaperRecordStore: WallpaperRecordStore by lazy { WallpaperRecordStore(application) }

    val videoImporter: VideoImporter by lazy { VideoImporter(application) }

    val wallpaperRepository: WallpaperRepository by lazy {
        WallpaperRepository(
            videoImporter = videoImporter,
            settingsRepository = settingsRepository,
            recordStore = wallpaperRecordStore,
            applicationScope = applicationScope,
        )
    }
}

val CreationExtras.appContainer: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AndroWallpApplication)
        .container