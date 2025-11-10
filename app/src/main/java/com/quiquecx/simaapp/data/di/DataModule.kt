package com.quiquecx.simaapp.data.di


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.firebase.auth.FirebaseAuth
import com.quiquecx.simaapp.data.datastore.SelectedCompanyDataStoreImpl
import com.quiquecx.simaapp.data.repository.AuthRepositoryImpl
import com.quiquecx.simaapp.domain.repository.AuthRepository
import com.quiquecx.simaapp.domain.repository.SelectedCompanyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // Provee FirebaseAuth
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    // Provee AuthRepository inyectando FirebaseAuth (mantengo tu implementación original)
    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth)
    }

    // Provee DataStore<Preferences>
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("sima_prefs")
        }
    }

    // Provee SelectedCompanyRepository usando DataStore
    @Provides
    @Singleton
    fun provideSelectedCompanyRepository(
        dataStore: DataStore<Preferences>
    ): SelectedCompanyRepository {
        return SelectedCompanyDataStoreImpl(dataStore)
    }
}