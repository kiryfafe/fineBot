@file:OptIn(ExperimentalMaterial3Api::class)

package com.kgeutris.bullsandcows.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kgeutris.bullsandcows.data.repository.Theme
import com.kgeutris.bullsandcows.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "Длина числа") {
                NumberLengthSelector(
                    currentLength = settings.numberLength,
                    onLengthChanged = viewModel::updateNumberLength
                )
            }

            SettingsSection(title = "Тема") {
                ThemeSelector(
                    currentTheme = settings.theme,
                    onThemeChanged = viewModel::updateTheme
                )
            }

            SettingsSection(title = "Обратная связь") {
                SwitchSetting(
                    title = "Звук",
                    checked = settings.soundEnabled,
                    onCheckedChange = viewModel::updateSound
                )
                SwitchSetting(
                    title = "Вибрация",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = viewModel::updateVibration
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun NumberLengthSelector(
    currentLength: Int,
    onLengthChanged: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        (3..6).forEach { length ->
            FilterChip(
                selected = length == currentLength,
                onClick = { onLengthChanged(length) },
                label = { Text("$length") }
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    currentTheme: Theme,
    onThemeChanged: (Theme) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Theme.entries.forEach { theme ->
            FilterChip(
                selected = theme == currentTheme,
                onClick = { onThemeChanged(theme) },
                label = { Text(theme.label) }
            )
        }
    }
}

private val Theme.label: String
    get() = when (this) {
        Theme.LIGHT -> "Светлая"
        Theme.DARK -> "Тёмная"
        Theme.AUTO -> "Авто"
    }

@Composable
private fun SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}