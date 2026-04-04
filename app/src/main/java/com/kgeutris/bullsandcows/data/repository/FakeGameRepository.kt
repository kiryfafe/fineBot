package com.kgeutris.bullsandcows.data.repository

import com.kgeutris.bullsandcows.domain.model.GameSession
import com.kgeutris.bullsandcows.domain.repository.GameRepository

/**
 * Временная реализация для разработки и тестов.
 * Хранит данные в памяти (при перезапуске приложения — сбрасывается).
 * Позже будет заменена на RoomGameRepository.
 */
class FakeGameRepository : GameRepository {
    private val sessions = mutableListOf<GameSession>()

    override suspend fun saveSession(session: GameSession) {
        // Генерируем ID как порядковый номер
        val newSession = session.copy(id = sessions.size + 1L)
        sessions += newSession
    }

    override suspend fun getHistory(): List<GameSession> =
        sessions.toList() // возвращает копию, чтобы избежать модификации извне

    override suspend fun getLeaderboard(): List<GameSession> =
        sessions
            .filter { it.mode.name == "SINGLE" && it.isWin }
            .sortedBy { it.attemptsCount }
            .take(10)
}