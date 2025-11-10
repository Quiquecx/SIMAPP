package com.quiquecx.simaapp.domain.repository

interface SelectedCompanyRepository {
    suspend fun saveSelectedCompany(companyId: String)
    suspend fun getSelectedCompany(): String?
}