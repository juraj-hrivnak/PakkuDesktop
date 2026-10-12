/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Badge
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.integration.revealInFileManager
import teksturepako.pakkuDesktop.app.ui.LocalShiftKeyState
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.button.CopyToClipboardButton
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.modifier.allowDragAndDrop
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeToOrNull

private val FilesDiffRowMinHeight = 40.dp
private val FilesDiffStripeWidth = 3.dp

/**
 * Jewel has no linked-scroller API; IntelliJ diffs keep both sides on one item stream.
 * Pair identical [LazyColumn] items and follow the side that is actively scrolling.
 */
@Composable
fun SyncLinkedLazyLists(first: LazyListState, second: LazyListState) {
    LaunchedEffect(first, second) {
        snapshotFlow {
            Triple(first.isScrollInProgress, first.firstVisibleItemIndex, first.firstVisibleItemScrollOffset)
        }.collect { (inProgress, index, offset) ->
            if (inProgress &&
                (second.firstVisibleItemIndex != index || second.firstVisibleItemScrollOffset != offset)
            ) {
                second.scrollToItem(index, offset)
            }
        }
    }
    LaunchedEffect(first, second) {
        snapshotFlow {
            Triple(second.isScrollInProgress, second.firstVisibleItemIndex, second.firstVisibleItemScrollOffset)
        }.collect { (inProgress, index, offset) ->
            if (inProgress &&
                (first.firstVisibleItemIndex != index || first.firstVisibleItemScrollOffset != offset)
            ) {
                first.scrollToItem(index, offset)
            }
        }
    }
}

@Composable
fun FilesDiffList(
    side: FilesDiffSide,
    publish: (ModpackMsg) -> Unit,
    model: FilesTabModel,
    pairs: List<FilesDiffPair>,
    scrollState: LazyListState,
    collapsedFolders: MutableState<Set<String>>,
    lastClickedIndex: MutableState<Int?>,
    showScrollbar: Boolean,
    showSelection: Boolean,
    allowDrop: Boolean,
) {
    if (model.lockFile?.get() == null) return
    val shiftPressed = LocalShiftKeyState.current.pressed
    val focusedKey = model.selectedProject?.uiKey()
    val groups = remember(pairs) {
        pairs.groupBy { it.folder }.toSortedMap()
    }
    val indexByKey = remember(pairs) {
        pairs.withIndex().associate { it.value.listKey to it.index }
    }

    Box(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .then(
                    if (allowDrop) {
                        Modifier.allowDragAndDrop { paths ->
                            publish(ModpackMsg.FilesDropped(paths.map { it.toString() }))
                        }
                    } else {
                        Modifier
                    },
                ),
            state = scrollState,
        ) {
            groups.forEach { (folder, folderPairs) ->
                val expanded = folder !in collapsedFolders.value
                stickyHeader(key = "folder:$folder") { _ ->
                    FolderHeader(
                        folder = folder,
                        count = folderPairs.size,
                        expanded = expanded,
                        showCheckboxSlot = showSelection && side == FilesDiffSide.Current,
                        canReveal = folder in model.diskSnapshot.existingFolders,
                        onToggle = {
                            collapsedFolders.value = if (expanded) {
                                collapsedFolders.value + folder
                            } else {
                                collapsedFolders.value - folder
                            }
                        },
                    )
                }
                if (!expanded) return@forEach
                itemsIndexed(
                    items = folderPairs,
                    key = { _, pair -> pair.listKey },
                    contentType = { _, _ -> "files_diff_row" },
                ) { _, pair ->
                    val index = indexByKey[pair.listKey] ?: 0
                    val project = pair.left?.project ?: pair.right?.project
                    FilesDiffRow(
                        side = side,
                        pair = pair,
                        index = index,
                        pairs = pairs,
                        checked = pair.selectionKey in model.selectedProjectKeys,
                        focused = project?.uiKey() == focusedKey,
                        shiftPressed = shiftPressed,
                        lastClickedIndex = lastClickedIndex,
                        publish = publish,
                        showSelection = showSelection,
                    )
                }
            }
        }

        if (showScrollbar) {
            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                scrollState = scrollState,
            )
        }
    }
}

