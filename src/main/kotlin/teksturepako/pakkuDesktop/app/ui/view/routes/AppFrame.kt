/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.appName
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TITLE_BAR_BASE_HEIGHT_DP
import teksturepako.pakkuDesktop.app.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.app.ui.component.button.SettingsButton
import teksturepako.pakkuDesktop.app.ui.component.button.ThemeButton
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import teksturepako.pakkuDesktop.elm.animatedRoute

/**
 * Stable window chrome (Pakku icon, theme, window buttons) stays mounted.
 * Only the title-bar slots and the page body fade when [AppModel.screen] changes.
 */
@Composable
fun PakkuApplicationScope.AppFrame(
    model: AppModel,
    publish: (AppMsg) -> Unit,
    activation: @Composable () -> Unit,
) {
    val routeKey = when (model.screen) {
        AppScreen.Modpack -> "modpack:${model.profile.data.currentProfile?.path}"
        AppScreen.Welcome -> "welcome"
        AppScreen.Activation -> "activation"
    }
    val titleBarHeight = TITLE_BAR_BASE_HEIGHT_DP.dp

    Box(Modifier.fillMaxSize()) {
        animatedRoute(routeKey) {
            when (model.screen) {
                AppScreen.Welcome -> WelcomeView(
                    publish = { publish(AppMsg.Welcome(it)) },
                    model = model.welcome,
                    appModel = model,
                    appPublish = publish,
                )
                AppScreen.Modpack -> ModpackView(
                    publish = { publish(AppMsg.Modpack(it)) },
                    model = model.modpack,
                    appModel = model,
                )
                AppScreen.Activation -> activation()
            }
        }

        MainTitleBar(
            Modifier.height(titleBarHeight),
            themeTrailingActions = {
                ThemeButton(publish, model.profile.data.intUiTheme)
            },
            startContent = {
                animatedRoute(routeKey, Modifier) {
                    when (model.screen) {
                        AppScreen.Welcome -> WelcomeTitleBarStart(
                            publish = { publish(AppMsg.Welcome(it)) },
                            model = model.welcome,
                            displayName = appName(model.isProActivated),
                        )
                        AppScreen.Modpack -> ModpackTitleBarStart(
                            publish = { publish(AppMsg.Modpack(it)) },
                            model = model.modpack,
                        )
                        AppScreen.Activation -> Text("Welcome to Pakku Pro")
                    }
                }
            },
            endContent = {
                // Same control on Welcome and Modpack; only its presence changes.
                val endKey = if (model.screen == AppScreen.Activation) "none" else "settings"
                animatedRoute(endKey, Modifier) {
                    if (model.screen != AppScreen.Activation) {
                        SettingsButton(
                            onClick = {
                                when (model.screen) {
                                    AppScreen.Welcome -> publish(AppMsg.Welcome(WelcomeMsg.ShowSettings))
                                    AppScreen.Modpack -> publish(AppMsg.Modpack(ModpackMsg.ShowSettings))
                                    AppScreen.Activation -> Unit
                                }
                            },
                        )
                    }
                }
            },
        )
    }
}
