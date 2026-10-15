/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TITLE_BAR_BASE_HEIGHT_DP
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TITLE_BAR_BORDER_HEIGHT_DP
import teksturepako.pakkuDesktop.app.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.app.ui.component.FadeIn
import teksturepako.pakkuDesktop.app.ui.component.button.ThemeButton
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight

/** Pakku Pro license activation screen. [license] is the pakku-pro plugin slot. */
@Composable
fun PakkuApplicationScope.ActivationView(
    appPublish: (AppMsg) -> Unit,
    intUiTheme: IntUiThemes,
    license: @Composable () -> Unit,
) {
    val titleBarHeight = TITLE_BAR_BASE_HEIGHT_DP.dp
    val titleBarChromeHeight = titleBarHeight + TITLE_BAR_BORDER_HEIGHT_DP.dp

    MainTitleBar(
        Modifier.height(titleBarHeight),
        themeTrailingActions = {
            ThemeButton(appPublish, intUiTheme)
        },
        startContent = {
            FadeIn {
                Text("Welcome to Pakku Pro")
            }
        },
    )

    Column(
        Modifier
            .fillMaxSize()
            .subtractTopHeight(titleBarChromeHeight),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5F)
                .padding(teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowColumn(
                verticalArrangement = Arrangement.Center,
            ) {
                FadeIn {
                    GradientHeader("Welcome to Pakku Pro!")
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                license()
                OutlinedButton(
                    onClick = { appPublish(AppMsg.HideActivation) },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Back")
                }
            }
        }
    }
}
