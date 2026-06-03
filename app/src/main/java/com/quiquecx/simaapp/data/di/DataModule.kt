package com.quiquecx.simaapp.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quiquecx.simaapp.data.datastore.SelectedCompanyDataStoreImpl
import com.quiquecx.simaapp.data.repository.AuthRepositoryImpl
import com.quiquecx.simaapp.data.repository.CompanyRepositoryImpl
import com.quiquecx.simaapp.data.repository.DashboardRepositoryImpl
import com.quiquecx.simaapp.data.repository.ProjectsRepositoryImpl
import com.quiquecx.simaapp.data.repository.ReportRepositoryImpl
import com.quiquecx.simaapp.domain.repository.AuthRepository
import com.quiquecx.simaapp.domain.repository.CompanyRepository
import com.quiquecx.simaapp.domain.repository.DashboardRepository
import com.quiquecx.simaapp.domain.repository.ProjectsRepository
import com.quiquecx.simaapp.domain.repository.ReportRepository
import com.quiquecx.simaapp.domain.repository.SelectedCompanyRepository
import com.quiquecx.simaapp.domain.useCase.EnrichActivitiesForReportUseCase
import com.quiquecx.simaapp.utils.CsvGenerator
import com.quiquecx.simaapp.utils.FileShareHelper
import com.quiquecx.simaapp.utils.HtmlGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    // 1. Proveer FirebaseAuth
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    // 2. Proveer Firestore
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // 3. Proveer AuthRepository
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth, firestore)
    }

    // 4. Proveer DataStore
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("sima_prefs")
        }
    }

    // 5. Proveer SelectedCompanyRepository
    @Provides
    @Singleton
    fun provideSelectedCompanyRepository(
        dataStore: DataStore<Preferences>
    ): SelectedCompanyRepository {
        return SelectedCompanyDataStoreImpl(dataStore)
    }

    @Provides
    @Singleton
    fun provideProjectsRepository(firestore: FirebaseFirestore): ProjectsRepository {
        return ProjectsRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideCompanyRepository(firestore: FirebaseFirestore): CompanyRepository {
        return CompanyRepositoryImpl(firestore)
    }

    @Provides
    @Singleton
    fun provideDashboardRepository(firestore: FirebaseFirestore): DashboardRepository {
        return DashboardRepositoryImpl(firestore)
    }

    // ✅ Proveer HtmlGenerator
    @Provides
    @Singleton
    fun provideHtmlGenerator(@ApplicationContext context: Context): HtmlGenerator {
        return HtmlGenerator(context)
    }

    // ✅ Proveer CsvGenerator
    @Provides
    @Singleton
    fun provideCsvGenerator(@ApplicationContext context: Context): CsvGenerator {
        return CsvGenerator(context)
    }

    // ✅ Proveer FileShareHelper
    @Provides
    @Singleton
    fun provideFileShareHelper(): FileShareHelper {
        return FileShareHelper()
    }

    // ✅ Proveer ReportRepository con todas sus dependencias
    @Provides
    @Singleton
    fun provideReportRepository(
        firestore: FirebaseFirestore,
        htmlGenerator: HtmlGenerator,
        csvGenerator: CsvGenerator,
        fileShareHelper: FileShareHelper
    ): ReportRepository {
        return ReportRepositoryImpl(firestore, htmlGenerator, csvGenerator, fileShareHelper)
    }

    @Provides
    @Singleton
    fun provideEnrichActivitiesUseCase(
        dashboardRepository: DashboardRepository,
        firestore: FirebaseFirestore
    ): EnrichActivitiesForReportUseCase {
        return EnrichActivitiesForReportUseCase(dashboardRepository, firestore)
    }
}
