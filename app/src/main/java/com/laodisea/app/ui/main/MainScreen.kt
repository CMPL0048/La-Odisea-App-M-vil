package com.laodisea.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.laodisea.app.ui.backpack.BackpackScreen
import com.laodisea.app.ui.backpack.ItemDetailScreen
import com.laodisea.app.ui.backpack.ShowcaseScreen
import com.laodisea.app.ui.bestiary.BestiaryScreen
import com.laodisea.app.ui.bestiary.CharacterDetailScreen
import com.laodisea.app.ui.components.MainSection
import com.laodisea.app.ui.components.OdiseaBottomBar
import com.laodisea.app.ui.home.HomeScreen
import com.laodisea.app.data.repository.MinigameRepository
import com.laodisea.app.data.repository.MinigameReward
import com.laodisea.app.ui.map.MapScreen
import com.laodisea.app.ui.minigames.MinigamesScreen
import com.laodisea.app.ui.minigames.RewardScreen
import com.laodisea.app.ui.minigames.SalvaScreen
import com.laodisea.app.ui.minigames.VicenteScreen
import com.laodisea.app.ui.more.MoreScreen
import com.laodisea.app.ui.settings.SettingsScreen
import com.laodisea.app.ui.stats.StatsScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

// Un grafo por sección, para que la barra inferior marque la sección correcta también en los detalles.
@Serializable data object HomeGraph
@Serializable data object MapGraph
@Serializable data object BackpackGraph
@Serializable data object BestiaryGraph
@Serializable data object MoreGraph

@Serializable data object HomeRoute
@Serializable data object StatsRoute
@Serializable data object MinigamesRoute
@Serializable data object SalvaRoute
@Serializable data object VicenteRoute
@Serializable data class RewardRoute(val code: String, val success: Boolean, val coinsDelta: Long, val itemCode: String? = null)
@Serializable data object MapRoute
@Serializable data object BackpackRoute
@Serializable data class ItemDetailRoute(val code: String)
@Serializable data object ShowcaseRoute
@Serializable data object BestiaryRoute
@Serializable data class CharacterDetailRoute(val code: String)
@Serializable data object MoreRoute
@Serializable data object SettingsRoute
@Serializable data object MoreStatsRoute

private fun MainSection.graph(): Any = when (this) {
    MainSection.Inicio -> HomeGraph
    MainSection.Mapa -> MapGraph
    MainSection.Mochila -> BackpackGraph
    MainSection.Bestiario -> BestiaryGraph
    MainSection.Mas -> MoreGraph
}

private fun MainSection.graphClass(): KClass<*> = graph()::class

private fun minigameRoute(code: String): Any =
    if (code == MinigameRepository.CODE_VICENTE) VicenteRoute else SalvaRoute

/** Contenedor de las 5 secciones con la barra inferior. */
@Composable
fun MainScreen(onSignedOut: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val hierarchy = backStackEntry?.destination?.hierarchy
    val selected = MainSection.entries.firstOrNull { section ->
        hierarchy?.any { it.hasRoute(section.graphClass()) } == true
    } ?: MainSection.Inicio

    Scaffold(
        bottomBar = {
            OdiseaBottomBar(
                selected = selected,
                onSelect = { section ->
                    navController.navigate(section.graph()) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { padding ->
        val back: () -> Unit = { navController.popBackStack() }
        NavHost(
            navController = navController,
            startDestination = HomeGraph,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            navigation<HomeGraph>(startDestination = HomeRoute) {
                composable<HomeRoute> {
                    HomeScreen(
                        onOpenStats = { navController.navigate(StatsRoute) },
                        onOpenMinigames = { navController.navigate(MinigamesRoute) },
                    )
                }
                composable<StatsRoute> { StatsScreen(onBack = back) }
                composable<MinigamesRoute> {
                    MinigamesScreen(onBack = back, onPlay = { code -> navController.navigate(minigameRoute(code)) })
                }
                // Al terminar una partida se reemplaza el juego por la recompensa (atrás vuelve a la lista).
                val showReward: (String, MinigameReward) -> Unit = { code, reward ->
                    navController.navigate(RewardRoute(code, reward.success, reward.coinsDelta, reward.itemCode)) {
                        popUpTo<MinigamesRoute>()
                    }
                }
                composable<SalvaRoute> {
                    SalvaScreen(onBack = back, onFinished = { showReward(MinigameRepository.CODE_SALVA, it) })
                }
                composable<VicenteRoute> {
                    VicenteScreen(onBack = back, onFinished = { showReward(MinigameRepository.CODE_VICENTE, it) })
                }
                composable<RewardRoute> { entry ->
                    val route = entry.toRoute<RewardRoute>()
                    RewardScreen(
                        code = route.code,
                        success = route.success,
                        coinsDelta = route.coinsDelta,
                        itemCode = route.itemCode,
                        onPlayAgain = {
                            navController.navigate(minigameRoute(route.code)) { popUpTo<MinigamesRoute>() }
                        },
                        onDone = { navController.popBackStack<MinigamesRoute>(inclusive = false) },
                    )
                }
            }
            navigation<MapGraph>(startDestination = MapRoute) {
                composable<MapRoute> { MapScreen() }
            }
            navigation<BackpackGraph>(startDestination = BackpackRoute) {
                composable<BackpackRoute> {
                    BackpackScreen(
                        onOpenItem = { navController.navigate(ItemDetailRoute(it)) },
                        onOpenShowcase = { navController.navigate(ShowcaseRoute) },
                    )
                }
                composable<ItemDetailRoute> { entry ->
                    ItemDetailScreen(code = entry.toRoute<ItemDetailRoute>().code, onBack = back)
                }
                composable<ShowcaseRoute> {
                    ShowcaseScreen(onBack = back, onOpenItem = { navController.navigate(ItemDetailRoute(it)) })
                }
            }
            navigation<BestiaryGraph>(startDestination = BestiaryRoute) {
                composable<BestiaryRoute> {
                    BestiaryScreen(onOpenCharacter = { navController.navigate(CharacterDetailRoute(it)) })
                }
                composable<CharacterDetailRoute> { entry ->
                    CharacterDetailScreen(code = entry.toRoute<CharacterDetailRoute>().code, onBack = back)
                }
            }
            navigation<MoreGraph>(startDestination = MoreRoute) {
                composable<MoreRoute> {
                    MoreScreen(
                        onSignedOut = onSignedOut,
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onOpenStats = { navController.navigate(MoreStatsRoute) },
                    )
                }
                composable<SettingsRoute> { SettingsScreen(onBack = back) }
                composable<MoreStatsRoute> { StatsScreen(onBack = back) }
            }
        }
    }
}
