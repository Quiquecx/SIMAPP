package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.CompanyEntity
// ✅ Importar la interfaz de Firestore
import com.quiquecx.simaapp.domain.repository.CompanyRepository
import javax.inject.Inject

class GetCompaniesUseCase @Inject constructor(
    // ✅ Inyectar el tipo correcto: CompanyRepository
    private val repository: CompanyRepository
) {
    suspend operator fun invoke(): List<CompanyEntity> {
        return repository.getAllCompanies()
    }
}