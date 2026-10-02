package com.itzmrazotyalin.androwallp.service

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.itzmrazotyalin.androwallp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwallp.data.settings.WallpaperRecordStore
import com.itzmrazotyalin.androwallp.domain.model.Wallpaper
import com.itzmrazotyalin.androwallp.domain.model.WallpaperConfig
import com.itzmrazotyalin.androwallp.domain.model.WallpaperScaling
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(UnstableApi::class)
class VideoLiveWallpaperService : WallpaperService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    private val recordStore: WallpaperRecordStore by lazy { WallpaperRecordStore(this) }

    private var wallpapersById: Map<String, Wallpaper> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        recordStore.wallpapers
            .onEach { records -> wallpapersById = records.associateBy { record -> record.id } }
            .launchIn(serviceScope)
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun resolveFile(wallpaperId: String?): File? {
        val id = wallpaperId?.takeIf { it.isNotBlank() } ?: return null
        val path = wallpapersById[id]?.filePath ?: return null
        val file = File(path)
        if (!file.exists() || !file.canRead() || file.length() == 0L) {
            Log.w(TAG, "Wallpaper file unusable for id=$id")
            return null
        }
        return file
    }

    /**
     * One engine instance is created per display target by the system, so every engine owns its
     * own player and surface lifecycle and only ever plays the clip assigned to its own target.
     */
    private inner class VideoEngine : Engine(), SurfaceHolder.Callback {

        private var exoPlayer: ExoPlayer? = null
        private var surfaceHolder: SurfaceHolder? = null
        private var configJob: Job? = null

        private var target: EngineTarget = EngineTarget.HOME
        private var preparedId: String? = null
        private var scalingMode = WallpaperScaling.CENTER_CROP
        private var isVisible = true
        private var hasVisibilityState = false
        private var config = WallpaperConfig(
            activeHomeWallpaperId = null,
            activeLockWallpaperId = null,
            resetOnUnlock = true,
            muteHome = true,
            muteLock = true,
            scalingMode = WallpaperScaling.CENTER_CROP,
        )

        private val playerListener = object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback failed for $preparedId", error)
                guarded("onPlayerError") { exoPlayer?.playWhenReady = false }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            guarded("onCreate") {
                target = resolveTarget()
                surfaceHolder = holder
                holder.addCallback(this@VideoEngine)
                configJob = settingsRepository.wallpaperConfig
                    .onEach { updated ->
                        config = updated
                        scalingMode = updated.scalingMode
                        applyPlayback()
                    }
                    .launchIn(serviceScope)
            }
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            guarded("surfaceCreated") {
                surfaceHolder = holder
                setTouchEventsEnabled(false)
                applyPlayback()
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            guarded("surfaceChanged") { }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            guarded("surfaceDestroyed") { releasePlayer() }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            guarded("onVisibilityChanged") {
                hasVisibilityState = true
                isVisible = visible
                val player = exoPlayer ?: return@guarded
                if (visible) {
                    applyPlayback()
                    if (config.resetOnUnlock) {
                        player.seekTo(0L)
                    }
                    player.playWhenReady = true
                } else {
                    player.playWhenReady = false
                }
            }
        }

        override fun onWallpaperFlagsChanged(flags: Int) {
            super.onWallpaperFlagsChanged(flags)
            guarded("onWallpaperFlagsChanged") {
                val updated = resolveTarget()
                Log.i(TAG, "engine target=$updated flags=$flags")
                if (updated != target) {
                    target = updated
                    preparedId = null
                    releasePlayer()
                }
                applyPlayback()
            }
        }

        override fun onDestroy() {
            guarded("onDestroy") {
                configJob?.cancel()
                releasePlayer()
            }
            super.onDestroy()
        }

        private fun resolveTarget(): EngineTarget {
            if (isPreview) return EngineTarget.HOME
            val flags = wallpaperFlags
            return if (flags and WallpaperManager.FLAG_LOCK != 0) {
                EngineTarget.LOCK
            } else {
                EngineTarget.HOME
            }
        }

        private fun applyPlayback() {
            val wallpaperId = when (target) {
                EngineTarget.LOCK -> config.activeLockWallpaperId
                EngineTarget.HOME -> config.activeHomeWallpaperId
            }
            val file = resolveFile(wallpaperId)
            if (file == null) {
                releasePlayer()
                return
            }
            val player = exoPlayer ?: createPlayer()
            player.setVideoScalingMode(scalingMode.videoScalingMode())
            player.setVideoSurfaceHolder(surfaceHolder)
            player.volume = if (target == EngineTarget.LOCK) {
                if (config.muteLock) 0f else 1f
            } else {
                if (config.muteHome) 0f else 1f
            }
            if (preparedId == wallpaperId) return
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.prepare()
            player.playWhenReady = !hasVisibilityState || isVisible
            preparedId = wallpaperId
        }

        private fun createPlayer(): ExoPlayer {
            val player = ExoPlayer.Builder(engineContext()).build()
            player.addListener(playerListener)
            exoPlayer = player
            return player
        }

        private fun releasePlayer() {
            exoPlayer?.let { player ->
                player.removeListener(playerListener)
                player.setVideoSurfaceHolder(null)
                player.release()
            }
            exoPlayer = null
            preparedId = null
        }

        private fun engineContext(): Context = displayContext ?: this@VideoLiveWallpaperService

        private fun guarded(stage: String, block: () -> Unit) {
            try {
                block()
            } catch (throwable: Throwable) {
                Log.e(TAG, "Engine failed during $stage", throwable)
            }
        }

        private fun WallpaperScaling.videoScalingMode(): Int = when (this) {
            WallpaperScaling.CENTER_CROP -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            WallpaperScaling.FIT_XY, WallpaperScaling.STRETCH -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }

    private enum class EngineTarget {
        HOME,
        LOCK,
    }

    private companion object {

        const val TAG = "VideoWallpaper"
    }
}