package com.kgeutris.bullsandcows.domain.repository

import com.kgeutris.bullsandcows.domain.model.GameSession

/**
 * Репозиторий для работы с историей игр и лидербордом.
 * Слой Domain — не зависит от Android, Room, DataStore.
 */
interface GameRepository {
    /**
     * Сохраняет завершённую игровую сессию.
     */
    suspend fun saveSession(session: GameSession)

    /**
     * Возвращает всю историю игр (хронологически).
     */
    suspend fun getHistory(): List<GameSession>

    /**
     * Возвращает топ-10 результатов в одиночной игре (победы, отсортировано по attemptsCount ASC).
     */
    suspend fun getLeaderboard(): List<GameSession>
}