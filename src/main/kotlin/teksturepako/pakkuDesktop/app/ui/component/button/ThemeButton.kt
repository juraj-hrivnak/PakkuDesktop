/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.button

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import teksturepako.pakkuDesktop.app.ui.model.AppMsg

@Composable
fun ThemeButton(
    appPublish: (AppMsg) -> Unit,
    intUiTheme: IntUiThemes,
) {
    if (intUiTheme.isDark()) {
        IconButton(
            onClick = { appPublish(AppMsg.ThemeChangeRequested(IntUiThemes.Light)) },
            Modifier.size(30.dp),
        ) {
            Icon(PakkuDesktopIcons.darkTheme, "dark_theme")
        }
    } else {
        IconButton(
            onClick = { appPublish(AppMsg.ThemeChangeRequested(IntUiThemes.Dark)) },
            Modifier.size(30.dp),
        ) {
            Icon(PakkuDesktopIcons.lightTheme, "light_theme")
        }
    }
}
