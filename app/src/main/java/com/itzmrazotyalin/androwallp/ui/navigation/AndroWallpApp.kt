package com.itzmrazotyalin.androwallp.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.itzmrazotyalin.androwallp.ui.screens.library.LibraryScreen
import com.itzmrazotyalin.androwallp.ui.screens.picker.PickerScreen
import com.itzmrazotyalin.androwallp.ui.screens.settings.SettingsScreen

@Composable
fun AndroWallpApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = when (backStackEntry?.destination?.route) {
        AppDestination.IMPORT.route, EDIT_ROUTE -> AppDestination.IMPORT
        AppDestination.SETTINGS.route -> AppDestination.SETTINGS
        else -> AppDestination.LIBRARY
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        bottomBar = {
            AppBottomBar(
                currentDestination = currentDestination,
                onNavigate = { destination ->
                    navController.navigateTopLevel(destination.route)
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.LIBRARY.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(route = AppDestination.LIBRARY.route) {
                LibraryScreen(
                    onImportClick = { navController.navigateTopLevel(AppDestination.IMPORT.route) },
                    onEditClick = { wallpaperId ->
                        navController.navigate(AppDestination.editRoute(wallpaperId))
                    },
                )
            }
            composable(route = AppDestination.IMPORT.route) {
                PickerScreen(
                    wallpaperId = "",
                    onSaved = { navController.navigateTopLevel(AppDestination.LIBRARY.route) },
                )
            }
            composable(
                route = EDIT_ROUTE,
                arguments = listOf(
                    navArgument(WALLPAPER_ID_ARG) { type = NavType.StringType },
                ),
            ) { entry ->
                val wallpaperId = entry.arguments?.getString(WALLPAPER_ID_ARG).orEmpty()
                PickerScreen(
                    wallpaperId = wallpaperId,
                    onSaved = { navController.navigateTopLevel(AppDestination.LIBRARY.route) },
                )
            }
            composable(route = AppDestination.SETTINGS.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
) {
    NavigationBar {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == currentDestination,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(destination.labelRes)) },
                alwaysShowLabel = true,
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = false }
        launchSingleTop = true
    }
}