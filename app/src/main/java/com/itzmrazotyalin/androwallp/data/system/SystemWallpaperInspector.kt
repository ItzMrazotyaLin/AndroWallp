package com.itzmrazotyalin.androwallp.data.system

import android.app.WallpaperManager
import android.content.Context
import android.util.Log
import com.itzmrazotyalin.androwallp.service.VideoLiveWallpaperService

class SystemWallpaperInspector(context: Context) {

    private val appContext = context.applicationContext

    fun isOurLiveWallpaperActive(): Boolean = runCatching {
        val info = WallpaperManager.getInstance(appContext).wallpaperInfo
        if (info == null) {
            Log.i(TAG, "No live wallpaper set by the system")
            return@runCatching false
        }
        val matches = info.packageName == appContext.packageName &&
            info.serviceName == VideoLiveWallpaperService::class.java.name
        Log.i(TAG, "Live wallpaper owner=${info.packageName}/${info.serviceName} matches=$matches")
        matches
    }.getOrElse { throwable ->
        Log.w(TAG, "Unable to read the current wallpaper", throwable)
        false
    }

    private companion object {

        const val TAG = "WallpaperInspector"
    }
}