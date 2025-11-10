package com.quiquecx.simaapp.view.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quiquecx.simaapp.view.auth.login.LoginScreen
import com.quiquecx.simaapp.view.auth.register.RegisterScreen
import com.quiquecx.simaapp.view.selectcompany.CompanySelectionScreen

@Composable
fun NavigationWrapper() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        //Pantalla de Login
        composable("login") {
            LoginScreen(
                navigateToRegister = { navController.navigate("register") },
                navigateToCompanySelection = { navController.navigate("companySelection") }
            )
        }

        //Pantalla de Registro
        composable("register") {
            RegisterScreen(
                navigateBack = { navController.popBackStack() }
            )
        }

        // Pantalla de Selección de Empresa
        composable("companySelection") {
            // Aquí podrías pasar una lista fija o desde tu LoginViewModel
            CompanySelectionScreen(
                companiesFromLogin = listOf("BorgWarner", "HELLA", "CHENSON"),
                onNavigateToHome = {
                    // 🔸 Aquí iría tu navegación hacia el Home real
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
    }
}

