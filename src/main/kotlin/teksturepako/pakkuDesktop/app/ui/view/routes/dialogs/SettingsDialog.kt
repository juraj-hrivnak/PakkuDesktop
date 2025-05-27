/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogWindow

@Composable
fun SettingsDialog(navController: NavHostController)
{
    PkUiDialogWindow(
        visible = navController.currentDestination?.route?.contains("settings") == true,
        onDismiss = { navController.popBackStack() },
        title = "Settings"
    ) {

    }
}
