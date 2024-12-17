package pakkupro.view.modpack.tabs

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.component.styling.TabStyle
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.painter.hints.Selected
import org.jetbrains.jewel.ui.painter.hints.Stateful
import org.jetbrains.jewel.ui.painter.rememberResourcePainterProvider
import org.jetbrains.jewel.ui.theme.defaultTabStyle
import org.jetbrains.jewel.ui.theme.editorTabStyle
import org.jetbrains.jewel.ui.util.thenIf
import teksturepako.pakkupro.ui.application.theme.IntUiThemes
import pakkupro.viewmodel.Icons
import pakkupro.viewmodel.MainViewModel
import pakkupro.viewmodel.PakkuDesktopIcons
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Preview
fun tabThree(coroutineScope: CoroutineScope)
{
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Theme:")

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeSelectionChip(IntUiThemes.Dark, "Dark", PakkuDesktopIcons.darkTheme)

            ThemeSelectionChip(IntUiThemes.Light, "Light", PakkuDesktopIcons.lightTheme)

            ThemeSelectionChip(IntUiThemes.System, "System", PakkuDesktopIcons.systemTheme)
        }
    }

    SegmentedControls()

    Tabs()

}

@Composable
fun ThemeSelectionChip(theme: IntUiThemes, name: String, iconKey: IconKey) {
    RadioButtonChip(
        selected = MainViewModel.theme == theme,
        onClick = { MainViewModel.theme = theme },
        enabled = true,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(iconKey, name, hint = Selected(MainViewModel.theme == theme))
            Text(name)
        }
    }
}

@Composable
fun SegmentedControls() {
    var selectedButtonIndex by remember { mutableStateOf(0) }
    val buttonIds = listOf(0, 1, 2, 3)
    val buttons =
        remember(selectedButtonIndex) {
            buttonIds.map { index ->
                SegmentedControlButtonData(
                    selected = index == selectedButtonIndex,
                    content = { _ -> Text("Button ${index + 1}") },
                    onSelect = { selectedButtonIndex = index },
                )
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SegmentedControl(buttons = buttons, enabled = true)

        SegmentedControl(buttons = buttons, enabled = false)
    }
}


@Composable
fun Tabs() {
    Column {
        Text("Default tabs", Modifier.fillMaxWidth())
        DefaultTabShowcase()

        Spacer(Modifier.height(16.dp))
        Text("Editor tabs", Modifier.fillMaxWidth())
        EditorTabShowcase()
    }
}

@Composable
private fun DefaultTabShowcase() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var tabIds by remember { mutableStateOf((1..12).toList()) }
    val maxId = remember(tabIds) { tabIds.maxOrNull() ?: 0 }

    val tabs =
        remember(tabIds, selectedTabIndex) {
            tabIds.mapIndexed { index, id ->
                TabData.Default(
                    selected = index == selectedTabIndex,
                    content = { tabState ->
                        val iconProvider = rememberResourcePainterProvider(AllIconsKeys.Actions.Find)
                        val icon by iconProvider.getPainter(Stateful(tabState))
                        SimpleTabContent(label = "Default Tab $id", state = tabState, icon = icon)
                    },
                    onClose = {
                        tabIds = tabIds.toMutableList().apply { removeAt(index) }
                        if (selectedTabIndex >= index) {
                            val maxPossibleIndex = max(0, tabIds.lastIndex)
                            selectedTabIndex = (selectedTabIndex - 1).coerceIn(0..maxPossibleIndex)
                        }
                    },
                    onClick = { selectedTabIndex = index },
                )
            }
        }

    TabStripWithAddButton(tabs, JewelTheme.defaultTabStyle) {
        val insertionIndex = (selectedTabIndex + 1).coerceIn(0..tabIds.size)
        val nextTabId = maxId + 1

        tabIds = tabIds.toMutableList().apply { add(insertionIndex, nextTabId) }
        selectedTabIndex = insertionIndex
    }
}

@Composable
private fun EditorTabShowcase() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var tabIds by remember { mutableStateOf((1..12).toList()) }
    val maxId = remember(tabIds) { tabIds.maxOrNull() ?: 0 }

    val tabs =
        remember(tabIds, selectedTabIndex) {
            tabIds.mapIndexed { index, id ->
                TabData.Editor(
                    selected = index == selectedTabIndex,
                    content = { tabState ->
                        SimpleTabContent(
                            state = tabState,
                            modifier = Modifier,
                            icon = {
                                Icon(
                                    key = AllIconsKeys.Actions.Find,
                                    contentDescription = null,
                                    iconClass = Icons::class.java,
                                    modifier = Modifier.size(16.dp).tabContentAlpha(state = tabState),
                                    tint = Color.Magenta,
                                )
                            },
                            label = { Text("Editor tab $id") },
                        )
                        Box(
                            modifier =
                            Modifier.size(12.dp).thenIf(tabState.isHovered) {
                                drawWithCache {
                                    onDrawBehind {
                                        drawCircle(color = Color.Magenta.copy(alpha = .4f), radius = 6.dp.toPx())
                                    }
                                }
                            }
                        )
                    },
                    onClose = {
                        tabIds = tabIds.toMutableList().apply { removeAt(index) }
                        if (selectedTabIndex >= index) {
                            val maxPossibleIndex = max(0, tabIds.lastIndex)
                            selectedTabIndex = (selectedTabIndex - 1).coerceIn(0..maxPossibleIndex)
                        }
                    },
                    onClick = { selectedTabIndex = index },
                )
            }
        }

    TabStripWithAddButton(tabs, JewelTheme.editorTabStyle) {
        val insertionIndex = (selectedTabIndex + 1).coerceIn(0..tabIds.size)
        val nextTabId = maxId + 1

        tabIds = tabIds.toMutableList().apply { add(insertionIndex, nextTabId) }
        selectedTabIndex = insertionIndex
    }
}

@Composable
private fun TabStripWithAddButton(tabs: List<TabData>, style: TabStyle, onAddClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TabStrip(tabs, style, modifier = Modifier.weight(1f))

        IconButton(onClick = onAddClick, modifier = Modifier.size(JewelTheme.defaultTabStyle.metrics.tabHeight)) {
            Icon(key = AllIconsKeys.General.Add, contentDescription = "Add a tab")
        }
    }
}
