/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.application
import org.jetbrains.jewel.foundation.DisabledAppearanceValues
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.*
import org.jetbrains.jewel.ui.ComponentStyling
import teksturepako.pakkuDesktop.app.ui.viewmodel.ProfileViewModel

fun themedApplication(
    content: @Composable ApplicationScope.() -> Unit
) = application {
    val profileData by ProfileViewModel.profileData.collectAsState()

    val textStyle = JewelTheme.createDefaultTextStyle(fontFamily = FontFamily.Default)

    val themeDefinition = if (profileData.intUiTheme.isDark())
    {
        JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle, disabledAppearanceValues = DisabledAppearanceValues.dark())
    }
    else
    {
        JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle, disabledAppearanceValues = DisabledAppearanceValues.light())
    }

    IntUiTheme(
        themeDefinition, ComponentStyling.default(),
        swingCompatMode = false
    ) {
        content(this)
    }
}