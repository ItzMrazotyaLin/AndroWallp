package com.itzmrazotyalin.androwalp.ui.screens.picker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itzmrazotyalin.androwalp.R
import com.itzmrazotyalin.androwalp.domain.model.VideoMetadata
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling
import com.itzmrazotyalin.androwalp.ui.components.DetailRow
import com.itzmrazotyalin.androwalp.ui.components.ScalingChipRow
import com.itzmrazotyalin.androwalp.ui.components.VideoThumbnail
import com.itzmrazotyalin.androwalp.ui.util.formatDuration
import com.itzmrazotyalin.androwalp.ui.util.formatFileSize
import com.itzmrazotyalin.androwalp.ui.util.formatResolution
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PickerViewModel = viewModel(factory = PickerViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.onVideoPicked(uri)
            } else {
                viewModel.onPickDismissed()
            }
        },
    )

    val launchPicker: () -> Unit = {
        runCatching {
            pickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
            )
        }.onFailure {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.message_picker_unavailable))
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PickerEvent.Saved -> onSaved()

                PickerEvent.ReadFailed -> snackbarHostState.showSnackbar(
                    context.getString(R.string.message_video_unreadable),
                )

                PickerEvent.ImportFailed -> snackbarHostState.showSnackbar(
                    context.getString(R.string.message_import_failed),
                )
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.picker_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val metadata = uiState.metadata
            if (metadata == null) {
                PickerPrompt(
                    isLoading = uiState.isInspecting,
                    onPickClick = launchPicker,
                )
            } else {
                VideoPreviewCard(
                    metadata = metadata,
                    onChangeClick = launchPicker,
                )
                VideoMetadataCard(metadata = metadata)
                ScalingSelectorCard(
                    selected = uiState.scaling,
                    onSelected = viewModel::onScalingSelected,
                )
                Button(
                    onClick = viewModel::onSaveRequested,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(R.string.picker_saving_action))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = stringResource(R.string.picker_save_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerPrompt(
    isLoading: Boolean,
    onPickClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLoading) {
                Text(
                    text = stringResource(R.string.picker_inspecting),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
Icon(
                    painter = painterResource(id = R.drawable.ic_video_camera_back_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.picker_prompt_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.picker_prompt_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
Button(onClick = onPickClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video_camera_back_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = stringResource(R.string.picker_pick_action))
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewCard(
    metadata: VideoMetadata,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                VideoThumbnail(
                    filePath = metadata.previewPath,
                    contentDescription = stringResource(R.string.picker_preview_label),
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.large,
                )
                SurfaceDurationBadge(
                    durationMs = metadata.durationMs,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                )
            }
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = metadata.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onChangeClick) {
                    Text(text = stringResource(R.string.picker_change_action))
                }
            }
        }
    }
}

@Composable
private fun VideoMetadataCard(
    metadata: VideoMetadata,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.picker_metadata_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            DetailRow(
                label = stringResource(R.string.label_file_name),
                value = metadata.displayName,
            )
            DetailRow(
                label = stringResource(R.string.label_duration),
                value = formatDuration(metadata.durationMs),
            )
            DetailRow(
                label = stringResource(R.string.label_resolution),
                value = formatResolution(metadata.width, metadata.height)
                    ?: stringResource(R.string.value_unknown),
            )
            DetailRow(
                label = stringResource(R.string.label_size),
                value = formatFileSize(metadata.sizeBytes),
            )
        }
    }
}

@Composable
private fun ScalingSelectorCard(
    selected: WallpaperScaling,
    onSelected: (WallpaperScaling) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.picker_scaling_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.picker_scaling_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ScalingChipRow(selected = selected, onSelected = onSelected)
        }
    }
}

@Composable
private fun SurfaceDurationBadge(
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White,
    ) {
        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}