/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.button

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import teksturepako.pakkuDesktop.app.ui.model.AppMsg

@Composable
fun ThemeButton(
    appPublish: (AppMsg) -> Unit,
    intUiTheme: IntUiThemes,
) {
    if (intUiTheme.isDark()) {
        ToolbarActionButton(
            key = PakkuDesktopIcons.darkTheme,
            label = "Switch to light theme",
            onClick = { appPublish(AppMsg.ThemeChangeRequested(IntUiThemes.Light)) },
        )
    } else {
        ToolbarActionButton(
            key = PakkuDesktopIcons.lightTheme,
            label = "Switch to dark theme",
            onClick = { appPublish(AppMsg.ThemeChangeRequested(IntUiThemes.Dark)) },
        )
    }
}
