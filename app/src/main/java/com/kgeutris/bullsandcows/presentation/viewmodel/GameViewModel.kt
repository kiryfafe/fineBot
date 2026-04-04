package com.kgeutris.bullsandcows.presentation.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kgeutris.bullsandcows.data.repository.SettingsRepository
import com.kgeutris.bullsandcows.domain.model.GameMode
import com.kgeutris.bullsandcows.domain.model.GameSession
import com.kgeutris.bullsandcows.domain.repository.GameRepository
import com.kgeutris.bullsandcows.domain.usecase.BullsCowsEvaluator
import com.kgeutris.bullsandcows.domain.usecase.SecretNumberGenerator
import com.kgeutris.bullsandcows.util.SoundManager
import com.kgeutris.bullsandcows.util.vibrateError
import com.kgeutris.bullsandcows.util.vibrateShort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GameState(
    val secret: String = "",
    val currentGuess: String = "",
    val history: List<Pair<String, Pair<Int, Int>>> = emptyList(),
    val isVictory: Boolean = false,
    val isGameOver: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private var soundManager: SoundManager? = null
    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null  // ← ДОБАВЬ ЭТО

    fun startNewGame() { // Убрали параметр по умолчанию
        viewModelScope.launch {
            // Получаем сохраненную длину из настроек
            val settings = settingsRepository.settings.first()
            val length = settings.numberLength

            val secret = SecretNumberGenerator.generate(length)
            _uiState.value = GameState(secret = secret)
        }
    }

    fun makeGuess(guess: String) {
        val state = _uiState.value
        if (state.isVictory || state.isGameOver) return

        // Валидация
        if (guess.length != state.secret.length) {
            _uiState.value = state.copy(error = "Длина должна быть ${state.secret.length}")
            return
        }
        if (guess.toSet().size != guess.length) {
            _uiState.value = state.copy(error = "Цифры не должны повторяться")
            return
        }

        // Оценка — ВЫПОЛНЯЕМ ОДИН РАЗ
        val (bulls, cows) = BullsCowsEvaluator.evaluate(state.secret, guess)
        val isVictory = bulls == state.secret.length
        val newHistory = state.history + (guess to (bulls to cows))
        val isGameOver = isVictory

        // Обновление UI
        _uiState.value = GameState(
            secret = state.secret,
            currentGuess = "",
            history = newHistory,
            isVictory = isVictory,
            isGameOver = isGameOver,
            error = null
        )

        // 🔊 Звук + вибрация
        val ctx = context ?: return  // безопасный выход, если context не задан
        soundManager?.let { sm ->
            when {
                isVictory -> {
                    sm.play(SoundManager.Sound.WIN)
                    ctx.vibrateShort()
                }
                bulls + cows > 0 -> {
                    sm.play(SoundManager.Sound.CLICK)
                }
                else -> {
                    sm.play(SoundManager.Sound.BUZZ)
                    ctx.vibrateError()
                }
            }
        }

        // Сохранение сессии
        if (isVictory) {
            viewModelScope.launch {
                gameRepository.saveSession(
                    GameSession(
                        mode = GameMode.SINGLE,
                        numberLength = state.secret.length,
                        attemptsCount = newHistory.size,
                        isWin = true
                    )
                )
            }
        }
    }

    fun updateCurrentGuess(guess: String) {
        _uiState.value = _uiState.value.copy(currentGuess = guess, error = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ✅ Правильная инициализация с context
    fun initSound(context: Context) {
        this.context = context
        if (soundManager == null) {
            soundManager = SoundManager(context)
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseSound()
    }

    fun releaseSound() {
        soundManager?.release()
        soundManager = null
        context = null
    }
}