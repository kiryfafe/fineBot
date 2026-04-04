package com.kgeutris.bullsandcows.di

import android.content.Context
import androidx.room.Room
import com.kgeutris.bullsandcows.data.local.AppDatabase
import com.kgeutris.bullsandcows.data.repository.RoomGameRepository
import com.kgeutris.bullsandcows.domain.repository.GameRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGameRepository(
        database: AppDatabase
    ): GameRepository = RoomGameRepository(database)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "bulls_and_cows.db"
    ).build()
}