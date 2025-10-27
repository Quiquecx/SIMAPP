package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.UserEntity
import com.quiquecx.simaapp.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(user: String, password: String): UserEntity {
        return authRepository.doLogin(user, password)
    }
}
