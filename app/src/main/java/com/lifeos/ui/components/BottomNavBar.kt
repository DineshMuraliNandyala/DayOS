package com.lifeos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lifeos.ui.navigation.Screen

private data class NavTab(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

private val TABS = listOf(
    NavTab(Screen.Today, "Today", Icons.Outlined.WbSunny),
    NavTab(Screen.Placement, "Placement", Icons.Outlined.Terminal),
    NavTab(Screen.Fitness, "Fitness", Icons.Outlined.FitnessCenter),
    NavTab(Screen.Notes, "Notes", Icons.Outlined.MenuBook),
    NavTab(Screen.Analytics, "Stats", Icons.AutoMirrored.Outlined.ShowChart),
    NavTab(Screen.Settings, "Settings", Icons.Outlined.Settings),
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        TABS.forEach { tab ->
            val selected = currentRoute == tab.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.screen.route) {
                            // Pop up to the start destination to avoid a large back stack.
                            popUpTo(Screen.Today.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(imageVector = tab.icon, contentDescription = tab.label)
                },
                label = { Text(text = tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
