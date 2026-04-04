package com.kgeutris.bullsandcows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kgeutris.bullsandcows.data.repository.AppSettings
import com.kgeutris.bullsandcows.data.repository.SettingsRepository
import com.kgeutris.bullsandcows.data.repository.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateNumberLength(length: Int) {
        viewModelScope.launch {
            settingsRepository.updateNumberLength(length)
        }
    }

    fun updateTheme(theme: Theme) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
        }
    }

    fun updateSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSound(enabled)
        }
    }

    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateVibration(enabled)
        }
    }
}