package com.quiquecx.simaapp.data.di

import com.google.firebase.auth.FirebaseAuth
import com.quiquecx.simaapp.data.repository.AuthRepositoryImpl
import com.quiquecx.simaapp.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // ✅ Provee una instancia de FirebaseAuth
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    // ✅ Provee AuthRepository inyectando FirebaseAuth
    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth)
    }
}
