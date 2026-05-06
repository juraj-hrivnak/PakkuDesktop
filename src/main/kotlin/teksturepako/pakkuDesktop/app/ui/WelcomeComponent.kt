/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.app.ui.model.WelcomeModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import teksturepako.pakkuDesktop.app.ui.view.routes.WelcomeView
import teksturepako.pakkuDesktop.elm.component

// ---------------------------------------------------------------------------
// welcomeUpdate — WelcomeModel has no child state to change internally;
// all side effects (showSettings, pendingPath, etc.) are handled by appUpdate.
// ---------------------------------------------------------------------------

fun welcomeUpdate(msg: WelcomeMsg, model: WelcomeModel): WelcomeModel = model

// ---------------------------------------------------------------------------
// welcomeComponent
// ---------------------------------------------------------------------------

val welcomeComponent = component(
    init = WelcomeModel(),
    update = ::welcomeUpdate,
    view = { publish, model ->
        val scope = LocalPakkuApplicationScope.current
        with(scope) { WelcomeView(publish, model) }
    }
)

