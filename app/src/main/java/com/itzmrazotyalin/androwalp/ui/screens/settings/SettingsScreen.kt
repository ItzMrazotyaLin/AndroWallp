package com.itzmrazotyalin.androwalp.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itzmrazotyalin.androwalp.R
import com.itzmrazotyalin.androwalp.core.AppBuildInfo
import com.itzmrazotyalin.androwalp.domain.model.AppLanguage
import com.itzmrazotyalin.androwalp.domain.model.ThemeMode
import com.itzmrazotyalin.androwalp.ui.components.DetailRow
import com.itzmrazotyalin.androwalp.ui.components.SettingsActionRow
import com.itzmrazotyalin.androwalp.ui.components.SettingsSection
import com.itzmrazotyalin.androwalp.ui.components.SettingsSectionStyle
import com.itzmrazotyalin.androwalp.ui.components.SettingsSwitchRow
import com.itzmrazotyalin.androwalp.ui.components.themeModeLabelRes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLanguageDialogVisible by remember { mutableStateOf(false) }
    val noBrowserMessage = stringResource(R.string.message_no_browser)
    val openSourceRepository: () -> Unit = {
        runCatching { uriHandler.openUri(AppBuildInfo.SOURCE_URL) }
            .onFailure { scope.launch { snackbarHostState.showSnackbar(noBrowserMessage) } }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
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
            SettingsSection(
                title = stringResource(R.string.settings_section_playback),
                icon = Icons.Filled.Replay,
                style = SettingsSectionStyle.FILLED,
            ) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_reset_on_unlock_title),
                    summary = stringResource(R.string.settings_reset_on_unlock_summary),
                    checked = settings.resetOnUnlock,
                    onCheckedChange = viewModel::onResetOnUnlockChanged,
                    icon = Icons.Filled.Replay,
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_section_audio),
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                style = SettingsSectionStyle.FILLED,
            ) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_mute_home_title),
                    summary = stringResource(R.string.settings_mute_home_summary),
                    checked = settings.muteHome,
                    onCheckedChange = viewModel::onMuteHomeChanged,
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                )
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_mute_lock_title),
                    summary = stringResource(R.string.settings_mute_lock_summary),
                    checked = settings.muteLock,
                    onCheckedChange = viewModel::onMuteLockChanged,
                    icon = Icons.Filled.Lock,
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_section_appearance),
                icon = Icons.Filled.Palette,
                style = SettingsSectionStyle.OUTLINED,
            ) {
                Text(
                    text = stringResource(R.string.settings_theme_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { themeMode ->
                        FilterChip(
                            selected = themeMode == settings.themeMode,
                            onClick = { viewModel.onThemeModeSelected(themeMode) },
                            label = { Text(text = stringResource(themeModeLabelRes(themeMode))) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_dynamic_color_title),
                    summary = stringResource(R.string.settings_dynamic_color_summary),
                    checked = settings.useDynamicColor,
                    onCheckedChange = viewModel::onUseDynamicColorChanged,
                    icon = Icons.Filled.Palette,
                )
                Spacer(modifier = Modifier.height(4.dp))
                SettingsActionRow(
                    title = stringResource(R.string.settings_language_title),
                    summary = stringResource(settings.language.labelRes),
                    onClick = { isLanguageDialogVisible = true },
                    icon = Icons.Filled.Language,
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_section_about),
                icon = Icons.Filled.Info,
                style = SettingsSectionStyle.FILLED,
            ) {
                SettingsActionRow(
                    title = stringResource(R.string.settings_author_title),
                    summary = stringResource(R.string.settings_author_summary),
                    onClick = openSourceRepository,
                    icon = Icons.Filled.Person,
                )
                DetailRow(
                    label = stringResource(R.string.app_name),
                    value = stringResource(R.string.settings_version, AppBuildInfo.DEV_BUILD),
                )
            }
        }
    }

    if (isLanguageDialogVisible) {
        LanguagePickerDialog(
            selected = settings.language,
            onSelect = { language ->
                viewModel.onLanguageSelected(language)
                isLanguageDialogVisible = false
            },
            onDismiss = { isLanguageDialogVisible = false },
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_language_title)) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = language == selected,
                            onClick = { onSelect(language) },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(language.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_language_more_soon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_close))
            }
        },
    )
}