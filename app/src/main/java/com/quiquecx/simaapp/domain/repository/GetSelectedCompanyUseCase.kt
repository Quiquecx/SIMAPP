package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.repository.SelectedCompanyRepository
import javax.inject.Inject // 👈 ¡ESTA IMPORTACIÓN ES CRUCIAL!

class GetSelectedCompanyUseCase @Inject constructor( // 👈 ¡LA ANOTACIÓN @Inject ES OBLIGATORIA!
    private val repository: SelectedCompanyRepository
) {
    /**
     * Devuelve el ID de la compañía seleccionada (de DataStore).
     */
    suspend operator fun invoke(): String? {
        // Asume que SelectedCompanyRepository tiene el método getSelectedCompanyId()
        return repository.getSelectedCompanyId()
    }
}