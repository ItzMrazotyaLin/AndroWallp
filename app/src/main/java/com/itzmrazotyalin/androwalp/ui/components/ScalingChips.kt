package com.itzmrazotyalin.androwalp.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.itzmrazotyalin.androwalp.R
import com.itzmrazotyalin.androwalp.domain.model.WallpaperScaling

@StringRes
fun scalingLabelRes(scaling: WallpaperScaling): Int = when (scaling) {
    WallpaperScaling.CENTER_CROP -> R.string.scaling_center_crop
    WallpaperScaling.FIT_XY -> R.string.scaling_fit_screen
    WallpaperScaling.STRETCH -> R.string.scaling_stretch
}

@Composable
fun ScalingChipRow(
    selected: WallpaperScaling,
    onSelected: (WallpaperScaling) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WallpaperScaling.entries.forEach { scaling ->
            FilterChip(
                selected = scaling == selected,
                onClick = { onSelected(scaling) },
                label = { Text(text = stringResource(scalingLabelRes(scaling))) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = scaling == selected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        }
    }
}