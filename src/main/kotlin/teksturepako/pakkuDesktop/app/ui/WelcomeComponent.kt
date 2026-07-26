/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import teksturepako.pakkuDesktop.app.ui.model.WelcomeModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import teksturepako.pakkuDesktop.elm.component

// ---------------------------------------------------------------------------
// welcomeUpdate — delegates dropdown state to welcomeDropdownComponent;
// all cross-cutting effects (showSettings, pendingPath, etc.) handled by appUpdate.
// ---------------------------------------------------------------------------

fun welcomeUpdate(msg: WelcomeMsg, model: WelcomeModel): WelcomeModel = when (msg) {
    // Cross-cutting — appUpdate handles; child is inert
    WelcomeMsg.ShowSettings,
    WelcomeMsg.ShowNewModpack,
    is WelcomeMsg.DirectoryPicked,
    is WelcomeMsg.WelcomeDropdown -> model
}

// ---------------------------------------------------------------------------
// welcomeComponent
// ---------------------------------------------------------------------------

val welcomeComponent = component(
    init = WelcomeModel(),
    update = ::welcomeUpdate,
    // Real UI is composed from AppComponent (needs app-level AppMsg publish + PakkuApplicationScope).
    view = { _, _ -> Spacer(Modifier) },
)
