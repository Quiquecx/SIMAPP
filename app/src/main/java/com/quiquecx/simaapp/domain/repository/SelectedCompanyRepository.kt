package com.quiquecx.simaapp.domain.repository

interface SelectedCompanyRepository {
    // ✅ Solo para guardar/obtener el ID de DataStore
    suspend fun saveSelectedCompanyId(id: String)
    suspend fun getSelectedCompanyId(): String?

}

