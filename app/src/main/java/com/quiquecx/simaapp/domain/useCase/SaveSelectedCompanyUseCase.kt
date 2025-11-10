package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.repository.SelectedCompanyRepository
import javax.inject.Inject

class SaveSelectedCompanyUseCase @Inject constructor(
    private val repository: SelectedCompanyRepository
) {
    suspend operator fun invoke(companyId: String) {
        repository.saveSelectedCompany(companyId)
    }
}