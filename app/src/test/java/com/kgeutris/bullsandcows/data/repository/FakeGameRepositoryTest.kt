package com.kgeutris.bullsandcows.data.repository

import com.kgeutris.bullsandcows.domain.model.GameMode
import com.kgeutris.bullsandcows.domain.model.GameSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeGameRepositoryTest {

    private lateinit var repository: FakeGameRepository

    @Before
    fun setup() {
        repository = FakeGameRepository()
    }

    @Test
    fun `saveSession присваивает уникальный ID`() = runTest {
        val session1 = GameSession(
            mode = GameMode.SINGLE,
            numberLength = 4,
            attemptsCount = 5,
            isWin = true
        )
        val session2 = GameSession(
            mode = GameMode.MULTIPLAYER,
            numberLength = 4,
            attemptsCount = 3,
            isWin = true,
            winnerName = "Кирилл"
        )

        repository.saveSession(session1)
        repository.saveSession(session2)

        val history = repository.getHistory()
        assertEquals(2, history.size)
        assertEquals(1L, history[0].id)
        assertEquals(2L, history[1].id)
    }

    @Test
    fun `getLeaderboard возвращает только победы в SINGLE, отсортировано по attemptsCount`() = runTest {
        repository.saveSession(
            GameSession(mode = GameMode.SINGLE, numberLength = 4, attemptsCount = 7, isWin = true)
        )
        repository.saveSession(
            GameSession(mode = GameMode.SINGLE, numberLength = 4, attemptsCount = 3, isWin = true) // ← лучше
        )
        repository.saveSession(
            GameSession(mode = GameMode.SINGLE, numberLength = 4, attemptsCount = 5, isWin = false) // ← не победа
        )
        repository.saveSession(
            GameSession(mode = GameMode.MULTIPLAYER, numberLength = 4, attemptsCount = 2, isWin = true) // ← не SINGLE
        )

        val leaderboard = repository.getLeaderboard()
        assertEquals(2, leaderboard.size)
        assertEquals(3, leaderboard[0].attemptsCount) // лучший результат первый
        assertEquals(7, leaderboard[1].attemptsCount)
    }

    @Test
    fun `getHistory возвращает все сессии в порядке добавления`() = runTest {
        repository.saveSession(
            GameSession(mode = GameMode.SINGLE, numberLength = 3, attemptsCount = 1, isWin = true)
        )
        repository.saveSession(
            GameSession(mode = GameMode.AI_GUESSING, numberLength = 4, attemptsCount = 6, isWin = true)
        )

        val history = repository.getHistory()
        assertEquals(2, history.size)
        assertEquals(GameMode.SINGLE, history[0].mode)
        assertEquals(GameMode.AI_GUESSING, history[1].mode)
    }
}