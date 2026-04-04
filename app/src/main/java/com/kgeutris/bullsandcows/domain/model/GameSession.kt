package com.kgeutris.bullsandcows.domain.model

data class GameSession(
    val id: Long = 0,
    val mode: GameMode,
    val numberLength: Int,
    val attemptsCount: Int,
    val isWin: Boolean,
    val winnerName: String? = null,
    val timestamp: Long = System.currentTimeMillis() / 1000
)