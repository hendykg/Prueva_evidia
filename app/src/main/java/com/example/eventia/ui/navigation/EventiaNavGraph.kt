package com.example.eventia.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eventia.ui.screens.AuthScreen
import com.example.eventia.ui.screens.EventDetailScreen
import com.example.eventia.ui.screens.ExploreScreen
import com.example.eventia.ui.screens.FavoritesScreen
import com.example.eventia.ui.screens.MapScreen
import com.example.eventia.ui.screens.ProfileScreen
import com.example.eventia.ui.screens.VenueDetailScreen

object Routes {
    const val EXPLORE = "explore"
    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val MAP = "map"
    const val AUTH = "auth"
    const val EVENT_DETAIL = "event_detail"
    const val VENUE_DETAIL = "venue_detail"
    const val EVENT_ID_ARG = "eventId"
    const val VENUE_ID_ARG = "venueId"
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

// Sección 5 del enunciado: barra inferior de cuatro destinos
// (Explorar, Mapa, Favoritos y Perfil); Explorar agrupa Eventos y Lugares.
private val bottomItems = listOf(
    BottomItem(Routes.EXPLORE, "Explorar", Icons.Filled.Home),
    BottomItem(Routes.MAP, "Mapa", Icons.Filled.LocationOn),
    BottomItem(Routes.FAVORITES, "Favoritos", Icons.Filled.Favorite),
    BottomItem(Routes.PROFILE, "Perfil", Icons.Filled.Person)
)

@Composable
fun EventiaNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.EXPLORE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.EXPLORE,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onEventClick = { eventId ->
                        navController.navigate("${Routes.EVENT_DETAIL}/$eventId")
                    },
                    onVenueClick = { venueId ->
                        navController.navigate("${Routes.VENUE_DETAIL}/$venueId")
                    }
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onEventClick = { eventId ->
                        navController.navigate("${Routes.EVENT_DETAIL}/$eventId")
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogin = { navController.navigate(Routes.AUTH) },
                    onEventClick = { eventId ->
                        navController.navigate("${Routes.EVENT_DETAIL}/$eventId")
                    }
                )
            }
            composable(Routes.MAP) {
                MapScreen(
                    onEventClick = { eventId ->
                        navController.navigate("${Routes.EVENT_DETAIL}/$eventId")
                    }
                )
            }
            composable(Routes.AUTH) {
                AuthScreen(
                    onBack = { navController.popBackStack() },
                    // Regresa a la pantalla anterior (perfil o el evento seleccionado)
                    onAuthSuccess = { navController.popBackStack() }
                )
            }
            composable(
                route = "${Routes.EVENT_DETAIL}/{${Routes.EVENT_ID_ARG}}",
                arguments = listOf(navArgument(Routes.EVENT_ID_ARG) { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString(Routes.EVENT_ID_ARG).orEmpty()
                EventDetailScreen(
                    eventId = eventId,
                    onBack = { navController.popBackStack() },
                    onVenueClick = { venueId ->
                        navController.navigate("${Routes.VENUE_DETAIL}/$venueId")
                    },
                    // RF-12: login y regreso al evento seleccionado
                    onRequireLogin = { navController.navigate(Routes.AUTH) }
                )
            }
            composable(
                route = "${Routes.VENUE_DETAIL}/{${Routes.VENUE_ID_ARG}}",
                arguments = listOf(navArgument(Routes.VENUE_ID_ARG) { type = NavType.StringType })
            ) { backStackEntry ->
                val venueId = backStackEntry.arguments?.getString(Routes.VENUE_ID_ARG).orEmpty()
                VenueDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEventClick = { eventId ->
                        navController.navigate("${Routes.EVENT_DETAIL}/$eventId")
                    }
                )
            }
        }
    }
}
