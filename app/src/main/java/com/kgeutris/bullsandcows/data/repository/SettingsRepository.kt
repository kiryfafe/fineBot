// В файле SettingsRepository.kt
package com.kgeutris.bullsandcows.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Добавим импорт для аннотации
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

data class AppSettings(
    val numberLength: Int = 4,
    val theme: Theme = Theme.AUTO,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

enum class Theme {
    LIGHT, DARK, AUTO
}

class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context // <-- Указываем, что нужен Application Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val NUMBER_LENGTH = intPreferencesKey("number_length")
        val THEME = stringPreferencesKey("theme")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val settings: Flow<AppSettings> = dataStore.data
        .map { preferences ->
            AppSettings(
                numberLength = preferences[PreferencesKeys.NUMBER_LENGTH] ?: 4,
                theme = Theme.valueOf(
                    preferences[PreferencesKeys.THEME] ?: Theme.AUTO.name
                ),
                soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED] ?: true,
                vibrationEnabled = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true
            )
        }

    suspend fun updateNumberLength(length: Int) {
        dataStore.edit { settings ->
            settings[PreferencesKeys.NUMBER_LENGTH] = length
        }
    }

    suspend fun updateTheme(theme: Theme) {
        dataStore.edit { settings ->
            settings[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun updateSound(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[PreferencesKeys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun updateVibration(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[PreferencesKeys.VIBRATION_ENABLED] = enabled
        }
    }
}