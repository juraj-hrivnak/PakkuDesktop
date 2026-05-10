/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.appName
import teksturepako.pakkuDesktop.app.ui.component.button.ThemeButton
import teksturepako.pakkuDesktop.app.ui.application.titlebar.AlignedTitleBarContent
import teksturepako.pakkuDesktop.app.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.app.ui.component.FadeIn
import teksturepako.pakkuDesktop.app.ui.component.HoverablePanel
import teksturepako.pakkuDesktop.app.ui.component.button.SettingsButton
import teksturepako.pakkuDesktop.app.ui.component.dropdown.WelcomeViewDropdown
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.driver.LocalPickDirectory
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.WelcomeModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pro.ui.component.Pro

@Composable
fun PakkuApplicationScope.WelcomeView(
    publish: (WelcomeMsg) -> Unit,
    model: WelcomeModel,
    appModel: AppModel,
    appPublish: (AppMsg) -> Unit,
) {
    val profileData = model.profileData
    val titleBarHeight = 40.dp
    val pickDirectory = LocalPickDirectory.current
    val displayName = appName(appModel.isProActivated)

    MainTitleBar(
        Modifier.height(titleBarHeight),
        themeTrailingActions = {
            ThemeButton(appPublish, appModel.profile.data.intUiTheme)
        },
    ) {
        AlignedTitleBarContent(alignment = Alignment.Start) {
            Text("Welcome to $displayName!")
            WelcomeViewDropdown(
                profileData = profileData,
                onOpenDirectory = { pickDirectory() },
                onNewModpack = { publish(WelcomeMsg.ShowNewModpack) },
                onRecentProfile = { path -> publish(WelcomeMsg.DirectoryPicked(path)) },
            )
        }
        AlignedTitleBarContent(alignment = Alignment.End) {
            SettingsButton(onClick = { publish(WelcomeMsg.ShowSettings) })
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .subtractTopHeight(titleBarHeight)
    ) {
        // Welcome Header
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45F)
                .padding(PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientHeader("Welcome to $displayName!")
        }

        // Modpacks Box
        Box(
            Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            FadeIn {
                ContentBox(
                    Modifier.fillMaxSize(0.9F).padding(20.dp)
                ) {
                    val scrollState = rememberScrollState()

                    Column {
                        // Header and Open button
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Header("Modpacks", Modifier.padding(horizontal = 24.dp))

                            Row(
                                Modifier.padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            ) {
                                OutlinedButton(onClick = { publish(WelcomeMsg.ShowNewModpack) }) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            key = AllIconsKeys.General.InlineAdd,
                                            contentDescription = "new modpack icon",
                                            tint = JewelTheme.contentColor,
                                            hints = arrayOf(),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("New Modpack...")
                                    }
                                }
                                OutlinedButton(onClick = { pickDirectory() }) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            key = PakkuDesktopIcons.open,
                                            contentDescription = "Open Icon",
                                            tint = JewelTheme.contentColor,
                                            hints = arrayOf(),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("Open...")
                                    }
                                }
                                Pro(appModel) {
                                    OutlinedButton(onClick = { }) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                key = AllIconsKeys.General.Vcs,
                                                contentDescription = "Clone Repository Icon",
                                                tint = JewelTheme.contentColor,
                                                hints = arrayOf(),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text("Clone Repository...")
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(
                            Modifier.padding(vertical = 16.dp)
                                .background(JewelTheme.globalColors.borders.disabled)
                                .height(1.dp).fillMaxWidth()
                        )

                        Box(Modifier.fillMaxSize()) {
                            FlowRow(
                                Modifier.fillMaxWidth().verticalScroll(scrollState).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                profileData.recentProfilesFiltered.forEach { profile ->
                                    HoverablePanel(
                                        onClick = { publish(WelcomeMsg.DirectoryPicked(profile.path)) }
                                    ) {
                                        FlowColumn(
                                            Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(profile.name, Modifier.padding(horizontal = 24.dp), fontSize = 18.sp)
                                            Text(profile.path, Modifier.padding(horizontal = 24.dp), fontSize = 16.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }

                            VerticalScrollbar(
                                scrollState, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}
