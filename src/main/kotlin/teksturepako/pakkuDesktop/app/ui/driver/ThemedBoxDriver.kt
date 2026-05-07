/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.foundation.theme.JewelTheme
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver
import teksturepako.pakkuDesktop.elm.animatedColor

// ---------------------------------------------------------------------------
// themedBoxDriver — animated full-window background, driven by the ELM model.
// Must be placed AFTER themeDriver in the driver list so it reads JewelTheme
// from inside themeDriver's IntUiTheme — synchronized with all other components.
// ---------------------------------------------------------------------------

val themedBoxDriver: Driver<AppModel, AppMsg> = { _, _, content ->
    val background = animatedColor(JewelTheme.globalColors.panelBackground)
    Box(Modifier.fillMaxSize().background(background)) {
        content()
    }
}

