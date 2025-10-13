package com.quiquecx.simaapp.data.repository

import com.quiquecx.simaapp.data.response.UserResponse
import com.quiquecx.simaapp.data.response.toDomain
import com.quiquecx.simaapp.domain.entity.UserEntity
import com.quiquecx.simaapp.domain.repository.AuthRepository

class AuthRepositoryImpl: AuthRepository {
    override fun doLogin(user: String, password: String): UserEntity {
        val userResponse: UserResponse = UserResponse("","","","", listOf(),0)

        return userResponse.toDomain()
    }

}