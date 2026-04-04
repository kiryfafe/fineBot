package com.kgeutris.bullsandcows.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameSessionEntity::class],
    version = 1,
    exportSchema = false  // учебный проект — миграции не нужны
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
}