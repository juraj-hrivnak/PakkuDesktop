/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Badge
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.integration.revealInFileManager
import teksturepako.pakkuDesktop.app.ui.LocalShiftKeyState
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectRowCompact
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeRow
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeFolderRow
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeItemRow
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeRowMinHeight
import teksturepako.pakkuDesktop.app.ui.component.tree.mergeExpandedFolders
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.modifier.allowDragAndDrop
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip
import java.awt.Cursor
import kotlin.io.path.Path

const val FilesDirectionToDiskDescription = "Change to disk"
const val FilesDirectionToPakkuDescription = "Change to Pakku"

private val FilesLinkGutterWidth = 28.dp
private val FilesSplitMinPaneWidth = 120.dp
private val FilesSplitHorizontalPadding = PakkuDesktopConstants.commonPaddingSize
private val FilesSplitHeaderHeight = 36.dp

@OptIn(FlowPreview::class)
@Composable
fun FilesLinkList(
    links: List<FilesLink>,
    listState: LazyListState,
    splitRatio: State<Float>,
    onSplitRatioChange: (Float) -> Unit,
    selectedProjectKeys: Set<String>,
    focusedProjectUiKey: String?,
    filesPreviewMode: FilesPreviewMode,
    configFile: teksturepako.pakku.api.data.ConfigFile?,
    expandedFolders: Set<String>,
    onToggleFolderExpanded: (String) -> Unit,
    linkIndexByPathKey: Map<String, Int>,
    lastClickedIndex: MutableState<Int?>,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showSelection = filesPreviewMode != FilesPreviewMode.None
    val tree = remember(links) { PathTreeSnapshot.of(links) { it.path } }
    val selectedPaths = remember(links, selectedProjectKeys) {
        links.filter { it.selectionKey in selectedProjectKeys }.map { it.path }.toSet()
    }
    val rows = remember(tree, expandedFolders, selectedPaths) {
        tree.flatRows(expandedFolders, selectedPaths)
    }
    val typeLabels = remember(configFile) { typeFolderLabels(configFile) }
    val shiftPressed = LocalShiftKeyState.current.pressed
    val updatedSplit = rememberUpdatedState(splitRatio)
    val onSplitDrag = remember(onSplitRatioChange, splitRatio) {
        { deltaPx: Float, splitAreaPx: Float ->
            val next = splitRatio.value + deltaPx / splitAreaPx
            onSplitRatioChange(next.coerceIn(0.05f, 0.95f))
        }
    }

    Column(modifier.fillMaxSize()) {
        FilesLinkListHeader(
            splitRatio = splitRatio,
            onSplitDrag = onSplitDrag,
        )
        Divider(Orientation.Horizontal)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .allowDragAndDrop { paths ->
                        publish(ModpackMsg.FilesDropped(paths.map { it.toString() }))
                    },
                state = listState,
            ) {
                items(
                    items = rows,
                    key = { row ->
                        when (row) {
                            is PathTreeRow.Folder -> "d:${row.fullPath}"
                            is PathTreeRow.Item -> "l:${row.item.pathKey}"
                        }
                    },
                    contentType = { row ->
                        if (row is PathTreeRow.Folder) "files_folder" else "files_link"
                    },
                ) { row ->
                    FilesLinkRow(
                        row = row,
                        splitRatio = { updatedSplit.value.value },
                        typeLabels = typeLabels,
                        selectedProjectKeys = selectedProjectKeys,
                        focusedProjectUiKey = focusedProjectUiKey,
                        showSelection = showSelection,
                        shiftPressed = shiftPressed,
                        linkIndexByPathKey = linkIndexByPathKey,
                        allLinks = links,
                        lastClickedIndex = lastClickedIndex,
                        onToggleFolderExpanded = onToggleFolderExpanded,
                        onSplitDrag = onSplitDrag,
                        publish = publish,
                    )
                }
            }
            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                scrollState = listState,
            )
        }
    }
}

