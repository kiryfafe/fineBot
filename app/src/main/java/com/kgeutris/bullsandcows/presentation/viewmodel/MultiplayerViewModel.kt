package com.kgeutris.bullsandcows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kgeutris.bullsandcows.domain.model.GameMode
import com.kgeutris.bullsandcows.domain.model.GameSession
import com.kgeutris.bullsandcows.domain.repository.GameRepository
import com.kgeutris.bullsandcows.domain.usecase.BullsCowsEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MultiplayerViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    // -------------------------------------------------------------
    // ФАЗЫ МУЛЬТИПЛЕЕРА
    // -------------------------------------------------------------
    sealed interface Phase {
        object EnterNames : Phase
        object SelectLength : Phase

        object Player1SetSecret : Phase
        object Player2SetSecret : Phase

        object Player1Guessing : Phase
        object Player2Guessing : Phase

        object GameFinished : Phase
    }

    // -------------------------------------------------------------
    // СОСТОЯНИЕ
    // -------------------------------------------------------------
    data class State(
        val phase: Phase = Phase.EnterNames,

        val player1Name: String = "",
        val player2Name: String = "",

        val numberLength: Int = 4,

        val secretP1: String = "",
        val secretP2: String = "",

        val currentGuess: String = "",

        val historyP1: List<Pair<String, Pair<Int, Int>>> = emptyList(),
        val historyP2: List<Pair<String, Pair<Int, Int>>> = emptyList(),

        val error: String? = null,
        val winner: String? = null
    )

    private val _uiState = MutableStateFlow(State())
    val uiState: StateFlow<State> = _uiState

    private fun update(update: State.() -> State) {
        _uiState.value = _uiState.value.update()
    }

    // -------------------------------------------------------------
    // ИМЕНА
    // -------------------------------------------------------------

    fun setPlayer1Name(name: String) = update { copy(player1Name = name) }
    fun setPlayer2Name(name: String) = update { copy(player2Name = name) }

    fun confirmNames() = update {
        if (player1Name.isBlank() || player2Name.isBlank()) {
            copy(error = "Введите имена игроков")
        } else copy(phase = Phase.SelectLength)
    }

    // -------------------------------------------------------------
    // ДЛИНА ЧИСЛА
    // -------------------------------------------------------------

    fun setNumberLength(length: Int) = update { copy(numberLength = length) }

    fun confirmLength() = update {
        copy(phase = Phase.Player1SetSecret)
    }

    // -------------------------------------------------------------
    // ЗАГАДЫВАНИЕ ЧИСЕЛ
    // -------------------------------------------------------------

    fun confirmSecret() = update {
        val secret = currentGuess

        if (secret.length != numberLength) {
            return@update copy(error = "Число должно быть длиной $numberLength")
        }
        if (secret.toSet().size != secret.length) {
            return@update copy(error = "Цифры не должны повторяться")
        }

        when (phase) {
            Phase.Player1SetSecret ->
                copy(secretP1 = secret, currentGuess = "", phase = Phase.Player2SetSecret)

            Phase.Player2SetSecret ->
                copy(secretP2 = secret, currentGuess = "", phase = Phase.Player1Guessing)

            else -> this
        }
    }

    // -------------------------------------------------------------
    // УГАДЫВАНИЕ
    // -------------------------------------------------------------

    fun addDigit(d: Char) = update {
        if (currentGuess.length < numberLength && d !in currentGuess)
            copy(currentGuess = currentGuess + d)
        else this
    }

    fun deleteDigit() = update {
        if (currentGuess.isNotEmpty())
            copy(currentGuess = currentGuess.dropLast(1))
        else this
    }

    fun submitGuess() = update {
        if (currentGuess.length != numberLength)
            return@update copy(error = "Введите $numberLength цифр")

        val guess = currentGuess

        when (phase) {

            Phase.Player1Guessing -> {
                val (bulls, cows) = BullsCowsEvaluator.evaluate(secretP2, guess)
                val newHistory = historyP1 + (guess to (bulls to cows))

                if (bulls == numberLength) {
                    saveResult(player1Name, newHistory)
                    return@update copy(
                        historyP1 = newHistory,
                        winner = player1Name,
                        phase = Phase.GameFinished
                    )
                }

                copy(
                    historyP1 = newHistory,
                    currentGuess = "",
                    phase = Phase.Player2Guessing
                )
            }

            Phase.Player2Guessing -> {
                val (bulls, cows) = BullsCowsEvaluator.evaluate(secretP1, guess)
                val newHistory = historyP2 + (guess to (bulls to cows))

                if (bulls == numberLength) {
                    saveResult(player2Name, newHistory)
                    return@update copy(
                        historyP2 = newHistory,
                        winner = player2Name,
                        phase = Phase.GameFinished
                    )
                }

                copy(
                    historyP2 = newHistory,
                    currentGuess = "",
                    phase = Phase.Player1Guessing
                )
            }

            else -> this
        }
    }

    // -------------------------------------------------------------
    // СОХРАНЕНИЕ ИТОГА
    // -------------------------------------------------------------

    private fun saveResult(winner: String, history: List<Pair<String, Pair<Int, Int>>>) {
        viewModelScope.launch {
            gameRepository.saveSession(
                GameSession(
                    mode = GameMode.MULTIPLAYER,
                    numberLength = _uiState.value.numberLength,
                    attemptsCount = history.size,
                    isWin = true,
                    winnerName = winner
                )
            )
        }
    }

    // -------------------------------------------------------------
    // ПРОЧЕЕ
    // -------------------------------------------------------------

    fun clearError() = update { copy(error = null) }

    fun resetGame() = update { State() }
}
