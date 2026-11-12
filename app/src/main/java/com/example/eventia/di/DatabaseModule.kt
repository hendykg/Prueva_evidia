package com.example.eventia.di

import android.content.Context
import androidx.room.Room
import com.example.eventia.data.local.EventiaDatabase
import com.example.eventia.data.local.dao.EventDao
import com.example.eventia.data.local.dao.FavoriteDao
import com.example.eventia.data.local.dao.VenueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EventiaDatabase =
        Room.databaseBuilder(context, EventiaDatabase::class.java, "eventia.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideEventDao(db: EventiaDatabase): EventDao = db.eventDao()

    @Provides
    fun provideFavoriteDao(db: EventiaDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideVenueDao(db: EventiaDatabase): VenueDao = db.venueDao()
}
