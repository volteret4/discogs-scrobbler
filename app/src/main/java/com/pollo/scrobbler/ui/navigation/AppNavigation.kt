package com.pollo.scrobbler.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pollo.scrobbler.ui.screens.detail.DetailViewModel
import com.pollo.scrobbler.ui.screens.auth.AuthScreen
import com.pollo.scrobbler.ui.screens.auth.AuthViewModel
import com.pollo.scrobbler.ui.screens.busqueda.BusquedaScreen
import com.pollo.scrobbler.ui.screens.collection.CollectionScreen
import com.pollo.scrobbler.ui.screens.detail.DetailScreen
import com.pollo.scrobbler.ui.screens.mbzsearch.MbzSearchScreen
import com.pollo.scrobbler.ui.screens.scrobbles.RecentScrobblesScreen
import com.pollo.scrobbler.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    val bottomNavItems = listOf(
        Triple(Screen.Collection, Icons.Filled.Home, "Colección"),
        Triple(Screen.Busqueda, Icons.Filled.Search, "Búsqueda"),
        Triple(Screen.Scrobbles, Icons.Filled.History, "Scrobbles"),
        Triple(Screen.Settings, Icons.Filled.Settings, "Ajustes"),
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomNavItems.any {
                currentDestination?.hierarchy?.any { d -> d.route == it.first.route } == true
            }
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthenticated = {
                        navController.navigate(Screen.Collection.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Collection.route) {
                CollectionScreen(
                    onReleaseClick = { releaseId ->
                        navController.navigate(Screen.Detail.createRoute(releaseId))
                    }
                )
            }
            composable(Screen.Busqueda.route) {
                BusquedaScreen(
                    onReleaseClick = { releaseId ->
                        navController.navigate(Screen.Detail.createRoute(releaseId))
                    }
                )
            }
            composable(Screen.Scrobbles.route) {
                RecentScrobblesScreen()
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("releaseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val releaseId = backStackEntry.arguments?.getInt("releaseId") ?: return@composable
                val detailVm: DetailViewModel = hiltViewModel()
                // Reload when returning from MbzSearch after saving MBIDs
                val mbidsUpdated by backStackEntry.savedStateHandle
                    .getStateFlow("mbids_updated", false)
                    .collectAsState()
                androidx.compose.runtime.LaunchedEffect(mbidsUpdated) {
                    if (mbidsUpdated) {
                        detailVm.forceReload(releaseId)
                        backStackEntry.savedStateHandle["mbids_updated"] = false
                    }
                }
                DetailScreen(
                    releaseId = releaseId,
                    viewModel = detailVm,
                    onBack = { navController.popBackStack() },
                    onMbzSearch = { navController.navigate(Screen.MbzSearch.createRoute(releaseId)) }
                )
            }
            composable(
                route = Screen.MbzSearch.route,
                arguments = listOf(navArgument("releaseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val releaseId = backStackEntry.arguments?.getInt("releaseId") ?: return@composable
                MbzSearchScreen(
                    releaseId = releaseId,
                    onBack = { navController.popBackStack() },
                    onMbidsSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("mbids_updated", true)
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = authViewModel)
            }
        }
    }
}
