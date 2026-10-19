package com.laodisea.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.laodisea.app.ui.auth.LoginScreen
import com.laodisea.app.ui.auth.RegisterScreen
import com.laodisea.app.ui.main.MainScreen
import com.laodisea.app.ui.splash.SplashScreen
import kotlinx.serialization.Serializable

@Serializable data object SplashRoute
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data object MainRoute

/** Navegación raíz: splash → sesión → secciones principales. */
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = SplashRoute) {
        composable<SplashRoute> {
            SplashScreen(
                onSignedIn = { navController.replaceAll(MainRoute) },
                onSignedOut = { navController.replaceAll(LoginRoute) },
            )
        }
        composable<LoginRoute> {
            LoginScreen(
                onSignedIn = { navController.replaceAll(MainRoute) },
                onRegister = { navController.navigate(RegisterRoute) },
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onSignedIn = { navController.replaceAll(MainRoute) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<MainRoute> {
            MainScreen(onSignedOut = { navController.replaceAll(LoginRoute) })
        }
    }
}

/** Navega a [route] dejando la pila solo con ese destino. */
private fun NavHostController.replaceAll(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
