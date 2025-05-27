/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.theme

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import teksturepako.pakkuDesktop.app.ui.viewmodel.ProfileViewModel

@Composable
fun ApplicationScope.ThemedBox(
    modifier: Modifier = Modifier,
    content: @Composable (ApplicationScope) -> Unit
)
{
    val profileData by ProfileViewModel.profileData.collectAsState()

    val themeDefinition = if (profileData.intUiTheme.isDark())
    {
        JewelTheme.darkThemeDefinition()
    }
    else
    {
        JewelTheme.lightThemeDefinition()
    }

    val color = remember { Animatable(themeDefinition.globalColors.panelBackground) }

    LaunchedEffect(profileData.intUiTheme) {
        color.animateTo(themeDefinition.globalColors.panelBackground, animationSpec = tween(200))
    }

    Box(
        modifier.background(color.value),
    ) {
        content(this@ThemedBox)
    }
}
