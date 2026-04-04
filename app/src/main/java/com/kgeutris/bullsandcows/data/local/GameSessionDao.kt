package com.kgeutris.bullsandcows.data.local

import androidx.room.Insert
import androidx.room.Query
import androidx.room.Dao

@Dao
interface GameSessionDao {
    @Insert
    suspend fun insert(session: GameSessionEntity)

    @Query("SELECT * FROM game_sessions ORDER BY timestamp DESC")
    suspend fun getAll(): List<GameSessionEntity>

    @Query(
        """
        SELECT * FROM game_sessions 
        WHERE mode = 'SINGLE' AND is_win = 1 
        ORDER BY attempts_count ASC 
        LIMIT 10
        """
    )
    suspend fun getLeaderboard(): List<GameSessionEntity>
}