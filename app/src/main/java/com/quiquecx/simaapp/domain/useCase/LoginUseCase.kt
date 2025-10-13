package com.quiquecx.simaapp.domain.useCase

import com.quiquecx.simaapp.domain.entity.UserEntity
import com.quiquecx.simaapp.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor ( val authRepository: AuthRepository) {

    operator fun invoke(user: String, password: String){
        if (user.contains("@gmail")){
            return
        }
        val response: UserEntity = authRepository.doLogin(user, password)
    }
}