package com.quiquecx.simaapp.view.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quiquecx.simaapp.view.auth.login.LoginScreen
import com.quiquecx.simaapp.view.auth.register.RegisterScreen
import com.quiquecx.simaapp.view.home.HomeScreen
import com.quiquecx.simaapp.view.selectcompany.CompanySelectionScreen

@Composable
fun NavigationWrapper() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        // Pantalla de Login
        composable("login") {
            LoginScreen(
                navigateToRegister = { navController.navigate("register") },
                navigateToCompanySelection = {
                    // 💡 Nota: Aquí deberías pasar datos importantes del login si es necesario,
                    // pero la lógica de la compañía ahora es interna del VM.
                    navController.navigate("companySelection")
                }
            )
        }

        // Pantalla de Registro
        composable("register") {
            RegisterScreen(
                navigateBack = { navController.popBackStack() }
            )
        }

        // Pantalla de Selección de Empresa (CORREGIDA)
        composable("companySelection") {
            CompanySelectionScreen(
                // 🛑 ELIMINAMOS EL ARGUMENTO 'companiesFromLogin'
                onNavigateToHome = {
                    // 🔸 Navegación hacia Home, eliminando el historial de login
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Pantalla Home
        composable("home") {
            HomeScreen(
                onProjectClick = { projectId ->
                    // Aquí navegaremos a las Actividades más adelante
                    // navController.navigate("activities/$projectId")
                }
            )
        }
    }
}
