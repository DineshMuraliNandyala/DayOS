package com.lifeos.ui.navigation

/**
 * Sealed class enumerating every top-level navigation destination.
 * Using a sealed class (not an enum) so future destinations can carry
 * typed arguments without a route-string append hack.
 */
sealed class Screen(val route: String) {
    data object Today : Screen("today")
    data object Placement : Screen("placement")
    data object Fitness : Screen("fitness")
    data object Notes : Screen("notes")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
}
