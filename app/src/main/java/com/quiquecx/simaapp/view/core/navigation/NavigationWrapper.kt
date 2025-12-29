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
import com.quiquecx.simaapp.view.dashboard.IncomingDashboardViewModel
import com.quiquecx.simaapp.view.dashboard.ActivityFormScreen
import com.quiquecx.simaapp.view.dashboard.ActivityFormViewModel
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

    // Ruta dinámica para el Dashboard
    const val DASHBOARD_BASE = "dashboard"
    const val DASHBOARD = "$DASHBOARD_BASE/{projectId}"

    // Ruta dinámica para el Formulario (Corregida)
    const val ACTIVITY_FORM_BASE = "activity_form"
    const val ACTIVITY_FORM = "$ACTIVITY_FORM_BASE/{projectId}"

    const val ACTIVITY_DETAILS_BASE = "activity_details"
    const val ACTIVITY_DETAILS = "$ACTIVITY_DETAILS_BASE/{activityId}"

    fun dashboardPath(projectId: String): String = "$DASHBOARD_BASE/$projectId"

    // Generador de ruta para el formulario
    fun activityFormPath(projectId: String): String = "$ACTIVITY_FORM_BASE/$projectId"

    fun activityDetailsPath(activityId: String): String {
        val encodedId = URLEncoder.encode(activityId, StandardCharsets.UTF_8.toString())
        return "$ACTIVITY_DETAILS_BASE/$encodedId"
    }
}

@Composable
fun NavigationWrapper() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = SimaRoutes.SPLASH) {

        // 1. SPLASH SCREEN
        composable(SimaRoutes.SPLASH) {
            val viewModel: SplashViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                if (viewModel.isUserAuthenticated()) {
                    navController.navigate(SimaRoutes.COMPANY_SELECTION) {
                        popUpTo(SimaRoutes.SPLASH) { inclusive = true }
                    }
                } else {
                    navController.navigate(SimaRoutes.LOGIN) {
                        popUpTo(SimaRoutes.SPLASH) { inclusive = true }
                    }
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // 2. MÓDULO DE AUTENTICACIÓN
        composable(SimaRoutes.LOGIN) {
            LoginScreen(
                navigateToRegister = { navController.navigate(SimaRoutes.REGISTER) },
                navigateToCompanySelection = {
                    navController.navigate(SimaRoutes.COMPANY_SELECTION) {
                        popUpTo(SimaRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(SimaRoutes.REGISTER) {
            RegisterScreen(navigateBack = { navController.popBackStack() })
        }

        // 3. SELECCIÓN DE EMPRESA Y HOME
        composable(SimaRoutes.COMPANY_SELECTION) {
            // Ahora sí reconocerá la función porque el import es correcto
            CompanySelectionScreen(
                onNavigateToHome = {
                    navController.navigate(SimaRoutes.HOME) {
                        popUpTo(SimaRoutes.COMPANY_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        composable(SimaRoutes.HOME) {
            HomeScreen(
                onSignOut = {
                    navController.navigate(SimaRoutes.LOGIN) {
                        popUpTo(SimaRoutes.HOME) { inclusive = true }
                    }
                },
                onProjectClick = { projectId ->
                    navController.navigate(SimaRoutes.dashboardPath(projectId))
                }
            )
        }

        // 4. MÓDULO DE DASHBOARD UNIVERSAL
        composable(
            route = SimaRoutes.DASHBOARD,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val viewModel: IncomingDashboardViewModel = hiltViewModel()

            LaunchedEffect(projectId) {
                viewModel.initProject(projectId)
            }

            IncomingDashboardScreen(
                viewModel = viewModel,
                onNavigateToDetails = { activityId ->
                    navController.navigate(SimaRoutes.activityDetailsPath(activityId))
                },
                onNavigateToCreate = {
                    // Navegación corregida pasando el projectId
                    navController.navigate(SimaRoutes.activityFormPath(projectId))
                },
                onBack = {
                    // Esto cerrará el dashboard y volverá a la pantalla anterior (ej. Selección de Proyecto)
                    navController.popBackStack()
                }
            )
        }

        // 5. FORMULARIO DE ACTIVIDAD (Corregido con argumentos)
        composable(
            route = SimaRoutes.ACTIVITY_FORM,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val viewModel: ActivityFormViewModel = hiltViewModel()

            // Inicializamos el ViewModel con el ID del proyecto recibido
            LaunchedEffect(projectId) {
                viewModel.initProject(projectId)
            }

            ActivityFormScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 6. DETALLES DE ACTIVIDAD
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