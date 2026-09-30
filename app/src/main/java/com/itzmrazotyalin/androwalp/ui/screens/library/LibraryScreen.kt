package com.itzmrazotyalin.androwalp.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itzmrazotyalin.androwalp.R
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

    uiState.details?.let { wallpaper ->
        AlertDialog(
            onDismissRequest = viewModel::onDetailsDismissed,
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
                        value = stringResource(scalingLabelRes(wallpaper.scaling)),
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
                TextButton(onClick = viewModel::onDetailsDismissed) {
                    Text(text = stringResource(R.string.action_close))
                }
            },
        )
    }
}