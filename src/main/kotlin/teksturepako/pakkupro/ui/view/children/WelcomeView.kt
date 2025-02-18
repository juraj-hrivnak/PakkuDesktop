package teksturepako.pakkupro.ui.view.children

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakkupro.ui.PakkuDesktopConstants
import teksturepako.pakkupro.ui.application.PakkuApplicationScope
import teksturepako.pakkupro.ui.application.titlebar.MainTitleBar
import teksturepako.pakkupro.ui.component.ContentBox
import teksturepako.pakkupro.ui.component.FadeIn
import teksturepako.pakkupro.ui.component.HoverablePanel
import teksturepako.pakkupro.ui.component.dropdown.WelcomeViewDropdown
import teksturepako.pakkupro.ui.component.text.GradientHeader
import teksturepako.pakkupro.ui.component.text.Header
import teksturepako.pakkupro.ui.modifier.subtractTopHeight
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel
import kotlin.io.path.Path

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PakkuApplicationScope.WelcomeView() {
    val profileData by ProfileViewModel.profileData.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val titleBarHeight = 40.dp

    val pickerLauncher: PickerResultLauncher = rememberDirectoryPickerLauncher(
        title = "Open modpack directory",
        platformSettings = FileKitPlatformSettings(parentWindow = this.decoratedWindowScope.window)
    ) { directory ->
        if (directory?.path == null) return@rememberDirectoryPickerLauncher
        coroutineScope.launch {
            ProfileViewModel.updateCurrentProfile(Path(directory.path!!))
        }
    }

    MainTitleBar(Modifier.height(titleBarHeight)) {
        Text("Welcome to Pakku Pro")
        WelcomeViewDropdown(pickerLauncher)
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
            GradientHeader("Welcome to Pakku Pro!")
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
                            OutlinedButton(onClick = { pickerLauncher.launch() }) {
                                Text("Open")
                            }
                        }

                        Spacer(
                            Modifier.padding(vertical = 16.dp).background(JewelTheme.globalColors.borders.disabled)
                                .height(1.dp).fillMaxWidth()
                        )

                        Box(
                            Modifier.fillMaxSize()
                        ) {
                            FlowRow(
                                Modifier.fillMaxWidth().verticalScroll(scrollState)
                                    .padding(end = 12.dp), // Space for scrollbar
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                profileData.recentProfilesFiltered.mapIndexed { i, profile ->
                                    HoverablePanel(
                                        onClick = {
                                            coroutineScope.launch {
                                                ProfileViewModel.updateCurrentProfile(Path(profile.path))
                                            }
                                        }
                                    ) {
                                        FlowColumn(
                                            Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                profile.name,
                                                Modifier.padding(horizontal = 24.dp),
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                profile.path,
                                                Modifier.padding(horizontal = 24.dp),
                                                fontSize = 16.sp,
                                                color = Color.Gray
                                            )
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
