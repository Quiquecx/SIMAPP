package com.quiquecx.simaapp.data.datastore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quiquecx.simaapp.domain.repository.SelectedCompanyRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SelectedCompanyDataStoreImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SelectedCompanyRepository {

    companion object {
        private val SELECTED_COMPANY_KEY = stringPreferencesKey("selected_company_id")
    }

    override suspend fun saveSelectedCompany(companyId: String) {
        dataStore.edit { prefs ->
            prefs[SELECTED_COMPANY_KEY] = companyId
        }
    }

    override suspend fun getSelectedCompany(): String? {
        return dataStore.data.first()[SELECTED_COMPANY_KEY]
    }
}