@Composable
private fun FilesDiffRow(
    side: FilesDiffSide,
    pair: FilesDiffPair,
    index: Int,
    pairs: List<FilesDiffPair>,
    checked: Boolean,
    focused: Boolean,
    shiftPressed: Boolean,
    lastClickedIndex: MutableState<Int?>,
    publish: (ModpackMsg) -> Unit,
    showSelection: Boolean,
) {
    val row = when (side) {
        FilesDiffSide.Current -> pair.left
        FilesDiffSide.Proposed -> pair.right
    }
    val empty = row == null
    val kind = pair.changeKind
    val project = pair.left?.project ?: pair.right?.project
    val interaction = remember(pair.listKey, side) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = filesDiffRowBackground(
        kind = kind,
        checked = checked,
        hovered = hovered,
        focused = focused,
    )
    val stripe = when {
        kind != null -> kind.statusColor()
        focused || checked -> PakkuDesktopConstants.highlightColor
        else -> Color.Transparent
    }
    val pathText = row?.fileName.orEmpty()
    val diskPath = remember(row?.revealPath, row?.relativePath, row?.fileName) {
        row?.revealPath?.let { Path(it) }
            ?: row?.relativePath?.takeIf { it.isNotBlank() }?.let { Path(workingPath).resolve(it) }
    }
    val canOpen = row?.onDisk == true && diskPath != null

    fun applyChecked(nextChecked: Boolean) {
        if (!pair.selectable) return
        if (shiftPressed && lastClickedIndex.value != null) {
            val start = minOf(lastClickedIndex.value!!, index)
            val end = maxOf(lastClickedIndex.value!!, index)
            val keys = pairs.slice(start..end)
                .filter { it.selectable }
                .map { it.selectionKey }
                .toSet()
            if (nextChecked) publish(ModpackMsg.ProjectsSelected(keys))
            else publish(ModpackMsg.ProjectsDeselected(keys))
        } else {
            if (nextChecked) publish(ModpackMsg.ProjectsSelected(setOf(pair.selectionKey)))
            else publish(ModpackMsg.ProjectsDeselected(setOf(pair.selectionKey)))
            lastClickedIndex.value = index
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilesDiffRowMinHeight)
            .background(bg)
            .hoverable(interaction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (side == FilesDiffSide.Current) {
            Spacer(
                Modifier
                    .width(FilesDiffStripeWidth)
                    .fillMaxHeight()
                    .background(stripe),
            )
        }
        if (showSelection && side == FilesDiffSide.Current) {
            Box(
                modifier = Modifier
                    .width(ProjectsListCheckboxColumnWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (pair.selectable) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { applyChecked(it) },
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        } else if (side == FilesDiffSide.Current) {
            Spacer(Modifier.width(12.dp))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (project != null) {
                        Modifier.clickable(
                            interactionSource = remember(pair.listKey, side, "project") { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                publish(ModpackMsg.ProjectSelected(project))
                                lastClickedIndex.value = index
                            },
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(start = if (side == FilesDiffSide.Proposed) 12.dp else 0.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!empty) {
                FilesPathChip(
                    path = pathText,
                    modifier = Modifier.weight(1f, fill = false),
                )
                CopyToClipboardButton(text = pathText)
                if (canOpen) {
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
            val syncFrom = if (kind == FilesChangeKind.Override) {
                (row ?: pair.right)?.sourcePath?.let { pathRelativeToWorking(it) }
            } else {
                null
            }
            if (side == FilesDiffSide.Proposed && !syncFrom.isNullOrBlank()) {
                Text(
                    text = "from $syncFrom",
                    color = JewelTheme.contentColor.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else if (empty) {
                Spacer(Modifier.weight(1f))
            }
            if (side == FilesDiffSide.Proposed && kind != null) {
                Badge(style = kind.actionBadgeStyle()) {
                    Text(kind.actionLabel())
                }
            }
        }
    }
}

private fun pathRelativeToWorking(absolute: String): String {
    val work = Path(workingPath).toAbsolutePath().normalize()
    val src = Path(absolute).toAbsolutePath().normalize()
    return src.relativeToOrNull(work)?.invariantSeparatorsPathString ?: src.invariantSeparatorsPathString
}

@Composable
private fun FilesPathChip(path: String, modifier: Modifier = Modifier) {
    val codeBg = JewelTheme.globalColors.borders.disabled.copy(alpha = 0.45f)
    val chipShape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(chipShape)
            .background(codeBg)
            .border(
                width = 1.dp,
                color = JewelTheme.globalColors.borders.normal.copy(alpha = 0.55f),
                shape = chipShape,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = path,
            color = JewelTheme.contentColor.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun filesDiffRowBackground(
    kind: FilesChangeKind?,
    checked: Boolean,
    hovered: Boolean,
    focused: Boolean,
): Color {
    val content = JewelTheme.contentColor
    val kindFill = kind?.lineBackground() ?: Color.Transparent
    val selectedFill = PakkuDesktopConstants.highlightColor.copy(alpha = if (JewelTheme.isDark) 0.14f else 0.10f)
    val hoverFill = content.copy(alpha = if (JewelTheme.isDark) 0.07f else 0.06f)
    val base = when {
        focused -> lerp(kindFill, selectedFill, if (kindFill == Color.Transparent) 1f else 0.55f)
        kindFill != Color.Transparent -> kindFill
        else -> Color.Transparent
    }
    return when {
        hovered && (focused || checked) -> lerp(base, hoverFill, 0.35f)
        hovered -> if (base == Color.Transparent) hoverFill else lerp(base, hoverFill, 0.45f)
        checked && !focused -> lerp(base, selectedFill, 0.45f)
        else -> base
    }
}

@Composable
private fun FolderHeader(
    folder: String,
    count: Int,
    expanded: Boolean,
    showCheckboxSlot: Boolean,
    canReveal: Boolean,
    onToggle: () -> Unit,
) {
    val codeBg = JewelTheme.globalColors.borders.disabled.copy(alpha = 0.45f)
    val chipShape = RoundedCornerShape(4.dp)
    val folderPath = remember(folder) {
        Path(workingPath).resolve(folder.trimEnd('/', '\\'))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JewelTheme.globalColors.panelBackground)
            .padding(vertical = 4.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(if (showCheckboxSlot) ProjectsListCheckboxColumnWidth else 24.dp)
                .clickableHover(scaleOnHover = false, onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                key = if (expanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
                contentDescription = if (expanded) "Collapse $folder" else "Expand $folder",
                tint = JewelTheme.contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickableHover(scaleOnHover = false, onClick = onToggle)
                    .clip(chipShape)
                    .background(codeBg)
                    .border(
                        width = 1.dp,
                        color = JewelTheme.globalColors.borders.normal.copy(alpha = 0.55f),
                        shape = chipShape,
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = folder.ifBlank { "/" },
                    color = JewelTheme.contentColor.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CopyToClipboardButton(text = folder.ifBlank { "/" })
            if (canReveal) {
                PkUiTooltip(tooltip = { Text("Show in folder") }) {
                    IconButton(
                        onClick = { revealInFileManager(folderPath) },
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
            Text(
                text = "$count",
                color = JewelTheme.contentColor.copy(alpha = 0.45f),
                fontSize = 12.sp,
            )
        }
    }
}
