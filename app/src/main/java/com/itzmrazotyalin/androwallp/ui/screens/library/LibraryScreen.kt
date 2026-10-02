package com.itzmrazotyalin.androwallp.ui.screens.library

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itzmrazotyalin.androwallp.R
import com.itzmrazotyalin.androwallp.domain.model.Wallpaper
import com.itzmrazotyalin.androwallp.domain.model.WallpaperScaling
import com.itzmrazotyalin.androwallp.domain.model.WallpaperStatus
import com.itzmrazotyalin.androwallp.domain.model.WallpaperTarget
import com.itzmrazotyalin.androwallp.service.VideoLiveWallpaperService
import com.itzmrazotyalin.androwallp.ui.components.DetailRow
import com.itzmrazotyalin.androwallp.ui.components.EmptyLibraryState
import com.itzmrazotyalin.androwallp.ui.components.WallpaperCard
import com.itzmrazotyalin.androwallp.ui.components.scalingLabelRes
import com.itzmrazotyalin.androwallp.ui.util.aspectRatioLabel
import com.itzmrazotyalin.androwallp.ui.util.formatDuration
import com.itzmrazotyalin.androwallp.ui.util.formatFileSize
import com.itzmrazotyalin.androwallp.ui.util.formatResolution

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onImportClick: () -> Unit,
    onEditClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshSystemState()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.ShowMessage -> {
                    val message = event.formatArg
                        ?.let { context.getString(event.messageRes, it) }
                        ?: context.getString(event.messageRes)
                    snackbarHostState.showSnackbar(message)
                }

                is LibraryEvent.ApplyLiveWallpaper -> {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, VideoLiveWallpaperService::class.java),
                        )
                    }
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.message_live_wallpaper_unavailable),
                            )
                        }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.library_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                uiState.wallpapers.isEmpty() -> EmptyLibraryState(
                    onImportClick = onImportClick,
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(all = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = uiState.wallpapers,
                        key = { wallpaper -> wallpaper.id },
                    ) { wallpaper ->
                        WallpaperCard(
                            wallpaper = wallpaper,
                            status = uiState.statuses[wallpaper.id] ?: WallpaperStatus.NONE,
                            onApply = { viewModel.onApplySelected(wallpaper) },
                            onDetails = { viewModel.onDetailsRequested(wallpaper) },
                            onEdit = { onEditClick(wallpaper.id) },
                            onDelete = { viewModel.onDeleteRequested(wallpaper) },
                        )
                    }
                }
            }
        }
    }

    uiState.pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismissed,
            icon = { Icon(imageVector = Icons.Filled.DeleteOutline, contentDescription = null) },
            title = { Text(text = stringResource(R.string.dialog_delete_title)) },
            text = { Text(text = stringResource(R.string.dialog_delete_body, target.title)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirmed) {
                    Text(text = stringResource(R.string.dialog_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismissed) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }

    uiState.applyTargetFor?.let { wallpaper ->
        ApplyTargetSheet(
            wallpaper = wallpaper,
            onSelect = viewModel::onApplyTargetSelected,
            onDismiss = viewModel::onApplyTargetDismissed,
        )
    }

    uiState.details?.let { wallpaper ->
        WallpaperDetailsDialog(
            wallpaper = wallpaper,
            scalingMode = uiState.scalingMode,
            onDismiss = viewModel::onDetailsDismissed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplyTargetSheet(
    wallpaper: Wallpaper,
    onSelect: (WallpaperTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.apply_target_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                text = stringResource(R.string.apply_target_body, wallpaper.title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            TargetOption(
                label = stringResource(R.string.apply_target_home),
                icon = Icons.Filled.Home,
                onClick = { onSelect(WallpaperTarget.HOME) },
            )
            TargetOption(
                label = stringResource(R.string.apply_target_lock),
                icon = Icons.Filled.Lock,
                onClick = { onSelect(WallpaperTarget.LOCK) },
            )
            TargetOption(
                label = stringResource(R.string.apply_target_both),
                icon = Icons.Filled.Layers,
                onClick = { onSelect(WallpaperTarget.BOTH) },
            )
        }
    }
}

@Composable
private fun TargetOption(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun WallpaperDetailsDialog(
    wallpaper: Wallpaper,
    scalingMode: WallpaperScaling,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_details_title)) },
        text = {
            Column {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                DetailRow(
                    label = stringResource(R.string.label_duration),
                    value = formatDuration(wallpaper.durationMs),
                )
                DetailRow(
                    label = stringResource(R.string.label_resolution),
                    value = formatResolution(wallpaper.width, wallpaper.height)
                        ?: stringResource(R.string.value_unknown),
                )
                DetailRow(
                    label = stringResource(R.string.label_size),
                    value = formatFileSize(wallpaper.sizeBytes),
                )
                DetailRow(
                    label = stringResource(R.string.label_scaling),
                    value = stringResource(scalingLabelRes(scalingMode)),
                )
                aspectRatioLabel(wallpaper.width, wallpaper.height)?.let { label ->
                    DetailRow(
                        label = stringResource(R.string.label_aspect_ratio),
                        value = label,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_close))
            }
        },
    )
}