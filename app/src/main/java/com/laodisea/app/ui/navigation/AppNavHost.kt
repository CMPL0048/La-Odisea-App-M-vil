package com.laodisea.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.laodisea.app.ui.auth.ForgotPasswordScreen
import com.laodisea.app.ui.auth.LoginScreen
import com.laodisea.app.ui.auth.RegisterScreen
import com.laodisea.app.ui.auth.ResetPasswordScreen
import com.laodisea.app.ui.main.MainScreen
import com.laodisea.app.ui.splash.SessionTransitionScreen
import com.laodisea.app.ui.splash.SplashScreen
import kotlinx.serialization.Serializable

@Serializable data object SplashRoute
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
/** [email] llega prellenado si el jugador ya lo había escrito en el inicio de sesión. */
@Serializable data class ForgotPasswordRoute(val email: String = "")
@Serializable data class ResetPasswordRoute(val email: String)
@Serializable data class SessionTransitionRoute(val signingIn: Boolean)
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
                onSignedIn = { navController.replaceAll(SessionTransitionRoute(signingIn = true)) },
                onRegister = { navController.navigate(RegisterRoute) },
                onForgotPassword = { email -> navController.navigate(ForgotPasswordRoute(email)) },
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onSignedIn = { navController.replaceAll(SessionTransitionRoute(signingIn = true)) },
                onBack = { navController.popBackStack() },
            )
        }
        // Los ViewModels de estas dos pantallas leen el correo de los argumentos de la ruta.
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                onCodeSent = { email -> navController.navigate(ResetPasswordRoute(email)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<ResetPasswordRoute> {
            ResetPasswordScreen(
                onSignedIn = { navController.replaceAll(SessionTransitionRoute(signingIn = true)) },
                onBack = { navController.popBackStack() },
                onSignedOut = { navController.replaceAll(LoginRoute) },
            )
        }
        composable<SessionTransitionRoute> { entry ->
            val signingIn = entry.toRoute<SessionTransitionRoute>().signingIn
            SessionTransitionScreen(
                onFinished = { navController.replaceAll(if (signingIn) MainRoute else LoginRoute) },
            )
        }
        composable<MainRoute> {
            MainScreen(onSignedOut = { navController.replaceAll(SessionTransitionRoute(signingIn = false)) })
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
