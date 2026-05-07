/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import teksturepako.pakkuDesktop.app.ui.application.theme.PakkuDarkColorPalette
import teksturepako.pakkuDesktop.app.ui.application.theme.PakkuDarkGlobalColors
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

// ---------------------------------------------------------------------------
// themeDriver — wraps content with the correct IntUiTheme
// ---------------------------------------------------------------------------

val themeDriver: Driver<AppModel, AppMsg> = { _, model, content ->
    val textStyle = JewelTheme.createDefaultTextStyle(fontFamily = FontFamily.Default)

    val themeDefinition = if (model.profile.data.intUiTheme.isDark()) {
        JewelTheme.darkThemeDefinition(
            colors = PakkuDarkGlobalColors,
            palette = PakkuDarkColorPalette,
            defaultTextStyle = textStyle
        )
    } else {
        JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle)
    }

    IntUiTheme(themeDefinition, ComponentStyling.default(), swingCompatMode = false) {
        content()
    }
}

