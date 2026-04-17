package com.pollo.scrobbler.ui.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Collection : Screen("collection")
    data object Busqueda : Screen("busqueda")
    data object Scrobbles : Screen("scrobbles")
    data object Settings : Screen("settings")
    data object Detail : Screen("detail/{releaseId}") {
        fun createRoute(releaseId: Int) = "detail/$releaseId"
    }
    data object MbzSearch : Screen("mbzsearch/{releaseId}") {
        fun createRoute(releaseId: Int) = "mbzsearch/$releaseId"
    }
}
