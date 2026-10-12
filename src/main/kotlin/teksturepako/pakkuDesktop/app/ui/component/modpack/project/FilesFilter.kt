/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.packFolderKeys
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.pathKey

@Composable
fun FilesFilter(
    publish: (ModpackMsg) -> Unit,
    model: FilesTabModel,
    focusRequester: FocusRequester,
) {
    val textFieldState = rememberTextFieldState(model.filesFilterText)
    val folders = remember(model.lockFile, model.configFile, model.manualOverrides) { model.packFolderKeys() }
    val presentKinds = remember(
        model.filesPreviewMode,
        model.fetchDownloads,
        model.fetchOverrides,
        model.fetchCleanup,
        model.syncAdded,
        model.syncRemoved,
        model.syncUpdated,
        model.filesHashMismatchKeys,
        model.manualOverrides,
        model.diskSnapshot,
    ) {
        buildSet {
            if (model.fetchDownloads.isNotEmpty()) add(FilesChangeKind.Fetch)
            if (model.fetchOverrides.isNotEmpty() ||
                model.manualOverrides.any { override ->
                    pathKey(override.relativeOutputPath) !in model.diskSnapshot.present
                }
            ) {
                add(FilesChangeKind.Override)
            }
            if (model.fetchCleanup.isNotEmpty()) add(FilesChangeKind.Cleanup)
            if (model.syncAdded.isNotEmpty()) add(FilesChangeKind.Added)
            if (model.syncRemoved.isNotEmpty()) add(FilesChangeKind.Removed)
            if (model.syncUpdated.isNotEmpty()) add(FilesChangeKind.Updated)
            if (model.filesHashMismatchKeys.isNotEmpty()) add(FilesChangeKind.HashMismatch)
        }
    }

    val activeCount = model.filesFilterFolders.size + model.filesFilterChangeKinds.size
    val anyFilterActive = activeCount > 0
    var filtersExpanded by remember { mutableStateOf(anyFilterActive) }
    LaunchedEffect(anyFilterActive) {
        if (anyFilterActive) filtersExpanded = true
    }

    LaunchedEffect(textFieldState.text) {
        val text = textFieldState.text.toString()
        if (text != model.filesFilterText) {
            publish(ModpackMsg.FilesFilterTextChanged(text))
        }
    }

    LaunchedEffect(model.wantsFocusProjectsFilter) {
        if (!model.wantsFocusProjectsFilter) return@LaunchedEffect
        focusRequester.requestFocus()
        publish(ModpackMsg.FocusProjectsFilterConsumed)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                textFieldState,
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { publish(ModpackMsg.FilesFilterFocusChanged(it.isFocused)) },
                placeholder = { Text("Search files...") },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { filtersExpanded = !filtersExpanded },
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        key = if (filtersExpanded) {
                            AllIconsKeys.General.ChevronDown
                        } else {
                            AllIconsKeys.General.ChevronRight
                        },
                        contentDescription = if (filtersExpanded) "Hide filters" else "Show filters",
                        tint = JewelTheme.contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (anyFilterActive) "Filters · $activeCount" else "Filters",
                        color = JewelTheme.contentColor.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                    )
                }
                if (anyFilterActive) {
                    Link(
                        text = "Clear",
                        onClick = {
                            publish(ModpackMsg.FilesFilterFoldersChanged(emptySet()))
                            publish(ModpackMsg.FilesFilterChangeKindsChanged(emptySet()))
                        },
                    )
                }
            }
        }

        if (filtersExpanded) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                folders.forEach { folder ->
                    val active = folder in model.filesFilterFolders
                    FilterToggle(
                        label = folder.trimEnd('/'),
                        active = active,
                        onClick = {
                            val next = if (active) {
                                model.filesFilterFolders - folder
                            } else {
                                model.filesFilterFolders + folder
                            }
                            publish(ModpackMsg.FilesFilterFoldersChanged(next))
                        },
                    )
                }
                presentKinds.forEach { kind ->
                    val active = kind in model.filesFilterChangeKinds
                    FilterToggle(
                        label = kind.filterLabel(),
                        active = active,
                        onClick = {
                            val next = if (active) {
                                model.filesFilterChangeKinds - kind
                            } else {
                                model.filesFilterChangeKinds + kind
                            }
                            publish(ModpackMsg.FilesFilterChangeKindsChanged(next))
                        },
                    )
                }
            }
        }
    }
}

private fun FilesChangeKind.filterLabel(): String = when (this) {
    FilesChangeKind.Fetch -> "Download"
    FilesChangeKind.Override -> "Manual override"
    FilesChangeKind.Cleanup -> "Delete"
    FilesChangeKind.Added -> "Add"
    FilesChangeKind.Removed -> "Remove"
    FilesChangeKind.Updated -> "Update"
    FilesChangeKind.HashMismatch -> "Hash mismatch"
}
