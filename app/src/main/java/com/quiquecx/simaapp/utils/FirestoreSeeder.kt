package com.quiquecx.simaapp.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreSeeder @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun seedInitialData() {
        val user = auth.currentUser ?: return
        val batch = firestore.batch()

        // 1. Definir IDs fijos para relacionarlos fácilmente
        val idBorg = "emp_borgwarner"
        val idHella = "emp_hella"
        val idChenson = "emp_chenson"

        // 2. Crear Empresas
        val listEmpresas = listOf(
            hashMapOf("id" to idBorg, "nombre" to "BorgWarner", "responsable" to "Gerente Planta 1"),
            hashMapOf("id" to idHella, "nombre" to "HELLA", "responsable" to "Gerente Planta 2"),
            hashMapOf("id" to idChenson, "nombre" to "CHENSON", "responsable" to "Gerente Planta 3")
        )

        listEmpresas.forEach { data ->
            val ref = firestore.collection("Empresas").document(data["id"] as String)
            batch.set(ref, data)
        }

        // 3. Crear Proyectos de BorgWarner (Incoming, Cadenas, VCTS)
        val proyectosBorg = listOf(
            hashMapOf(
                "id" to "proy_incoming",
                "empresaId" to idBorg,
                "nombre" to "Incoming",
                "descripcion" to "Control de Sorteos y Retrabajos",
                "estado" to "Activo",
                "imageType" to "incoming" // 👈 Clave para la UI
            ),
            hashMapOf(
                "id" to "proy_cadenas",
                "empresaId" to idBorg,
                "nombre" to "Cadenas",
                "descripcion" to "Gestión de línea de cadenas",
                "estado" to "Activo",
                "imageType" to "cadenas"
            ),
            hashMapOf(
                "id" to "proy_vcts",
                "empresaId" to idBorg,
                "nombre" to "VCTS",
                "descripcion" to "Validación técnica",
                "estado" to "Activo",
                "imageType" to "vcts"
            )
        )

        proyectosBorg.forEach { data ->
            val ref = firestore.collection("Proyectos").document(data["id"] as String)
            batch.set(ref, data)
        }

        // 4. Asignar TU usuario a BorgWarner para que veas estos proyectos
        val userRef = firestore.collection("Users").document(user.uid)
        batch.update(userRef, "empresaId", idBorg)

        try {
            batch.commit().await()
            Log.d("Seeder", "✅ DATOS REALES (BorgWarner, Incoming...) CARGADOS")
        } catch (e: Exception) {
            Log.e("Seeder", "❌ Error: ${e.message}")
        }
    }
}