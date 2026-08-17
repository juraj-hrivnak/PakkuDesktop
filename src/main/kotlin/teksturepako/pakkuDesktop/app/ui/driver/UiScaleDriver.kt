/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

/**
 * User UI zoom on top of OS DPI. Must sit inside [teksturepako.pakkuDesktop.app.ui.application.window.mainWindowDriver]
 * so window placement (saved Dp sizes) stays at system density.
 */
val uiScaleDriver: Driver<AppModel, AppMsg> = { _, model, content ->
    val uiScale = ProfileData.coerceUiScale(model.profile.data.uiScale)
    val system = LocalDensity.current
    // Always wrap — skipping at 100% remounts the tree and glitches dialogs/combos.
    val scaled = remember(system.density, system.fontScale, uiScale) {
        Density(
            density = system.density * uiScale,
            fontScale = system.fontScale,
        )
    }
    CompositionLocalProvider(LocalDensity provides scaled) {
        content()
    }
}
