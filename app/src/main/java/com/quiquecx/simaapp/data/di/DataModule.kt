package com.quiquecx.simaapp.data.di

import com.quiquecx.simaapp.data.repository.AuthRepositoryImpl
import com.quiquecx.simaapp.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun provideAuthRepository(): AuthRepository {
        return AuthRepositoryImpl()

    }

}