package com.quiquecx.simaapp.domain.repository

import com.quiquecx.simaapp.domain.entity.UserEntity

interface AuthRepository {
    suspend fun doLogin(user: String, password: String): UserEntity
}