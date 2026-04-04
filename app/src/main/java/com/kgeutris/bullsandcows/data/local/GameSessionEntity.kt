package com.kgeutris.bullsandcows.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kgeutris.bullsandcows.domain.model.GameMode

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "mode")
    val mode: String,  // будем хранить как строку: "SINGLE", "AI_GUESSING", "MULTIPLAYER"

    @ColumnInfo(name = "number_length")
    val numberLength: Int,

    @ColumnInfo(name = "attempts_count")
    val attemptsCount: Int,

    @ColumnInfo(name = "is_win")
    val isWin: Boolean,

    @ColumnInfo(name = "winner_name")
    val winnerName: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis() / 1000
) {
    // Конвертеры
    fun toDomain() = com.kgeutris.bullsandcows.domain.model.GameSession(
        id = id,
        mode = GameMode.valueOf(mode),
        numberLength = numberLength,
        attemptsCount = attemptsCount,
        isWin = isWin,
        winnerName = winnerName,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(domain: com.kgeutris.bullsandcows.domain.model.GameSession) = GameSessionEntity(
            id = domain.id,
            mode = domain.mode.name,
            numberLength = domain.numberLength,
            attemptsCount = domain.attemptsCount,
            isWin = domain.isWin,
            winnerName = domain.winnerName,
            timestamp = domain.timestamp
        )
    }
}