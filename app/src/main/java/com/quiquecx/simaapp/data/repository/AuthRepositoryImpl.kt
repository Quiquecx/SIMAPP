package com.quiquecx.simaapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quiquecx.simaapp.data.response.UserResponse
import com.quiquecx.simaapp.data.response.toDomain
import com.quiquecx.simaapp.domain.entity.UserEntity
import com.quiquecx.simaapp.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun doLogin(user: String, password: String): UserEntity {
        return try {
            // Inicia sesión con Firebase y espera la respuesta
            val result = firebaseAuth.signInWithEmailAndPassword(user, password).await()
            val firebaseUser = result.user ?: throw Exception("No se encontró el usuario")

            // Construimos el UserResponse con tus campos
            val userResponse = UserResponse(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: user,
                role = "",
                name = firebaseUser.displayName ?: "",
                company = listOf(""),
                userType = 2 // 1 = Manager, 2 = Supervisor, 3 = Operador
            )

            userResponse.toDomain()

        } catch (e: Exception) {
            throw Exception("Error al iniciar sesión: ${e.message}")
        }
    }
}
