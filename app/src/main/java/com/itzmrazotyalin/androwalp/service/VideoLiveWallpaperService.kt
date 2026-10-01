package com.itzmrazotyalin.androwalp.service

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import java.io.File
import java.util.Collections
import java.util.LinkedHashSet
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.itzmrazotyalin.androwalp.data.settings.SettingsRepository
import com.itzmrazotyalin.androwalp.domain.model.WallpaperConfig
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling
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

    private val keyguardManager: KeyguardManager? by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }

    private val engines: MutableSet<VideoEngine> = Collections.synchronizedSet(LinkedHashSet())

    private val screenReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action !in setOf(Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT)) return
            engines.toList().forEach { engine ->
                engine.refreshPlaybackTarget()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        engines.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class VideoEngine : Engine(), SurfaceHolder.Callback {

        private var exoPlayer: ExoPlayer? = null
        private var settingsJob: Job? = null
        private var surfaceHolder: SurfaceHolder? = null

        private var config: WallpaperConfig = WallpaperConfig(
            homeWallpaperPath = null,
            lockWallpaperPath = null,
            resetOnUnlock = true,
            muteHome = true,
            muteLock = true,
            scalingMode = WallpaperScaling.CENTER_CROP,
        )
        private var preparedKey: String? = null
        private var scalingMode = WallpaperScaling.CENTER_CROP
        private var isVisible = true
        private var hasVisibilityState = false

        private val playerListener = object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "state=$playbackState")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "isPlaying=$isPlaying")
            }

            override fun onRenderedFirstFrame() {
                Log.d(TAG, "firstFrame rendered")
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                Log.d(TAG, "videoSize=${videoSize.width}x${videoSize.height}")
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback failed for $preparedKey", error)
                guarded("onPlayerError") { exoPlayer?.playWhenReady = false }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            guarded("onCreate") {
                engines.add(this@VideoEngine)
                surfaceHolder = holder
                holder.addCallback(this@VideoEngine)
                settingsJob = settingsRepository.wallpaperConfig
                    .onEach { updated ->
                        Log.i(TAG, "config loaded home=${updated.homeWallpaperPath != null} lock=${updated.lockWallpaperPath != null}")
                        applyConfig(updated)
                    }
                    .launchIn(serviceScope)
            }
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            guarded("surfaceCreated") {
                surfaceHolder = holder
                setTouchEventsEnabled(false)
                evaluatePlaybackTarget()
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
                    evaluatePlaybackTarget()
                    if (config.resetOnUnlock) {
                        player.seekTo(0L)
                    }
                    player.playWhenReady = true
                } else {
                    player.playWhenReady = false
                }
            }
        }

        override fun onDestroy() {
            guarded("onDestroy") {
                settingsJob?.cancel()
                engines.remove(this@VideoEngine)
                releasePlayer()
            }
            super.onDestroy()
        }

        fun refreshPlaybackTarget() {
            guarded("screenEvent") { evaluatePlaybackTarget() }
        }

        private fun applyConfig(updated: WallpaperConfig) {
            guarded("applyConfig") {
                val scalingChanged = updated.scalingMode != scalingMode
                config = updated
                scalingMode = updated.scalingMode
                exoPlayer?.let { player ->
                    if (scalingChanged) {
                        player.setVideoScalingMode(scalingMode.videoScalingMode())
                    }
                }
                evaluatePlaybackTarget()
            }
        }

        private fun isKeyguardLocked(): Boolean =
            runCatching { keyguardManager?.isKeyguardLocked == true }.getOrDefault(false)

        private fun evaluatePlaybackTarget() {
            val locked = isKeyguardLocked()
            val rawPath: String?
            val mute: Boolean
            if (locked) {
                rawPath = config.lockWallpaperPath ?: config.homeWallpaperPath
                mute = config.muteLock
            } else {
                rawPath = config.homeWallpaperPath ?: config.lockWallpaperPath
                mute = config.muteHome
            }
            val candidate = rawPath?.takeIf { it.isNotBlank() }
            val file = candidate?.let { File(it) }
            if (file == null || !file.exists() || !file.canRead() || file.length() == 0L) {
                Log.i(TAG, "No readable wallpaper for locked=$locked, engine idle")
                releasePlayer()
                return
            }
            val playbackKey = locked.toString() + file.absolutePath
            val player = exoPlayer ?: createPlayer()
            player.setVideoSurfaceHolder(surfaceHolder)
            player.volume = if (mute) 0f else 1f
            if (preparedKey == playbackKey) return
            Log.i(TAG, "preparing locked=$locked file=${file.name}")
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.prepare()
            player.playWhenReady = !hasVisibilityState || isVisible
            preparedKey = playbackKey
        }

        private fun createPlayer(): ExoPlayer {
            val player = ExoPlayer.Builder(engineContext())
                .setVideoScalingMode(scalingMode.videoScalingMode())
                .build()
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
            preparedKey = null
        }

        private fun engineContext(): Context = displayContext ?: this@VideoLiveWallpaperService

        private fun guarded(stage: String, block: () -> Unit) {
            try {
                block()
            } catch (throwable: Throwable) {
                Log.e(TAG, "Live wallpaper engine failed during $stage", throwable)
            }
        }

        private fun WallpaperScaling.videoScalingMode(): Int = when (this) {
            WallpaperScaling.CENTER_CROP -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            WallpaperScaling.FIT_XY, WallpaperScaling.STRETCH -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }

    private companion object {

        const val TAG = "VideoWallpaper"
    }
}