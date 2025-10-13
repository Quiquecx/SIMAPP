package com.quiquecx.simaapp.data.response
import com.quiquecx.simaapp.domain.entity.UserEntity
import com.quiquecx.simaapp.domain.entity.UserMode


data class UserResponse(
    val uid: String = "",
    val email: String = "",
    val role: String = "",
    val name: String = "",
    val company: List<String>,
    val userType: Int
)

fun UserResponse.toDomain(): UserEntity {

    val userMode: UserMode = when(userType){
        UserMode.GENERAL_MANAGER.userType -> UserMode.GENERAL_MANAGER
        UserMode.SUPERVISOR.userType -> UserMode.SUPERVISOR
        UserMode.OPERADOR.userType -> UserMode.OPERADOR
        else -> UserMode.SUPERVISOR
    }

    return UserEntity(
        uid = uid,
        email = email,
        role = role,
        name = name,
        company = company,
        userMode = userMode
    )
}