package com.kgeutris.bullsandcows.data.repository

import com.kgeutris.bullsandcows.data.local.AppDatabase
import com.kgeutris.bullsandcows.data.local.GameSessionEntity
import com.kgeutris.bullsandcows.domain.model.GameSession
import com.kgeutris.bullsandcows.domain.repository.GameRepository
import javax.inject.Inject

class RoomGameRepository @Inject constructor(
    database: AppDatabase
) : GameRepository {

    private val dao = database.gameSessionDao()

    override suspend fun saveSession(session: GameSession) {
        val entity = GameSessionEntity.fromDomain(session)
        dao.insert(entity)
    }

    override suspend fun getHistory(): List<GameSession> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getLeaderboard(): List<GameSession> =
        dao.getLeaderboard().map { it.toDomain() }
}