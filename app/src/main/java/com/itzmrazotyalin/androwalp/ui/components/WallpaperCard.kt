package com.itzmrazotyalin.androwalp.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itzmrazotyalin.androwalp.R
import com.itzmrazotyalin.androwalp.domain.model.Wallpaper
import com.itzmrazotyalin.androwalp.domain.model.WallpaperTarget
import com.itzmrazotyalin.androwalp.ui.theme.ThumbnailShape
import com.itzmrazotyalin.androwalp.ui.util.aspectRatioLabel
import com.itzmrazotyalin.androwalp.ui.util.formatDuration
import com.itzmrazotyalin.androwalp.ui.util.formatFileSize

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    onApply: () -> Unit,
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onDetails,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
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
                    filePath = wallpaper.thumbnailPath,
                    contentDescription = stringResource(R.string.cd_thumbnail),
                    modifier = Modifier.fillMaxSize(),
                    shape = ThumbnailShape,
                )
                WallpaperStatusBadge(
                    target = wallpaper.target,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
                WallpaperOverflowMenu(
                    onDetails = onDetails,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
                DurationBadge(
                    durationMs = wallpaper.durationMs,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                )
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = wallpaper.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    aspectRatioLabel(wallpaper.width, wallpaper.height)?.let { label ->
                        AspectRatioChip(label = label)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatFileSize(wallpaper.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (wallpaper.isActive) {
                        ButtonDefaults.filledTonalButtonColors()
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (wallpaper.isActive) {
                                R.string.library_action_reapply
                            } else {
                                R.string.library_action_apply
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperStatusBadge(
    target: WallpaperTarget,
    modifier: Modifier = Modifier,
) {
    val isApplied = target != WallpaperTarget.NONE
    val badgeContentColor = if (isApplied) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (isApplied) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = badgeContentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isApplied) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(badgeContentColor),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = stringResource(target.labelRes()),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun AspectRatioChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun DurationBadge(
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
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

@Composable
private fun WallpaperOverflowMenu(
    onDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.4f),
            contentColor = Color.White,
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.cd_more_options),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.library_menu_details)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onDetails()
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.library_menu_edit)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.library_menu_delete)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.DeleteOutline, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@StringRes
private fun WallpaperTarget.labelRes(): Int = when (this) {
    WallpaperTarget.NONE -> R.string.library_status_inactive
    WallpaperTarget.HOME -> R.string.library_status_home
    WallpaperTarget.LOCK -> R.string.library_status_lock
    WallpaperTarget.BOTH -> R.string.library_status_both
}