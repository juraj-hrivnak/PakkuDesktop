package teksturepako.pakkupro.ui.view.children

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakkupro.data.Polar
import teksturepako.pakkupro.ui.PakkuDesktopConstants
import teksturepako.pakkupro.ui.application.PakkuApplicationScope
import teksturepako.pakkupro.ui.application.titlebar.MainTitleBar
import teksturepako.pakkupro.ui.component.FadeIn
import teksturepako.pakkupro.ui.component.text.GradientHeader
import teksturepako.pakkupro.ui.component.text.Header
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PakkuApplicationScope.WelcomeView()
{
    val profileData by ProfileViewModel.profileData.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val titleBarHeight = 40.dp

    val pickerLauncher: PickerResultLauncher = rememberDirectoryPickerLauncher(
        title = "Open modpack directory",
        platformSettings = FileKitPlatformSettings(parentWindow = this.decoratedWindowScope.window)
    ) { directory ->
        if (directory?.path == null) return@rememberDirectoryPickerLauncher

        ProfileViewModel.updateCurrentProfile(Path(directory.path!!))
    }

    MainTitleBar(Modifier.height(titleBarHeight)) {
        FadeIn {
            Text("Welcome to Pakku Pro")
        }
    }

//    Box(
//        Modifier
//            .fillMaxSize()
//            .offset(y = titleBarHeight)
//    ) {
//        Row {
//            Column(
//                Modifier
//                    .fillMaxHeight()
//                    .width(200.dp),
//            ) {
//                Icon(PakkuDesktopIcons.pakku, null)
//            }
//
//            Spacer(Modifier.background(JewelTheme.globalColors.borders.normal).width(1.dp).fillMaxHeight())
//
//            val scrollState = rememberScrollState()
//
//            Row(
//                Modifier
//                    .fillMaxWidth()
//                    .fillMaxHeight()
//                    .padding(PakkuDesktopConstants.commonPaddingSize)
//                    .verticalScroll(scrollState),
//                horizontalArrangement = Arrangement.Start,
//                verticalAlignment = Alignment.Top
//            ) {
//                FlowColumn(
//                    verticalArrangement = Arrangement.spacedBy(16.dp),
//                ) {
//                    Header("Recent modpacks")
//
//                    profileData.recentProfilesFiltered.forEach { (modpack, path) ->
//                        FadeIn {
//                            DefaultButton(
//                                onClick = {
//                                    ProfileViewModel.updateCurrentProfile(Path(path))
//                                },
//                            ) {
//                                Text(
//                                    modpack,
//                                    Modifier
//                                        .padding(horizontal = 16.dp, vertical = 4.dp),
//                                    fontSize = 16.sp
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//
//            VerticalScrollbar(scrollState = scrollState)
//        }
//    }


    Column(
        Modifier
            .fillMaxSize()
            .offset(y = titleBarHeight),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5F)
                .padding(PakkuDesktopConstants.commonPaddingSize),
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
                .padding(PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            val delay = 1.seconds

            var activated by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit)
            {
                activated = Polar.isActivated()
            }

            FadeIn(delay = delay) {
                FlowColumn(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    when (activated)
                    {
                        true  ->
                        {
                            GradientHeader("Pakku Pro is activated!")
                        }
                        false ->
                        {
                            Header("Please enter your license key.")

                            val licenseKeyText = rememberTextFieldState()
                            val success = mutableStateOf(false)
                            var error by remember { mutableStateOf<ActionError?>(null) }

                            TextField(
                                licenseKeyText,
                                Modifier
                                    .size(width = 445.dp, height = 62.dp)
                                    .padding(vertical = 16.dp),
                                textStyle = JewelTheme.editorTextStyle,
                                placeholder = { Text("PAKKU-PRO-00000000-0000-0000-0000-000000000000") }
                            )

                            ActionButton(onClick = {
                                coroutineScope.launch {
                                    Polar.process(licenseKeyText.text.toString()).onSuccess {
                                        success.value = true
                                    }.onFailure {
                                        error = it
                                    }
                                }
                            }) {
                                Text("Submit")
                            }

                            if (success.value)
                            {
                                Text("Success")
                            }

                            if (error != null)
                            {
                                Text(error!!.rawMessage)
                            }
                        }

                        null  ->
                        {

                        }
                    }
                }
            }
        }
    }
}

