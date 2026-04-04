package com.kgeutris.bullsandcows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kgeutris.bullsandcows.domain.model.GameMode
import com.kgeutris.bullsandcows.domain.model.GameSession
import com.kgeutris.bullsandcows.domain.repository.GameRepository
import com.kgeutris.bullsandcows.domain.usecase.AiSolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AiGuessingPhase {
    SET_SECRET,    // Пользователь вводит число
    AI_GUESSING,   // AI угадывает
    FINISHED       // Игра завершена
}
data class AiGameState(
    val phase: AiGuessingPhase = AiGuessingPhase.SET_SECRET, // ← ДОБАВИТЬ
    val numberLength: Int = 4,
    val currentGuess: String? = null,
    val secret: String = "", // ← УЖЕ ЕСТЬ
    val history: List<Pair<String, Pair<Int, Int>>> = emptyList(),
    val bullsInput: String = "",
    val cowsInput: String = "",
    val activeInput: InputType = InputType.BULLS,
    val error: String? = null,
    val isVictory: Boolean = false,
    val attemptsCount: Int = 0, // ← ДОБАВИТЬ
    val canSubmitFeedback: Boolean = false
)

enum class InputType {
    BULLS, COWS
}

@HiltViewModel
class AiGuessingViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiGameState())
    val uiState: StateFlow<AiGameState> = _uiState.asStateFlow()

    private var solver: AiSolver? = null

    fun startNewGame(length: Int = 4) {
        solver = null
        _uiState.value = AiGameState(
            phase = AiGuessingPhase.SET_SECRET,
            numberLength = length,
            secret = "" // Сбрасываем секрет
        )
    }

    fun setActiveInput(inputType: InputType) {
        _uiState.value = _uiState.value.copy(
            activeInput = inputType,
            error = null
        )
    }


    fun addDigitToInput(digit: Char) {
        val state = _uiState.value

        when (state.activeInput) {
            InputType.BULLS -> {
                // Просто устанавливаем цифру (максимум 1 цифра)
                _uiState.value = state.copy(bullsInput = digit.toString(), error = null)
            }
            InputType.COWS -> {
                // Просто устанавливаем цифру (максимум 1 цифра)
                _uiState.value = state.copy(cowsInput = digit.toString(), error = null)
            }
        }
    }

    fun deleteLastDigit() {
        val state = _uiState.value
        when (state.activeInput) {
            InputType.BULLS -> {
                // Просто удаляем последнюю цифру
                val newValue = state.bullsInput.dropLast(1)
                _uiState.value = state.copy(
                    bullsInput = newValue.ifEmpty { "" },
                    error = null
                )
            }
            InputType.COWS -> {
                // Просто удаляем последнюю цифру
                val newValue = state.cowsInput.dropLast(1)
                _uiState.value = state.copy(
                    cowsInput = newValue.ifEmpty { "" },
                    error = null
                )
            }
        }
    }

    fun submitFeedback() {
        val state = _uiState.value
        val guess = state.currentGuess ?: return

        // Валидация ввода
        val bulls = state.bullsInput.toIntOrNull() ?: 0
        val cows = state.cowsInput.toIntOrNull() ?: 0

        // Проверка диапазона
        if (bulls !in 0..state.numberLength) {
            _uiState.value = state.copy(
                error = "Быки должны быть от 0 до ${state.numberLength}",
                activeInput = InputType.BULLS
            )
            return
        }

        if (cows !in 0..state.numberLength) {
            _uiState.value = state.copy(
                error = "Коровы должны быть от 0 до ${state.numberLength}",
                activeInput = InputType.COWS
            )
            return
        }

        // Проверка суммы
        if (bulls + cows > state.numberLength) {
            _uiState.value = state.copy(
                error = "Сумма быков и коров не может превышать ${state.numberLength}",
                activeInput = InputType.BULLS
            )
            return
        }

        // Проверка с помощью AI Solver
        try {
            solver?.validateFeedback(guess, bulls, cows)
        } catch (e: IllegalArgumentException) {
            _uiState.value = state.copy(
                error = e.message ?: "Некорректная оценка",
                activeInput = InputType.BULLS
            )
            return
        }

        val newHistory = state.history + (guess to (bulls to cows))
        val isVictory = bulls == state.numberLength
        val isGameOver = isVictory || newHistory.size >= 15

        if (isVictory || isGameOver) {
            viewModelScope.launch {
                gameRepository.saveSession(
                    GameSession(
                        mode = GameMode.AI_GUESSING,
                        numberLength = state.numberLength,
                        attemptsCount = newHistory.size,
                        isWin = isVictory
                    )
                )
            }

            _uiState.value = state.copy(
                phase = AiGuessingPhase.FINISHED,
                history = newHistory,
                bullsInput = "",
                cowsInput = "",
                isVictory = isVictory,
                attemptsCount = newHistory.size,
                canSubmitFeedback = false,
                error = null
            )
            return
        }

        // Следующая попытка AI
        val nextGuess = solver?.nextGuess(newHistory)
        if (nextGuess == null) {
            _uiState.value = state.copy(
                error = "Нет подходящих вариантов. Проверьте предыдущие оценки.",
                canSubmitFeedback = false
            )
            return
        }

        // ВАЖНО: Используем state.copy(), а не создаем новый AiGameState
        _uiState.value = state.copy(
            currentGuess = nextGuess,
            history = newHistory,
            bullsInput = "",
            cowsInput = "",
            activeInput = InputType.BULLS,
            canSubmitFeedback = true,
            error = null,
            attemptsCount = newHistory.size // Обновляем счетчик попыток
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    private val _secret = MutableStateFlow("")
    val secret: String get() = _secret.value

    fun setSecret(number: String) {
        if (number.length == _uiState.value.numberLength &&
            number.all { it.isDigit() } &&
            number.toSet().size == number.length) {
            _secret.value = number
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Число должно содержать ${_uiState.value.numberLength} неповторяющихся цифр"
            )
        }
    }

    fun clearSecret() {
        _secret.value = ""
    }

    fun addDigitToSecret(digit: Char) {
        val state = _uiState.value
        if (state.secret.length < state.numberLength) {
            val newSecret = state.secret + digit
            // Проверка на повторяющиеся цифры
            if (newSecret.toSet().size == newSecret.length) {
                _uiState.value = state.copy(
                    secret = newSecret,
                    error = null
                )
            } else {
                _uiState.value = state.copy(
                    error = "Цифры не должны повторяться"
                )
            }
        }
    }

    fun deleteSecretDigit() {
        val state = _uiState.value
        _uiState.value = state.copy(
            secret = state.secret.dropLast(1),
            error = null
        )
    }

    fun confirmSecret() {
        val state = _uiState.value
        if (state.secret.length != state.numberLength) {
            _uiState.value = state.copy(
                error = "Нужно ввести ${state.numberLength} цифр"
            )
            return
        }

        // Запускаем AI
        startAiGuessing()
    }

    private fun startAiGuessing() {
        solver = AiSolver(_uiState.value.numberLength)
        val firstGuess = solver?.nextGuess() ?: "1234"
        _uiState.value = _uiState.value.copy(
            phase = AiGuessingPhase.AI_GUESSING, // ← МЕНЯЕМ ФАЗУ
            currentGuess = firstGuess,
            activeInput = InputType.BULLS,
            canSubmitFeedback = true,
            error = null
        )
    }

    fun startNewGameWithSameSecret() {
        val state = _uiState.value
        if (state.secret.length == state.numberLength) {
            solver = AiSolver(state.numberLength)
            val firstGuess = solver?.nextGuess() ?: "1234"
            _uiState.value = state.copy(
                phase = AiGuessingPhase.AI_GUESSING,
                currentGuess = firstGuess,
                history = emptyList(),
                bullsInput = "",
                cowsInput = "",
                activeInput = InputType.BULLS,
                canSubmitFeedback = true,
                isVictory = false,
                attemptsCount = 0,
                error = null
            )
        }
    }

    fun resetGame() {
        solver = null
        _uiState.value = AiGameState(
            phase = AiGuessingPhase.SET_SECRET,
            numberLength = _uiState.value.numberLength, // Сохраняем длину
            secret = "" // Сбрасываем секрет
        )
    }
}