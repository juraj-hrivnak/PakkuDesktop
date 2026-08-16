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
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import teksturepako.pakkuDesktop.app.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.app.ui.component.FadeIn
import teksturepako.pakkuDesktop.app.ui.component.button.ThemeButton
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight
import teksturepako.pakkuDesktop.pro.ui.component.license.LicenseKeyField

/** Pakku Pro license activation screen. */
@Composable
fun PakkuApplicationScope.ActivationView(
    appPublish: (AppMsg) -> Unit,
    isProActivated: Boolean?,
    licenseKeyError: ActionError?,
    intUiTheme: IntUiThemes,
) {
    val titleBarHeight = 40.dp

    MainTitleBar(
        Modifier.height(titleBarHeight),
        themeTrailingActions = {
            ThemeButton(appPublish, intUiTheme)
        },
    ) {
        FadeIn {
            Text("Welcome to Pakku Pro")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .subtractTopHeight(titleBarHeight),
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
                LicenseKeyField(
                    isProActivated = isProActivated,
                    licenseKeyError = licenseKeyError,
                    onSubmitLicenseKey = { key -> appPublish(AppMsg.LicenseKeySubmit(key)) },
                )
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
