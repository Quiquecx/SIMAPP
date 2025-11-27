package com.quiquecx.simaapp.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quiquecx.simaapp.domain.repository.SelectedCompanyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SelectedCompanyDataStoreImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SelectedCompanyRepository {

    companion object {
        // La clave utilizada para guardar el ID de la empresa.
        val COMPANY_ID_KEY = stringPreferencesKey("selected_company_id")
    }

    /**
     * Guarda el ID de la empresa seleccionada en DataStore.
     */
    override suspend fun saveSelectedCompanyId(id: String) {
        dataStore.edit { preferences ->
            preferences[COMPANY_ID_KEY] = id
        }
    }

    /**
     * Obtiene el ID de la empresa seleccionada de DataStore como un String.
     */
    override suspend fun getSelectedCompanyId(): String? {
        return dataStore.data.map { preferences ->
            preferences[COMPANY_ID_KEY]
        }.first() // .first() obtiene el valor más reciente y termina la corrutina.
    }

}