@Composable
fun FilesLinkListHeader(
    splitRatio: State<Float>,
    onSplitDrag: (deltaPx: Float, splitAreaPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilesSplitRow(
        ratio = splitRatio.value,
        modifier = modifier
            .fillMaxWidth()
            .height(FilesSplitHeaderHeight),
        gutterChange = null,
        onSplitDrag = onSplitDrag,
        pakkuCell = {
            Text("Pakku", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        },
        diskCell = {
            Text("File system", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        },
    )
}

@Composable
private fun FilesSplitRow(
    ratio: Float,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = FilesSplitHorizontalPadding,
    gutterChange: FilesChangeKind? = null,
    onSplitDrag: ((deltaPx: Float, splitAreaPx: Float) -> Unit)? = null,
    pakkuCell: @Composable () -> Unit,
    diskCell: @Composable () -> Unit,
) {
    val borderColor = JewelTheme.globalColors.borders.normal.copy(alpha = 0.22f)
    BoxWithConstraints(modifier) {
        val gutterWidth = FilesLinkGutterWidth
        val innerWidth = (maxWidth - horizontalPadding * 2 - gutterWidth).coerceAtLeast(0.dp)
        val minFraction = if (innerWidth > 0.dp) {
            (FilesSplitMinPaneWidth / innerWidth).coerceIn(0.05f, 0.45f)
        } else {
            0.05f
        }
        val r = ratio.coerceIn(minFraction, 1f - minFraction)
        val pakkuWidth = innerWidth * r
        val diskWidth = innerWidth - pakkuWidth
        val splitAreaPx = with(LocalDensity.current) { innerWidth.toPx().coerceAtLeast(1f) }

        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(pakkuWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                pakkuCell()
            }
            FilesSplitGutter(
                change = gutterChange,
                draggable = onSplitDrag != null,
                onDragSplit = { deltaPx -> onSplitDrag?.invoke(deltaPx, splitAreaPx) },
                borderColor = borderColor,
            )
            Box(
                Modifier
                    .width(diskWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                diskCell()
            }
        }
    }
}

@Composable
fun FilesLinkRow(
    row: PathTreeRow<FilesLink>,
    splitRatio: () -> Float,
    typeLabels: Map<String, String>,
    selectedProjectKeys: Set<String>,
    focusedProjectUiKey: String?,
    showSelection: Boolean,
    shiftPressed: Boolean,
    linkIndexByPathKey: Map<String, Int>,
    allLinks: List<FilesLink>,
    lastClickedIndex: MutableState<Int?>,
    onToggleFolderExpanded: (String) -> Unit,
    onSplitDrag: (deltaPx: Float, splitAreaPx: Float) -> Unit,
    publish: (ModpackMsg) -> Unit,
) {
    RecomposeProbe("FilesLinkRow")
    val ratio = splitRatio().coerceIn(0.05f, 0.95f)
    FilesSplitRow(
        ratio = ratio,
        modifier = Modifier
            .fillMaxWidth()
            .height(TreeRowMinHeight)
            .testTag("FilesLinkRow"),
        gutterChange = (row as? PathTreeRow.Item)?.item?.change,
        onSplitDrag = onSplitDrag,
        pakkuCell = {
            when (row) {
                is PathTreeRow.Folder -> PakkuFolderCell(row, typeLabels)
                is PathTreeRow.Item -> PakkuLinkCell(
                    link = row.item,
                    focused = row.item.pakkuProject()?.uiKey() == focusedProjectUiKey,
                    checked = row.item.selectionKey in selectedProjectKeys,
                    publish = publish,
                    lastClickedIndex = lastClickedIndex,
                    linkIndex = linkIndexByPathKey[row.item.pathKey] ?: 0,
                    shiftPressed = shiftPressed,
                    allLinks = allLinks,
                )
            }
        },
        diskCell = {
            when (row) {
                is PathTreeRow.Folder -> TreeFolderRow(
                    row = row,
                    onToggleExpanded = { onToggleFolderExpanded(row.fullPath) },
                    onToggleSelected = null,
                    modifier = Modifier.fillMaxSize(),
                )
                is PathTreeRow.Item -> {
                    val link = row.item
                    val checked = link.selectionKey?.let { it in selectedProjectKeys }
                    TreeItemRow(
                        depth = row.depth,
                        label = link.path.substringAfterLast('/'),
                        status = link.state.treeStatus(),
                        checked = if (showSelection) checked else null,
                        onCheckedChange = { next ->
                            toggleLinkSelection(
                                link = link,
                                nextChecked = next,
                                shiftPressed = shiftPressed,
                                linkIndex = linkIndexByPathKey[link.pathKey] ?: 0,
                                allLinks = allLinks,
                                lastClickedIndex = lastClickedIndex,
                                publish = publish,
                            )
                        },
                        highlighted = link.pakkuProject()?.uiKey() == focusedProjectUiKey,
                        dimmed = !link.onDisk,
                        paintHighlightOnStripe = false,
                        onClick = {
                            link.pakkuProject()?.let { publish(ModpackMsg.ProjectSelected(it)) }
                        },
                        modifier = Modifier.fillMaxSize(),
                        interactionKey = link.pathKey,
                        trailing = {
                            DiskLinkTrailing(link)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun DiskLinkTrailing(link: FilesLink) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val change = link.change
        if (change != null && change.direction() == FilesDirection.ToDisk) {
            Badge(style = change.actionBadgeStyle()) {
                Text(change.actionLabel())
            }
        }
        if (link.onDisk) {
            val diskPath = remember(link.path) { Path(workingPath).resolve(link.path) }
            PkUiTooltip(tooltip = { Text("Show in folder") }) {
                IconButton(
                    onClick = { revealInFileManager(diskPath) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        key = AllIconsKeys.General.OpenDisk,
                        contentDescription = "Show in folder",
                        tint = Color.Gray,
                        hints = arrayOf(),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PakkuFolderCell(row: PathTreeRow.Folder, typeLabels: Map<String, String>) {
    val panel = JewelTheme.globalColors.panelBackground
    val content = JewelTheme.contentColor
    val folderFill = lerp(panel, content, if (JewelTheme.isDark) 0.06f else 0.04f)
    val typeLabel = typeLabels["${row.fullPath}/"]
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(folderFill)
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (typeLabel != null) {
            Text(
                text = typeLabel,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${row.subtreeItemCount}",
                color = JewelTheme.contentColor.copy(alpha = 0.45f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PakkuLinkCell(
    link: FilesLink,
    focused: Boolean,
    checked: Boolean,
    publish: (ModpackMsg) -> Unit,
    lastClickedIndex: MutableState<Int?>,
    linkIndex: Int,
    shiftPressed: Boolean,
    allLinks: List<FilesLink>,
) {
    val bg = pakkuRowBackground(link, focused, checked)
    val project = link.pakkuProject()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .then(
                if (project != null) {
                    Modifier.clickable(
                        interactionSource = remember(link.pathKey) { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            publish(ModpackMsg.ProjectSelected(project))
                            lastClickedIndex.value = linkIndex
                        },
                    )
                } else {
                    Modifier
                },
            )
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PakkuLinkContent(link = link)
    }
}

@Composable
private fun PakkuLinkContent(link: FilesLink) {
    when (val entry = link.pakku) {
        is PakkuEntry.Tracked -> {
            ProjectRowCompact(
                project = entry.project,
                continuation = entry.continuation,
                modifier = Modifier.fillMaxWidth(),
                trailing = {
                    val change = link.change
                    if (change != null && change.direction() == FilesDirection.ToPakku) {
                        Badge(style = change.actionBadgeStyle()) {
                            Text(change.actionLabel())
                        }
                    }
                },
            )
        }
        is PakkuEntry.Detected -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProjectRowCompact(
                    project = entry.project,
                    ghost = true,
                    modifier = Modifier.weight(1f),
                    trailing = {
                        val change = link.change
                        if (change != null && change.direction() == FilesDirection.ToPakku) {
                            Badge(style = change.actionBadgeStyle()) {
                                Text(change.actionLabel())
                            }
                        }
                    },
                )
                Text(
                    text = "detected",
                    fontSize = 11.sp,
                    color = JewelTheme.contentColor.copy(alpha = 0.5f),
                )
            }
        }
        is PakkuEntry.Override -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    key = AllIconsKeys.General.Settings,
                    contentDescription = null,
                    tint = JewelTheme.contentColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Manual override",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = overrideSourceFolderName(entry.sourcePath),
                    fontSize = 12.sp,
                    color = JewelTheme.contentColor.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        null -> {
            Text(
                text = if (link.unrecognized) "Not on any platform" else "Not in Pakku",
                color = JewelTheme.contentColor.copy(alpha = 0.55f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun overrideSourceFolderName(sourcePath: String): String {
    val name = Path(sourcePath).parent?.fileName?.toString()
    return name ?: sourcePath.substringAfterLast('/')
}

@Composable
private fun pakkuRowBackground(link: FilesLink, focused: Boolean, checked: Boolean): Color {
    val content = JewelTheme.contentColor
    val kindFill = link.change?.lineBackground() ?: Color.Transparent
    val selectedFill = PakkuDesktopConstants.highlightColor.copy(alpha = if (JewelTheme.isDark) 0.14f else 0.10f)
    val hoverFill = content.copy(alpha = if (JewelTheme.isDark) 0.07f else 0.06f)
    val base = when {
        focused -> lerp(kindFill, selectedFill, if (kindFill == Color.Transparent) 1f else 0.55f)
        kindFill != Color.Transparent -> kindFill
        else -> Color.Transparent
    }
    return when {
        checked && !focused -> lerp(base, selectedFill, 0.45f)
        else -> base
    }
}

@Composable
private fun FilesSplitGutter(
    change: FilesChangeKind?,
    draggable: Boolean,
    onDragSplit: (Float) -> Unit,
    borderColor: Color,
) {
    Box(
        modifier = Modifier
            .width(FilesLinkGutterWidth)
            .fillMaxHeight()
            .then(
                if (draggable) {
                    Modifier
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                onDragSplit(dragAmount)
                            }
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(borderColor),
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                DirectionGutter(change)
            }
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(borderColor),
            )
        }
    }
}

@Composable
fun DirectionGutter(change: FilesChangeKind?) {
    val direction = change?.direction()
    when (direction) {
        FilesDirection.ToDisk -> {
            Icon(
                key = AllIconsKeys.General.ChevronRight,
                contentDescription = FilesDirectionToDiskDescription,
                tint = change.statusColor(),
                hints = arrayOf(),
                modifier = Modifier.size(14.dp),
            )
        }
        FilesDirection.ToPakku -> {
            Icon(
                key = AllIconsKeys.General.ChevronLeft,
                contentDescription = FilesDirectionToPakkuDescription,
                tint = change.statusColor(),
                hints = arrayOf(),
                modifier = Modifier.size(14.dp),
            )
        }
        null -> Unit
    }
}

private fun FilesLink.pakkuProject(): Project? = when (val entry = pakku) {
    is PakkuEntry.Tracked -> entry.project
    is PakkuEntry.Detected -> entry.project
    else -> null
}

private fun toggleLinkSelection(
    link: FilesLink,
    nextChecked: Boolean,
    shiftPressed: Boolean,
    linkIndex: Int,
    allLinks: List<FilesLink>,
    lastClickedIndex: MutableState<Int?>,
    publish: (ModpackMsg) -> Unit,
) {
    val selectionKey = link.selectionKey ?: return
    if (shiftPressed && lastClickedIndex.value != null) {
        val start = minOf(lastClickedIndex.value!!, linkIndex)
        val end = maxOf(lastClickedIndex.value!!, linkIndex)
        val keys = allLinks.slice(start..end)
            .mapNotNull { it.selectionKey }
            .toSet()
        if (nextChecked) publish(ModpackMsg.ProjectsSelected(keys))
        else publish(ModpackMsg.ProjectsDeselected(keys))
    } else {
        if (nextChecked) publish(ModpackMsg.ProjectsSelected(setOf(selectionKey)))
        else publish(ModpackMsg.ProjectsDeselected(setOf(selectionKey)))
        lastClickedIndex.value = linkIndex
    }
}

/** Remember expanded folder paths across link list updates. */
@Composable
fun rememberFilesExpandedFolders(links: List<FilesLink>): Pair<Set<String>, (String) -> Unit> {
    var expanded by remember { mutableStateOf(emptySet<String>()) }
    var previousPaths by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(links) {
        val paths = links.map { it.path }
        expanded = mergeExpandedFolders(expanded, previousPaths, paths)
        previousPaths = paths
    }
    val toggle: (String) -> Unit = { folder ->
        expanded = if (folder in expanded) expanded - folder else expanded + folder
    }
    return expanded to toggle
}

@Composable
fun rememberFilesLinkListSplitRatio(
    modelRatio: Float,
    publish: (ModpackMsg) -> Unit,
): MutableState<Float> {
    val ratioState = remember { mutableStateOf(modelRatio.coerceIn(0.05f, 0.95f)) }
    LaunchedEffect(modelRatio) {
        if (kotlin.math.abs(ratioState.value - modelRatio) > 0.01f) {
            ratioState.value = modelRatio.coerceIn(0.05f, 0.95f)
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { ratioState.value }
            .drop(1)
            .distinctUntilChanged()
            .debounce(200)
            .collect { ratio ->
                if (ratio in 0.05f..0.95f) {
                    publish(ModpackMsg.FilesSplitRatioChanged(ratio))
                }
            }
    }
    return ratioState
}
