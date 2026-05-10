/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.window

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import io.github.kdroidfilter.nucleus.window.jewel.JewelDecoratedWindow
import org.jetbrains.jewel.ui.component.painterResource
import teksturepako.pakkuDesktop.app.data.WindowData
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.appNameWithVersion
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver
import java.awt.Dimension
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import teksturepako.pakkuDesktop.app.ui.LocalPakkuApplicationScope

/**
 * Window state for placement persistence — provided by [mainWindowDriver] for descendants (e.g. windowDiskDriver).
 */
val LocalWindowState = compositionLocalOf<WindowState> {
    error("LocalWindowState not provided — mainWindowDriver must wrap the tree")
}

fun snapshotWindowData(windowState: WindowState) = WindowData(
    placement = windowState.placement,
    x = windowState.position.x.value.takeUnless { it.isNaN() },
    y = windowState.position.y.value.takeUnless { it.isNaN() },
    width = windowState.size.width.value,
    height = windowState.size.height.value,
)

/**
 * Outermost UI driver: decorated window, title from [AppModel], [LocalPakkuApplicationScope] and [LocalWindowState].
 */
fun mainWindowDriver(
    applicationScope: ApplicationScope,
    initialWindowData: WindowData,
    onCloseRequest: () -> Unit,
): Driver<AppModel, AppMsg> = { _, model, content ->
    val windowState = rememberWindowState(
        placement = initialWindowData.placement,
        isMinimized = false,
        position = if (initialWindowData.x != null && initialWindowData.y != null) {
            WindowPosition.Absolute(x = initialWindowData.x.dp, y = initialWindowData.y.dp)
        } else {
            WindowPosition(Alignment.Center)
        },
        width = initialWindowData.width.dp,
        height = initialWindowData.height.dp,
    )

    val title = model.profile.data.currentProfile?.name ?: appNameWithVersion(model.isProActivated)

    JewelDecoratedWindow(
        state = windowState,
        onCloseRequest = onCloseRequest,
        title = title,
        icon = painterResource("icons/pakku.svg"),
    ) {
        this.window.minimumSize = Dimension(600, 400)

        CompositionLocalProvider(
            LocalPakkuApplicationScope provides object : PakkuApplicationScope {
                override val applicationScope = applicationScope
                override val decoratedWindowScope = this@JewelDecoratedWindow
            },
            LocalWindowState provides windowState,
        ) {
            content()
        }
    }
}
