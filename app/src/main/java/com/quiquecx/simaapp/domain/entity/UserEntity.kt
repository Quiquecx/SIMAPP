package com.quiquecx.simaapp.domain.entity

data class UserEntity (
    val uid: String = "",
    val email: String = "",
    val role: String = "",
    val name: String = "",
    val company: List<String>,
    val userMode: UserMode

)

sealed class UserMode(val userType: Int){
    data object GENERAL_MANAGER : UserMode(0)
    data object SUPERVISOR : UserMode(1)
    data object OPERADOR : UserMode(2)
}
