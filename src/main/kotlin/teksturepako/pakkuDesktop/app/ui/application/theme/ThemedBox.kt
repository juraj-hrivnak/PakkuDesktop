/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import teksturepako.pakkuDesktop.app.ui.LocalAppModel
import teksturepako.pakkuDesktop.elm.animatedColor

/**
 * Animated background Box that reacts to the current theme in the model.
 * Uses [animatedColor] — purely local visual animation, no driver needed.
 */
@Composable
fun ApplicationScope.ThemedBox(
    modifier: Modifier = Modifier,
    content: @Composable (ApplicationScope) -> Unit,
) {
    val model = LocalAppModel.current

    val themeDefinition = if (model.profile.data.intUiTheme.isDark()) {
        JewelTheme.darkThemeDefinition()
    } else {
        JewelTheme.lightThemeDefinition()
    }

    val background = animatedColor(themeDefinition.globalColors.panelBackground)

    Box(modifier.background(background)) {
        content(this@ThemedBox)
    }
}
