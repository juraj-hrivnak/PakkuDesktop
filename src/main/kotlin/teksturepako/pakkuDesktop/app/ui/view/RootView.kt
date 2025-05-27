/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.view.routes.ModpackView
import teksturepako.pakkuDesktop.app.ui.view.routes.WelcomeView
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.SettingsDialog
import teksturepako.pakkuDesktop.app.ui.viewmodel.ProfileViewModel

@Serializable
sealed class Navigation(val route: String) {
    data object Home : Navigation("home")
    data object Modpack : Navigation("modpack")
    data class Settings(val parent: Navigation) : Navigation("$parent/settings")
}

@Composable
fun PakkuApplicationScope.RootView() {
    val profileData by ProfileViewModel.profileData.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        if (profileData.currentProfile != null) {
            navController.navigate(Navigation.Modpack.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Navigation.Home.route,
    ) {
        composable(Navigation.Home.route) {
            WelcomeView(navController)
        }
        composable(Navigation.Modpack.route) {
            ModpackView(navController)
        }

        dialog(Navigation.Settings(Navigation.Home).route) {
            SettingsDialog(navController)
        }
        dialog(Navigation.Settings(Navigation.Modpack).route) {
            SettingsDialog(navController)
        }
    }
}