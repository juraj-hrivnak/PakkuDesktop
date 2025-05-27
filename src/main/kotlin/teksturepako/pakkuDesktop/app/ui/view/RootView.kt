package teksturepako.pakkuDesktop.app.ui.view

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.view.children.ModpackView
import teksturepako.pakkuDesktop.app.ui.view.children.WelcomeView

@Serializable
data object Home

@Serializable
data object Modpack

@Composable
fun PakkuApplicationScope.RootView() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Home,
    ) {
        composable<Home> { WelcomeView(navController) }
        composable<Modpack> { ModpackView(navController) }
    }
}
