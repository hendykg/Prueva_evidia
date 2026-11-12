package com.example.eventia.di

import com.example.eventia.data.repository.AttendanceRepositoryImpl
import com.example.eventia.data.repository.AuthRepositoryImpl
import com.example.eventia.data.repository.EventsRepositoryImpl
import com.example.eventia.data.repository.FavoritesRepositoryImpl
import com.example.eventia.data.repository.VenuesRepositoryImpl
import com.example.eventia.domain.repository.AttendanceRepository
import com.example.eventia.domain.repository.AuthRepository
import com.example.eventia.domain.repository.EventsRepository
import com.example.eventia.domain.repository.FavoritesRepository
import com.example.eventia.domain.repository.VenuesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindEventsRepository(impl: EventsRepositoryImpl): EventsRepository

    @Binds
    @Singleton
    abstract fun bindVenuesRepository(impl: VenuesRepositoryImpl): VenuesRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository
}
