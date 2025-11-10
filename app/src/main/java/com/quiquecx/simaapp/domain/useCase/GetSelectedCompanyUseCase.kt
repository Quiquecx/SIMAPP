package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.repository.SelectedCompanyRepository
import javax.inject.Inject

class GetSelectedCompanyUseCase @Inject constructor(
    private val repository: SelectedCompanyRepository
) {
    suspend operator fun invoke(): String? = repository.getSelectedCompany()
}


