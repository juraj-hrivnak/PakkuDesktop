/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.styling.dark
import org.jetbrains.jewel.intui.standalone.styling.light
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.ui.component.styling.LocalMenuStyle
import org.jetbrains.jewel.ui.component.styling.MenuStyle
import teksturepako.pakkuDesktop.app.ui.application.theme.PakkuDarkColorPalette
import teksturepako.pakkuDesktop.app.ui.application.theme.PakkuDarkGlobalColors
import teksturepako.pakkuDesktop.app.ui.application.theme.pakkuPopupMenuStyle
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

// -- themeDriver --

val themeDriver: Driver<AppModel, AppMsg> = { _, model, content ->
    val isDark = model.profile.data.intUiTheme.isDark()
    val textStyle = remember { JewelTheme.createDefaultTextStyle(fontFamily = FontFamily.Default) }
    val themeDefinition = remember(isDark) {
        if (isDark) {
            JewelTheme.darkThemeDefinition(
                colors = PakkuDarkGlobalColors,
                palette = PakkuDarkColorPalette,
                defaultTextStyle = textStyle,
            )
        } else {
            JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle)
        }
    }

    // Use the empty ComponentStyling companion so IntUiTheme applies only ComponentStyling.default()
    // (see Jewel BaseJewelTheme). Do not merge a second LocalMenuStyle via that bulk provider: duplicate
    // keys in CompositionLocalProvider(values = …) are unreliable; shadow menu style with an inner provider.
    IntUiTheme(
        themeDefinition,
        ComponentStyling,
        swingCompatMode = false,
    ) {
        val menuStyle = remember(themeDefinition.isDark) {
            val reference = if (themeDefinition.isDark) MenuStyle.dark() else MenuStyle.light()
            pakkuPopupMenuStyle(reference.metrics, reference.icons, themeDefinition.isDark)
        }
        CompositionLocalProvider(LocalMenuStyle provides menuStyle) {
            content()
        }
    }
}

