package com.itzmrazotyalin.androwalp.ui.screens.library

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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itzmrazotyalin.androwalp.R
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling
import com.itzmrazotyalin.androwalp.domain.model.WallpaperTarget
import com.itzmrazotyalin.androwalp.service.VideoLiveWallpaperService
import com.itzmrazotyalin.androwalp.ui.components.DetailRow
import com.itzmrazotyalin.androwalp.ui.components.EmptyLibraryState
import com.itzmrazotyalin.androwalp.ui.components.WallpaperCard
import com.itzmrazotyalin.androwalp.ui.components.scalingLabelRes
import com.itzmrazotyalin.androwalp.ui.util.aspectRatioLabel
import com.itzmrazotyalin.androwalp.ui.util.formatDuration
import com.itzmrazotyalin.androwalp.ui.util.formatFileSize
import com.itzmrazotyalin.androwalp.ui.util.formatResolution

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

    uiState.applyTarget?.let { target ->
        ApplyTargetDialog(
            wallpaper = target,
            onSelect = viewModel::onApplyTargetConfirmed,
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

@Composable
private fun ApplyTargetDialog(
    wallpaper: Wallpaper,
    onSelect: (WallpaperTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    var selection by remember { mutableStateOf(defaultTargetFor(wallpaper.target)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.apply_target_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.apply_target_body, wallpaper.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.size(12.dp))
                TargetOption(
                    label = stringResource(R.string.apply_target_home),
                    icon = Icons.Filled.Home,
                    selected = selection == WallpaperTarget.HOME,
                    onClick = { selection = WallpaperTarget.HOME },
                )
                TargetOption(
                    label = stringResource(R.string.apply_target_lock),
                    icon = Icons.Filled.Lock,
                    selected = selection == WallpaperTarget.LOCK,
                    onClick = { selection = WallpaperTarget.LOCK },
                )
                TargetOption(
                    label = stringResource(R.string.apply_target_both),
                    icon = Icons.Filled.Layers,
                    selected = selection == WallpaperTarget.BOTH,
                    onClick = { selection = WallpaperTarget.BOTH },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selection) }) {
                Text(text = stringResource(R.string.apply_target_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun TargetOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.size(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
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

private fun defaultTargetFor(current: WallpaperTarget): WallpaperTarget = when (current) {
    WallpaperTarget.LOCK -> WallpaperTarget.LOCK
    WallpaperTarget.BOTH -> WallpaperTarget.BOTH
    WallpaperTarget.NONE, WallpaperTarget.HOME -> WallpaperTarget.HOME
}