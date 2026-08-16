/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import teksturepako.pakkuDesktop.app.ui.model.WelcomeModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import teksturepako.pakkuDesktop.elm.component

// -- welcomeUpdate --

fun welcomeUpdate(msg: WelcomeMsg, model: WelcomeModel): WelcomeModel = when (msg) {
    // parent
    WelcomeMsg.ShowSettings,
    WelcomeMsg.ShowNewModpack,
    is WelcomeMsg.DirectoryPicked,
    is WelcomeMsg.WelcomeDropdown -> model
}

// -- welcomeComponent --

val welcomeComponent = component(
    init = WelcomeModel(),
    update = ::welcomeUpdate,
    // view in AppComponent
    view = { _, _ -> Spacer(Modifier) },
)
