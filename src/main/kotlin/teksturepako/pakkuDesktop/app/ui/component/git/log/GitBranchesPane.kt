/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git.log

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.git.GitBranch
import teksturepako.pakkuDesktop.app.git.GitRef
import teksturepako.pakkuDesktop.app.git.GitRefType
import teksturepako.pakkuDesktop.app.ui.component.git.log.gitLogTagMenu
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiContextMenuArea

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GitBranchesPane(
    refs: List<GitRef>,
    headHash: String?,
    currentBranch: String?,
    busy: Boolean,
    onJump: (String) -> Unit,
    onFilter: (String) -> Unit,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    val local = refs.filter { it.type == GitRefType.LocalBranch }.sortedBy { it.name }
    val remote = refs.filter { it.type == GitRefType.RemoteBranch }.groupBy { it.name.substringBefore('/') }
    val tags = refs.filter { it.type == GitRefType.Tag }
        .sortedWith(compareByDescending<GitRef> { it.createdAt ?: kotlinx.datetime.Instant.DISTANT_PAST }.thenBy { it.name })
    val scrollState = rememberLazyListState()
    Row(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).fillMaxHeight(), state = scrollState) {
            if (headHash != null) {
                item(key = "head") {
                    BranchHeader("HEAD")
                    BranchRow(
                        label = currentBranch ?: "HEAD detached",
                        current = true,
                        onClick = { onJump(headHash) },
                        onDoubleClick = { currentBranch?.let(onFilter) },
                        menu = {
                            val branch = currentBranch?.let { GitBranch(it, isRemote = false, isCurrent = true) }
                            if (branch == null) emptyList() else gitLogBranchMenu(branch, busy, publish)
                        },
                    )
                }
            }
            if (local.isNotEmpty()) {
                item(key = "local-header") { BranchHeader("Local") }
                items(local, key = { "local-${it.name}" }) { ref ->
                    val branch = GitBranch(ref.name, isRemote = false, isCurrent = ref.name == currentBranch)
                    BranchRow(
                        label = ref.name,
                        current = branch.isCurrent,
                        onClick = { onJump(ref.hash) },
                        onDoubleClick = { onFilter(ref.name) },
                        menu = { gitLogBranchMenu(branch, busy, publish) },
                    )
                }
            }
            remote.toSortedMap().forEach { (remoteName, branches) ->
                item(key = "remote-$remoteName") { BranchHeader(remoteName) }
                items(branches.sortedBy { it.name }, key = { "remote-${it.name}" }) { ref ->
                    val branch = GitBranch(ref.name, isRemote = true, isCurrent = false)
                    BranchRow(
                        label = ref.name.substringAfter('/'),
                        current = false,
                        onClick = { onJump(ref.hash) },
                        onDoubleClick = { onFilter(ref.name) },
                        menu = { gitLogBranchMenu(branch, busy, publish) },
                    )
                }
            }
            if (tags.isNotEmpty()) {
                item(key = "tags-header") { BranchHeader("Tags") }
                items(tags, key = { "tag-${it.name}" }) { ref ->
                    BranchRow(
                        label = ref.name,
                        current = false,
                        onClick = { onJump(ref.hash) },
                        onDoubleClick = {},
                        menu = { gitLogTagMenu(ref, busy, publish) },
                        iconKey = AllIconsKeys.Nodes.Tag,
                    )
                }
            }
        }
        VerticalScrollbar(
            scrollState = scrollState,
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

@Composable
private fun BranchHeader(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 2.dp),
        color = JewelTheme.contentColor.copy(alpha = 0.55f),
        maxLines = 1,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BranchRow(
    label: String,
    current: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    menu: () -> List<androidx.compose.foundation.ContextMenuItem>,
    iconKey: IconKey? = null,
) {
    PkUiContextMenuArea(items = menu) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
                .padding(horizontal = 16.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconKey != null) {
                Icon(
                    key = iconKey,
                    contentDescription = null,
                    tint = JewelTheme.contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp).padding(end = 6.dp),
                )
            }
            Text(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (current) JewelTheme.contentColor else JewelTheme.contentColor.copy(alpha = 0.85f),
            )
        }
    }
}
