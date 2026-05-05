/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.component.dialog.CloseDialog
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.view.routes.ModpackView
import teksturepako.pakkuDesktop.app.ui.view.routes.WelcomeView
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.NewModpackDialog
import teksturepako.pakkuDesktop.app.ui.view.routes.dialogs.SettingsDialog

/**
 * Top-level view that switches between screens based on model.screen.
 * Navigation is purely declarative — dialogs are shown conditionally.
 *
 * Called from [PakkuApplicationScope] (inside MainWindow).
 */
@Composable
fun PakkuApplicationScope.AppView(
    publish: (AppMsg) -> Unit,
    model: AppModel,
) {
    // Conditional dialogs — pure composables, no driver needed
    CloseDialog(publish, model)

    if (model.showSettings) {
        SettingsDialog(onDismiss = { publish(AppMsg.HideSettings) })
    }

    if (model.showNewModpack) {
        NewModpackDialog(
            profileData = model.profile.data,
            onDismiss = { publish(AppMsg.HideNewModpack) }
        )
    }

    // Main screen
    when (model.screen) {
        AppScreen.Welcome -> WelcomeView(publish, model)
        AppScreen.Modpack -> ModpackView(publish, model)
    }
}

