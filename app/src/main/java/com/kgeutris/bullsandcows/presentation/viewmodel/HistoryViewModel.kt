package com.kgeutris.bullsandcows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kgeutris.bullsandcows.domain.model.GameSession
import com.kgeutris.bullsandcows.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<GameSessionUiItem>>(emptyList())
    val sessions: StateFlow<List<GameSessionUiItem>> = _sessions

    init {
        viewModelScope.launch {
            // ✅ Правильно: вызываем suspend-функцию внутри launch
            val history = gameRepository.getHistory()
            val items = history
                .sortedByDescending { it.timestamp }
                .groupBy { timestampToLocalDate(it.timestamp) }
                .flatMap { (date, sessions) ->
                    listOf(GameSessionUiItem.DateHeader(date)) +
                            sessions.map { GameSessionUiItem.SessionItem(it) }
                }
            _sessions.value = items
        }
    }
}

private fun timestampToLocalDate(timestamp: Long): String {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

sealed interface GameSessionUiItem {
    data class DateHeader(val date: String) : GameSessionUiItem
    data class SessionItem(val session: GameSession) : GameSessionUiItem
}