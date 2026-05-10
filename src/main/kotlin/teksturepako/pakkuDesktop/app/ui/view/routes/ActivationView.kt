/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.LocalAppModel
import teksturepako.pakkuDesktop.app.ui.LocalAppPublish
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.app.ui.component.FadeIn
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight
import teksturepako.pakkuDesktop.pro.ui.component.license.LicenseKeyField

@Composable
fun PakkuApplicationScope.ActivationView()
{
    val titleBarHeight = 40.dp
    val appModel = LocalAppModel.current
    val appPublish = LocalAppPublish.current

    MainTitleBar(Modifier.height(titleBarHeight)) {
        FadeIn {
            Text("Welcome to Pakku Pro")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .subtractTopHeight(titleBarHeight)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5F)
                .padding(teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
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
            verticalAlignment = Alignment.Top
        ) {
            LicenseKeyField(
                isProActivated = appModel.isProActivated,
                licenseKeyError = appModel.licenseKeyError,
                onSubmitLicenseKey = { key -> appPublish(AppMsg.LicenseKeySubmit(key)) },
            )
        }
    }
}