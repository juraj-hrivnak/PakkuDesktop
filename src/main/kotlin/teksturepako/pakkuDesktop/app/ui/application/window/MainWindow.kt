/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.window

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import io.github.kdroidfilter.nucleus.window.jewel.JewelDecoratedWindow
import org.jetbrains.jewel.ui.component.painterResource
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.LocalAppModel
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.appNameWithVersion
import teksturepako.pakkuDesktop.app.ui.application.theme.ThemedBox
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import java.awt.Dimension

/**
 * Main application window. Receives:
 * - [initialWindowData] loaded synchronously in main() before application {}
 * - [onCloseRequest] called by the window system — dispatches into the ELM loop
 * - [content] receives [PakkuApplicationScope] and a reference to the live [WindowState]
 *   (needed by the windowDiskDriver to snapshot position/size before quitting)
 */
@Composable
fun ApplicationScope.MainWindow(
    initialWindowData: WindowData,
    onCloseRequest: () -> Unit,
    content: @Composable PakkuApplicationScope.(windowState: WindowState) -> Unit,
) {
    val windowState = rememberWindowState(
        placement = initialWindowData.placement,
        isMinimized = false,
        position = if (initialWindowData.x != null && initialWindowData.y != null) {
            WindowPosition.Absolute(x = initialWindowData.x.dp, y = initialWindowData.y.dp)
        } else WindowPosition(Alignment.Center),
        width = initialWindowData.width.dp,
        height = initialWindowData.height.dp,
    )

    // Title derived from model via CompositionLocal (set by AppComponent view)
    val model = LocalAppModel.current
    val title = model.profile.data.currentProfile?.name ?: appNameWithVersion

    JewelDecoratedWindow(
        state = windowState,
        onCloseRequest = onCloseRequest,
        title = title,
        icon = painterResource("icons/pakku.svg"),
    ) {
        this.window.minimumSize = Dimension(600, 400)

        ThemedBox(Modifier.fillMaxSize()) {
            content(
                object : PakkuApplicationScope {
                    override val applicationScope = this@MainWindow
                    override val decoratedWindowScope = this@JewelDecoratedWindow
                },
                windowState,
            )
        }
    }
}
