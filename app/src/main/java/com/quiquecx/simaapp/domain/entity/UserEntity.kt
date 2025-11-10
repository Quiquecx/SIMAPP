package com.quiquecx.simaapp.domain.entity

data class UserEntity (
    val uid: String = "",
    val email: String = "",
    val role: String = "",
    val name: String = "",
    val company: List<String> = emptyList(),
    val userMode: UserMode
)

sealed class UserMode(val userType: Int){
    object GENERAL_MANAGER : UserMode(0)
    object SUPERVISOR : UserMode(1)
    object OPERADOR : UserMode(2)
}