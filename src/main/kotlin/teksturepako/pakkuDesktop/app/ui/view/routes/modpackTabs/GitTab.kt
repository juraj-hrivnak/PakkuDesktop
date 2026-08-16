/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.colorPalette
import org.jetbrains.jewel.ui.theme.treeStyle
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.animatedColor
import teksturepako.pakkuDesktop.elm.animatedDividerStyle
import teksturepako.pakkuDesktop.pro.git.wrapper.GitChangelistFlatRow
import teksturepako.pakkuDesktop.pro.git.wrapper.GitState
import teksturepako.pakkuDesktop.pro.git.wrapper.gitChangelistUiSnapshot
import teksturepako.pakkuDesktop.pro.ui.component.diff.DiffViewer
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.GitChange

private val ChangelistIndentPerLevel = 20.dp
private val ChangelistChevronSlot = 20.dp
private val ChangelistChevronGap = 4.dp
private val ChangelistCheckSlot = 32.dp
/** Status letter (A/M/D) or folder icon: same width keeps file and folder names aligned. */
private val ChangelistStatusSlot = 20.dp
private val ChangelistRowMinHeight = 30.dp
private val ChangelistSelectionStripeWidth = 3.dp

@Composable
fun GitTab(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    val gitState = model.git
    val diffContent = model.gitCurrentDiff

    val splitState = remember { SplitLayoutState(0.32f) }

    val diffPendingPath = model.gitDiffPendingFile?.path

    Column(Modifier.fillMaxSize()) {
        HorizontalSplitLayout(
            state = splitState,
            dividerStyle = animatedDividerStyle(),
            first = {
                SourceControlSidePanel(
                    gitState = gitState,
                    currentDiff = diffContent,
                    diffPendingPath = diffPendingPath,
                    publish = publish,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            second = {
                DiffViewer(
                    diffContent,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            firstPaneMinWidth = 240.dp,
            secondPaneMinWidth = 200.dp,
            draggableWidth = 16.dp,
        )
    }
}

@Composable
private fun SourceControlSidePanel(
    gitState: GitState,
    currentDiff: DiffContent?,
    diffPendingPath: String?,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    val files = gitState.gitFiles
    val selectedCount = gitState.selectedFiles.size

    val panelBg = animatedColor(JewelTheme.globalColors.panelBackground)
    val borderColor = animatedColor(JewelTheme.globalColors.borders.normal)

    val changelistSnapshot = remember(files) { gitChangelistUiSnapshot(files) }
    val folderIds = changelistSnapshot.folderIds
    val selectedPaths = remember(gitState.selectedFiles) {
        gitState.selectedFiles.map { it.path }.toSet()
    }
    val flatRows = remember(changelistSnapshot, gitState.expandedFolderPaths, selectedPaths) {
        changelistSnapshot.flatRows(gitState.expandedFolderPaths, selectedPaths)
    }

    Column(
        modifier
            .background(panelBg)
            .fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = when {
                    files.isEmpty() -> "no local changes"
                    files.size == 1 -> "1 file"
                    else -> "${files.size} files"
                },
                fontSize = 12.sp,
                color = JewelTheme.contentColor.copy(alpha = 0.55f),
            )
        }

        ChangelistToolbar(
            filesEmpty = files.isEmpty(),
            selectedCount = selectedCount,
            hasFolders = folderIds.isNotEmpty(),
            publish = publish,
        )

        Spacer(Modifier.background(borderColor).height(1.dp).fillMaxWidth())

        if (files.isEmpty()) {
            Text(
                text = "No local changes",
                modifier = Modifier
                    .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 18.dp)
                    .fillMaxWidth(),
                style = JewelTheme.defaultTextStyle,
                color = JewelTheme.contentColor.copy(alpha = 0.55f),
            )
            Text(
                text = "Edits to tracked files and new files will appear in this list.",
                modifier = Modifier
                    .padding(horizontal = PakkuDesktopConstants.commonPaddingSize)
                    .fillMaxWidth(),
                style = JewelTheme.defaultTextStyle,
                color = JewelTheme.contentColor.copy(alpha = 0.45f),
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            ) {
                items(
                    items = flatRows,
                    key = { row ->
                        when (row) {
                            is GitChangelistFlatRow.Folder -> "d:${row.fullPath}"
                            is GitChangelistFlatRow.File -> "f:${row.file.path}"
                        }
                    },
                    contentType = { row ->
                        when (row) {
                            is GitChangelistFlatRow.Folder -> "changelist_folder"
                            is GitChangelistFlatRow.File -> "changelist_file"
                        }
                    },
                ) { row ->
                    GitChangesListRow(
                        row = row,
                        currentDiff = currentDiff,
                        diffPendingPath = diffPendingPath,
                        publish = publish,
                    )
                }
            }
        }

        Spacer(Modifier.background(borderColor).height(1.dp).fillMaxWidth())

        CommitPanel(
            gitState = gitState,
            publish = publish,
            canCommit = selectedCount > 0 && gitState.commitMessage.isNotBlank(),
            selectedCount = selectedCount,
            totalCount = files.size,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChangelistToolbar(
    filesEmpty: Boolean,
    selectedCount: Int,
    hasFolders: Boolean,
    publish: (ModpackMsg) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            enabled = !filesEmpty,
            onClick = { publish(ModpackMsg.GitSelectAllChangedFiles) },
        ) {
            Icon(
                key = AllIconsKeys.Actions.Selectall,
                contentDescription = "Select all",
                tint = JewelTheme.contentColor.copy(alpha = 0.9f),
                hints = arrayOf(),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(
            enabled = selectedCount > 0,
            onClick = { publish(ModpackMsg.GitClearChangedFileSelection) },
        ) {
            Icon(
                key = AllIconsKeys.Actions.Unselectall,
                contentDescription = "Clear selection",
                tint = JewelTheme.contentColor.copy(alpha = 0.9f),
                hints = arrayOf(),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            enabled = !filesEmpty && hasFolders,
            onClick = { publish(ModpackMsg.GitChangelistExpandAllFolders) },
        ) {
            Icon(
                key = AllIconsKeys.Actions.Expandall,
                contentDescription = "Expand all",
                tint = JewelTheme.contentColor.copy(alpha = 0.9f),
                hints = arrayOf(),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(
            enabled = !filesEmpty && hasFolders,
            onClick = { publish(ModpackMsg.GitChangelistCollapseAllFolders) },
        ) {
            Icon(
                key = AllIconsKeys.Actions.Collapseall,
                contentDescription = "Collapse all",
                tint = JewelTheme.contentColor.copy(alpha = 0.9f),
                hints = arrayOf(),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun GitChangesListRow(
    row: GitChangelistFlatRow,
    currentDiff: DiffContent?,
    diffPendingPath: String?,
    publish: (ModpackMsg) -> Unit,
) {
    when (row) {
        is GitChangelistFlatRow.Folder -> ChangelistFolderRow(row, publish)
        is GitChangelistFlatRow.File -> ChangelistFileRow(row, currentDiff, diffPendingPath, publish)
    }
}

@Composable
private fun ChangelistFolderRow(
    row: GitChangelistFlatRow.Folder,
    publish: (ModpackMsg) -> Unit,
) {
    val interaction = remember(row.fullPath) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val folderCheckState = when {
        row.subtreeFileCount == 0 -> ToggleableState.Off
        row.selectedInSubtree == 0 -> ToggleableState.Off
        row.selectedInSubtree == row.subtreeFileCount -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val stripe = Color.Transparent
    val bg = changelistRowBackground(
        selectedForDiff = false,
        hovered = hovered,
        folderBand = true,
    )
    val treeChevron = JewelTheme.treeStyle.icons.chevron(isExpanded = row.expanded, isSelected = false)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ChangelistRowMinHeight)
            .background(bg)
            .hoverable(interaction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionStripe(stripe, selectedForDiff = false)
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp, horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(ChangelistIndentPerLevel * row.depth))
            Box(
                modifier = Modifier
                    .size(ChangelistChevronSlot)
                    .clickable(
                        indication = null,
                        interactionSource = remember(row.fullPath) { MutableInteractionSource() },
                    ) {
                        publish(ModpackMsg.GitChangelistFolderExpansionToggled(row.fullPath))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    key = treeChevron,
                    contentDescription = if (row.expanded) "Collapse" else "Expand",
                    tint = JewelTheme.contentColor.copy(alpha = 0.65f),
                    hints = arrayOf(),
                    modifier = Modifier.size(13.dp),
                )
            }
            Spacer(Modifier.width(ChangelistChevronGap))
            Box(
                modifier = Modifier
                    .width(ChangelistCheckSlot)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                TriStateCheckbox(
                    state = folderCheckState,
                    onClick = { publish(ModpackMsg.GitFolderSelectionToggled(row.fullPath)) },
                )
            }
            Box(
                modifier = Modifier
                    .width(ChangelistStatusSlot)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    key = AllIconsKeys.Nodes.Folder,
                    contentDescription = null,
                    tint = JewelTheme.contentColor.copy(alpha = 0.85f),
                    hints = arrayOf(),
                    modifier = Modifier.size(17.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember(row.fullPath) { MutableInteractionSource() },
                    ) {
                        publish(ModpackMsg.GitChangelistFolderExpansionToggled(row.fullPath))
                    },
            ) {
                Text(
                    text = row.displayName,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = JewelTheme.contentColor,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ChangelistFileRow(
    row: GitChangelistFlatRow.File,
    currentDiff: DiffContent?,
    diffPendingPath: String?,
    publish: (ModpackMsg) -> Unit,
) {
    val file = row.file
    val included = row.selectedForCommit
    val isViewed =
        sameGitPath(file.path, diffPendingPath) ||
            sameGitPath(file.path, currentDiff?.newPath) ||
            sameGitPath(file.path, currentDiff?.oldPath)
    val interaction = remember(file.path) { MutableInteractionSource() }
    val diffClickInteraction = remember(file.path, "diffOpen") { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val status = file.status
    val label = gitStatusLetter(status)
    val statusColor = gitStatusColor(status)
    val pathLabel = file.path.substringAfterLast('/').ifEmpty { file.path }
    val stripe = if (isViewed) PakkuDesktopConstants.highlightColor else Color.Transparent
    val bg = changelistRowBackground(
        selectedForDiff = isViewed,
        hovered = hovered,
        folderBand = false,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ChangelistRowMinHeight)
            .background(bg)
            .hoverable(interaction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionStripe(stripe, selectedForDiff = isViewed)
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(ChangelistIndentPerLevel * row.depth))
            Spacer(Modifier.width(ChangelistChevronSlot))
            Spacer(Modifier.width(ChangelistChevronGap))
            Box(
                modifier = Modifier
                    .width(ChangelistCheckSlot)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Checkbox(
                    checked = included,
                    onCheckedChange = { publish(ModpackMsg.GitFileSelectionToggled(file)) },
                )
            }
            // Transparent overlay: Jewel Text can consume pointer input, so parent Row.clickable
            // often never fires on short names (e.g. pakku.json) — overlay receives hits first.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(ChangelistStatusSlot)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = statusColor,
                            style = JewelTheme.defaultTextStyle.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                    Text(
                        text = pathLabel,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isViewed) JewelTheme.contentColor else JewelTheme.contentColor.copy(alpha = 0.92f),
                        style = JewelTheme.defaultTextStyle.copy(
                            fontWeight = if (isViewed) FontWeight.Medium else FontWeight.Normal,
                        ),
                    )
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = diffClickInteraction,
                            indication = null,
                            onClick = { publish(ModpackMsg.GitDiffFileSelected(file)) },
                        ),
                )
            }
        }
    }
}

@Composable
private fun SelectionStripe(color: Color, selectedForDiff: Boolean) {
    Spacer(
        Modifier
            .width(ChangelistSelectionStripeWidth)
            .fillMaxHeight()
            .background(
                if (selectedForDiff) PakkuDesktopConstants.highlightColor
                else Color.Transparent,
            ),
    )
}

@Composable
@ReadOnlyComposable
private fun changelistRowBackground(
    selectedForDiff: Boolean,
    hovered: Boolean,
    folderBand: Boolean,
): Color {
    val content = JewelTheme.contentColor
    val panel = JewelTheme.globalColors.panelBackground
    val selectedFill = PakkuDesktopConstants.highlightColor.copy(alpha = if (JewelTheme.isDark) 0.14f else 0.10f)
    val hoverFill = content.copy(alpha = if (JewelTheme.isDark) 0.07f else 0.06f)
    val folderFill = lerp(panel, content, if (JewelTheme.isDark) 0.06f else 0.04f)
    return when {
        selectedForDiff && hovered -> lerp(selectedFill, hoverFill, 0.35f)
        selectedForDiff -> selectedFill
        hovered -> hoverFill
        folderBand -> folderFill
        else -> Color.Transparent
    }
}

/** Normalize repo-relative paths from JGit vs unified diff headers (slashes, `./`). */
private fun gitPathKey(repoRelative: String): String =
    repoRelative.replace('\\', '/').trim().removePrefix("./")

private fun sameGitPath(a: String, b: String?): Boolean =
    b != null && gitPathKey(a) == gitPathKey(b)

private fun gitStatusLetter(status: GitChange): String = when (status) {
    is GitChange.Added -> "A"
    is GitChange.Modified -> "M"
    is GitChange.Deleted -> "D"
    is GitChange.Untracked -> "U"
}

@Composable
private fun gitStatusColor(status: GitChange): Color {
    val p = JewelTheme.colorPalette
    val fallback = JewelTheme.contentColor
    fun pick(ramps: List<Color>): Color =
        ramps.getOrNull((ramps.lastIndex * 5 / 10).coerceIn(0, ramps.lastIndex)) ?: fallback
    return when (status) {
        is GitChange.Added -> pick(p.green)
        is GitChange.Modified -> pick(p.yellow)
        is GitChange.Deleted -> pick(p.red)
        is GitChange.Untracked -> JewelTheme.globalColors.text.disabled
    }
}

@Composable
private fun CommitPanel(
    gitState: GitState,
    publish: (ModpackMsg) -> Unit,
    canCommit: Boolean,
    selectedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val selectionLabel = when {
        totalCount == 0 -> "0 of 0 selected"
        selectedCount == 0 -> "0 of $totalCount selected"
        selectedCount == totalCount -> "All $selectedCount of $totalCount selected"
        else -> "$selectedCount of $totalCount selected"
    }

    Column(
        modifier = modifier.padding(
            horizontal = PakkuDesktopConstants.commonPaddingSize,
            vertical = 10.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val textFieldState = rememberTextFieldState(gitState.commitMessage)

        LaunchedEffect(gitState.commitMessage) {
            val fieldText = textFieldState.text.toString()
            if (fieldText != gitState.commitMessage) {
                textFieldState.edit {
                    delete(0, length)
                    insert(0, gitState.commitMessage)
                }
            }
        }

        LaunchedEffect(textFieldState.text) {
            publish(ModpackMsg.GitCommitMessageChanged(textFieldState.text.toString()))
        }

        TextField(
            textFieldState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp, max = 100.dp),
            placeholder = { Text("Commit message") },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = selectionLabel,
                color = JewelTheme.contentColor.copy(alpha = 0.7f),
                fontSize = 12.5.sp,
            )

            Spacer(Modifier.weight(1f))

            DefaultButton(
                enabled = canCommit,
                onClick = { publish(ModpackMsg.GitCommitRequested) },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        key = AllIconsKeys.Actions.Commit,
                        contentDescription = null,
                        tint = JewelTheme.contentColor,
                        hints = arrayOf(),
                        modifier = Modifier.size(16.dp),
                    )
                    Text("Commit")
                }
            }
        }
    }
}
