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
import teksturepako.pakkuDesktop.elm.animatedColor

/**
 * Animated background Box that reacts to the current theme.
 * Reads from JewelTheme (provided by themedApplication via ProfileViewModel bridge),
 * which is the correct source at this level — LocalAppModel is not yet provided here.
 */
@Composable
fun ApplicationScope.ThemedBox(
    modifier: Modifier = Modifier,
    content: @Composable (ApplicationScope) -> Unit,
) {
    val background = animatedColor(JewelTheme.globalColors.panelBackground)

    Box(modifier.background(background)) {
        content(this@ThemedBox)
    }
}
