package com.itzmrazotyalin.androwalp.ui.navigation

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
import com.itzmrazotyalin.androwalp.ui.screens.library.LibraryScreen
import com.itzmrazotyalin.androwalp.ui.screens.picker.PickerScreen
import com.itzmrazotyalin.androwalp.ui.screens.settings.SettingsScreen

@Composable
fun AndroWalpApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = when (backStackEntry?.destination?.route) {
        AppDestination.IMPORT.route -> AppDestination.IMPORT
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
                    navController.navigateTopLevel(
                        if (destination == AppDestination.IMPORT) {
                            AppDestination.importRoute()
                        } else {
                            destination.route
                        },
                    )
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
                    onImportClick = {
                        navController.navigateTopLevel(AppDestination.importRoute())
                    },
                    onEditClick = { wallpaperId ->
                        navController.navigateTopLevel(AppDestination.importRoute(wallpaperId))
                    },
                )
            }
            composable(
                route = AppDestination.IMPORT.route,
                arguments = listOf(
                    navArgument(AppDestination.WALLPAPER_ID_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val wallpaperId = entry.arguments
                    ?.getString(AppDestination.WALLPAPER_ID_ARG)
                    .orEmpty()
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
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}