package com.lifeos.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lifeos.ui.components.BottomNavBar
import com.lifeos.ui.screens.analytics.AnalyticsScreen
import com.lifeos.ui.screens.fitness.FitnessScreen
import com.lifeos.ui.screens.notes.NotesScreen
import com.lifeos.ui.screens.placement.PlacementScreen
import com.lifeos.ui.screens.settings.SettingsScreen
import com.lifeos.ui.screens.today.TodayScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavBar(navController = navController)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Today.route) { TodayScreen() }
            composable(Screen.Placement.route) { PlacementScreen() }
            composable(Screen.Fitness.route) { FitnessScreen() }
            composable(Screen.Notes.route) { NotesScreen() }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
