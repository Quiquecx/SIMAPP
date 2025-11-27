package com.quiquecx.simaapp.domain.repository

import com.quiquecx.simaapp.domain.entity.CompanyEntity

interface CompanyRepository {
    // ✅ Solo para obtener la lista de empresas de Firestore
    suspend fun getAllCompanies(): List<CompanyEntity>
}