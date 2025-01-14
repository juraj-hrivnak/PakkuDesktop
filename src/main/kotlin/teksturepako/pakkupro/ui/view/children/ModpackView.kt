package teksturepako.pakkupro.ui.view.children

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.PickerResultLauncher
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import teksturepako.pakkupro.ui.application.PakkuApplicationScope
import teksturepako.pakkupro.ui.application.titlebar.MainTitleBar
import teksturepako.pakkupro.ui.component.dropdown.ModpackDropdown
import teksturepako.pakkupro.ui.component.text.Header
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel
import teksturepako.pakkupro.ui.viewmodel.state.SelectedTab
import kotlin.io.path.Path

@Composable
fun PakkuApplicationScope.ModpackView()
{
    val titleBarHeight = 40.dp

    val profileData by ProfileViewModel.profileData.collectAsState()
    val appUiState by ModpackViewModel.modpackUiState.collectAsState()

    val pickerLauncher: PickerResultLauncher = rememberDirectoryPickerLauncher(
        title = "Open modpack directory"
    ) { directory ->
        if (directory?.path == null) return@rememberDirectoryPickerLauncher

        ProfileViewModel.updateCurrentProfile(Path(directory.path!!))
    }

    MainTitleBar(Modifier.height(titleBarHeight)) {
        ModpackDropdown(pickerLauncher)
    }

    Column(
        Modifier
            .fillMaxSize()
            .offset(y = titleBarHeight),
    ) {
        profileData.currentProfile?.let { Header(it) }

        when (appUiState.selectedTab)
        {
            SelectedTab.PROJECTS -> ProjectsTab()
            SelectedTab.MODPACK  -> ModpackTab()
        }
    }
}

@Composable
fun ProjectsTab()
{
    Text("projects tab")

    val outerSplitState: SplitLayoutState = remember { SplitLayoutState(0.0F) }
    val verticalSplitState: SplitLayoutState = rememberSplitLayoutState()
    val innerSplitState: SplitLayoutState = rememberSplitLayoutState()
    val onResetState: () -> Unit = { }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reset split state:")
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onResetState) { Text("Reset") }
        }

        Spacer(Modifier.height(16.dp))

        HorizontalSplitLayout(
            state = outerSplitState,
            first = { FirstPane() },
            second = { SecondPane(innerSplitState = innerSplitState, verticalSplitState = verticalSplitState) },
            modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, color = JewelTheme.globalColors.borders.normal),
            firstPaneMinWidth = 200.dp,
            secondPaneMinWidth = 200.dp,
            draggableWidth = 16.dp
        )
    }
}

@Composable
fun ModpackTab()
{
    Text("modpack tab")
}

@Composable
private fun FirstPane() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        val state by remember { mutableStateOf(TextFieldState()) }
        TextField(state, placeholder = { Text("Placeholder") })
    }
}

@Composable
private fun SecondPane(innerSplitState: SplitLayoutState, verticalSplitState: SplitLayoutState) {
    VerticalSplitLayout(
        state = verticalSplitState,
        modifier = Modifier.fillMaxSize(),
        first = {
            val state by remember { mutableStateOf(TextFieldState()) }
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                TextField(state, placeholder = { Text("Right Panel Content") })
            }
        },
        second = {
            HorizontalSplitLayout(
                first = {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Second Pane left")
                    }
                },
                second = {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Second Pane right")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                state = innerSplitState,
                firstPaneMinWidth = 100.dp,
                secondPaneMinWidth = 100.dp,
            )
        },
        firstPaneMinWidth = 300.dp,
        secondPaneMinWidth = 100.dp,
    )
}