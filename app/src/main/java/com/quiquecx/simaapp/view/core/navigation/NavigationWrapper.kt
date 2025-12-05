package com.quiquecx.simaapp.view.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavBackStackEntry
import androidx.hilt.navigation.compose.hiltViewModel
import com.quiquecx.simaapp.view.core.SplashViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Pantallas
import com.quiquecx.simaapp.view.auth.login.LoginScreen
import com.quiquecx.simaapp.view.auth.register.RegisterScreen
import com.quiquecx.simaapp.view.home.HomeScreen
import com.quiquecx.simaapp.view.selectcompany.CompanySelectionScreen
import com.quiquecx.simaapp.view.dashboard.IncomingDashboardScreen
import com.quiquecx.simaapp.view.dashboard.ActivityFormScreen
import com.quiquecx.simaapp.view.dashboard.ActivityDetailsScreen
import com.quiquecx.simaapp.view.dashboard.ActivityDetailsViewModel


/**
 * Objeto que centraliza todas las rutas de navegación de la aplicación SIMA.
 */
object SimaRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val COMPANY_SELECTION = "companySelection"
    const val HOME = "home"

    const val DASHBOARD_INCOMING = "dashboard_incoming"
    const val ACTIVITY_FORM = "activity_form"
    const val ACTIVITY_DETAILS_BASE = "activity_details"
    const val ACTIVITY_DETAILS = "$ACTIVITY_DETAILS_BASE/{activityId}"

    /**
     * Genera la ruta completa para navegar a los detalles de una actividad,
     * codificando el ID para su uso seguro en la URL.
     */
    fun activityDetailsPath(activityId: String): String {
        val encodedId = URLEncoder.encode(activityId, StandardCharsets.UTF_8.toString())
        return "$ACTIVITY_DETAILS_BASE/$encodedId"
    }
}

/**
 * Contenedor principal de navegación que define el NavHost y todas las transiciones.
 * Gestiona el flujo de la aplicación incluyendo la persistencia de la sesión.
 */
@Composable
fun NavigationWrapper() {
    val navController: NavHostController = rememberNavController()

    // El punto de partida ahora es SPLASH para la verificación de autenticación
    NavHost(navController = navController, startDestination = SimaRoutes.SPLASH) {

        // ---------------------------------------------------------------------
        // 1. SPLASH SCREEN (VERIFICACIÓN DE SESIÓN)
        // ---------------------------------------------------------------------
        composable(SimaRoutes.SPLASH) {
            val viewModel: SplashViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                if (viewModel.isUserAuthenticated()) {
                    // Si está autenticado, salta a la selección de empresas
                    navController.navigate(SimaRoutes.COMPANY_SELECTION) {
                        // Limpia la pila para evitar regresar al splash o login
                        popUpTo(SimaRoutes.SPLASH) { inclusive = true }
                    }
                } else {
                    // Si no está autenticado, va a la pantalla de Login
                    navController.navigate(SimaRoutes.LOGIN) {
                        popUpTo(SimaRoutes.SPLASH) { inclusive = true }
                    }
                }
            }
            // Muestra un indicador de carga mientras verifica la sesión
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // ---------------------------------------------------------------------
        // 2. MÓDULO DE AUTENTICACIÓN
        // ---------------------------------------------------------------------
        composable(SimaRoutes.LOGIN) {
            LoginScreen(
                navigateToRegister = { navController.navigate(SimaRoutes.REGISTER) },
                navigateToCompanySelection = {
                    // Tras el login exitoso, navegar a la selección de empresa y limpiar la pila
                    navController.navigate(SimaRoutes.COMPANY_SELECTION) {
                        popUpTo(SimaRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(SimaRoutes.REGISTER) {
            RegisterScreen(navigateBack = { navController.popBackStack() })
        }

        // ---------------------------------------------------------------------
        // 3. SELECCIÓN DE EMPRESA Y HOME
        // ---------------------------------------------------------------------
        composable(SimaRoutes.COMPANY_SELECTION) {
            CompanySelectionScreen(
                onNavigateToHome = {
                    // Tras seleccionar empresa, navegar al HOME y limpiar la pila
                    navController.navigate(SimaRoutes.HOME) {
                        popUpTo(SimaRoutes.COMPANY_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        composable(SimaRoutes.HOME) {
            HomeScreen(
                // 🚨 FUNCIÓN DE LOGOUT: Regresa al Login y limpia la pila para cerrar sesión de forma segura
                onSignOut = {
                    navController.navigate(SimaRoutes.LOGIN) {
                        // Elimina todas las pantallas (incluyendo HOME y COMPANY_SELECTION)
                        popUpTo(SimaRoutes.HOME) { inclusive = true }
                    }
                },
                onProjectClick = { projectId ->
                    if (projectId == "proy_incoming") {
                        navController.navigate(SimaRoutes.DASHBOARD_INCOMING)
                    }
                }
            )
        }

        // ---------------------------------------------------------------------
        // 4. MÓDULO DE DASHBOARD (INCOMING)
        // ---------------------------------------------------------------------
        composable(SimaRoutes.DASHBOARD_INCOMING) {
            IncomingDashboardScreen(
                onNavigateToDetails = { activityId ->
                    navController.navigate(SimaRoutes.activityDetailsPath(activityId))
                },
                onNavigateToCreate = {
                    navController.navigate(SimaRoutes.ACTIVITY_FORM)
                }
            )
        }

        composable(SimaRoutes.ACTIVITY_FORM) {
            ActivityFormScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = SimaRoutes.ACTIVITY_DETAILS,
            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
        ) { backStackEntry: NavBackStackEntry ->
            val vm: ActivityDetailsViewModel = hiltViewModel(backStackEntry)
            ActivityDetailsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}