package com.quiquecx.simaapp.view.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavBackStackEntry
import androidx.hilt.navigation.compose.hiltViewModel
import java.net.URLEncoder
import java.net.URLDecoder
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

object SimaRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val COMPANY_SELECTION = "companySelection"
    const val HOME = "home"

    const val DASHBOARD_INCOMING = "dashboard_incoming"
    const val ACTIVITY_FORM = "activity_form"
    const val ACTIVITY_DETAILS_BASE = "activity_details"
    const val ACTIVITY_DETAILS = "$ACTIVITY_DETAILS_BASE/{activityId}"

    fun activityDetailsPath(activityId: String): String {
        return "$ACTIVITY_DETAILS_BASE/$activityId"
    }
}

@Composable
fun NavigationWrapper() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = SimaRoutes.LOGIN) {
        composable(SimaRoutes.LOGIN) {
            LoginScreen(
                navigateToRegister = { navController.navigate(SimaRoutes.REGISTER) },
                navigateToCompanySelection = { navController.navigate(SimaRoutes.COMPANY_SELECTION) }
            )
        }

        composable(SimaRoutes.REGISTER) {
            RegisterScreen(navigateBack = { navController.popBackStack() })
        }

        composable(SimaRoutes.COMPANY_SELECTION) {
            CompanySelectionScreen(
                onNavigateToHome = {
                    navController.navigate(SimaRoutes.HOME) {
                        popUpTo(SimaRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(SimaRoutes.HOME) {
            HomeScreen(
                onProjectClick = { projectId ->
                    if (projectId == "proy_incoming") {
                        navController.navigate(SimaRoutes.DASHBOARD_INCOMING)
                    }
                }
            )
        }

        composable(SimaRoutes.DASHBOARD_INCOMING) {
            IncomingDashboardScreen(
                onNavigateToDetails = { activityId ->
                    // Codificamos para evitar caracteres especiales en la ruta
                    val encoded = URLEncoder.encode(activityId, StandardCharsets.UTF_8.toString())
                    navController.navigate(SimaRoutes.activityDetailsPath(encoded))
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
            // Creamos el ViewModel con el backStackEntry para que SavedStateHandle tenga activityId
            val vm: ActivityDetailsViewModel = hiltViewModel(backStackEntry)
            // ActivityDetailsViewModel debería decodificar el id si fue codificado en la ruta
            ActivityDetailsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}