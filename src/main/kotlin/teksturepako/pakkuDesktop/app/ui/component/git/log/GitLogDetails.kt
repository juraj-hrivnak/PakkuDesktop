/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.git.GitCommitChange
import teksturepako.pakkuDesktop.app.git.GitCommitDetails
import teksturepako.pakkuDesktop.app.git.toTreeStatus
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeRow
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeFolderRow
import teksturepako.pakkuDesktop.app.ui.component.tree.TreeItemRow
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun GitLogDetails(
    details: GitCommitDetails?,
    containing: List<String>?,
    selectedChange: GitCommitChange?,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        if (details == null) {
            Text(
                "Select a commit to see its changes.",
                modifier = Modifier.padding(12.dp),
                color = JewelTheme.contentColor.copy(alpha = 0.6f),
            )
            return@Column
        }
        var expanded by remember(details.hash) { mutableStateOf(details.changes.mapNotNull { parentFolders(it.path) }.flatten().toSet()) }
        val tree = remember(details.changes) { PathTreeSnapshot.of(details.changes) { it.path } }
        val rows = tree.flatRows(expanded, setOfNotNull(selectedChange?.path))
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(rows, key = { row ->
                when (row) {
                    is PathTreeRow.Folder -> "f:${row.fullPath}"
                    is PathTreeRow.Item -> "i:${row.path}"
                }
            }) { row ->
                when (row) {
                    is PathTreeRow.Folder -> TreeFolderRow(
                        row = row,
                        onToggleExpanded = {
                            expanded = if (row.fullPath in expanded) expanded - row.fullPath else expanded + row.fullPath
                        },
                        onToggleSelected = null,
                    )
                    is PathTreeRow.Item -> TreeItemRow(
                        depth = row.depth,
                        label = row.item.status.displayName,
                        status = row.item.status.toTreeStatus(),
                        checked = null,
                        onCheckedChange = {},
                        highlighted = row.item == selectedChange,
                        onClick = { publish(ModpackMsg.GitLogChangeSelected(row.item)) },
                        interactionKey = row.path,
                    )
                }
            }
        }
        Divider(Orientation.Horizontal)
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            val subject = details.message.lineSequence().firstOrNull().orEmpty()
            val body = details.message.lineSequence().drop(1).joinToString("\n").trim()
            Text(subject, fontWeight = FontWeight.SemiBold, color = JewelTheme.contentColor)
            if (body.isNotEmpty()) {
                Text(body, modifier = Modifier.padding(top = 6.dp), color = JewelTheme.contentColor)
            }
            Text(
                commitLine(details),
                modifier = Modifier.padding(top = 8.dp),
                color = JewelTheme.contentColor.copy(alpha = 0.75f),
            )
            if (details.committerName != details.authorName || details.committerEmail != details.authorEmail) {
                Text(
                    "committed by ${details.committerName} <${details.committerEmail}>",
                    color = JewelTheme.contentColor.copy(alpha = 0.75f),
                )
            }
            if (containing != null) {
                Text(
                    "In ${containing.size} branches: ${containing.joinToString(", ")}",
                    modifier = Modifier.padding(top = 4.dp),
                    color = JewelTheme.contentColor.copy(alpha = 0.75f),
                )
            }
        }
    }
}

private fun parentFolders(path: String): List<String>? {
    val parts = path.split('/').filter { it.isNotEmpty() }
    if (parts.size < 2) return null
    val folders = ArrayList<String>(parts.size - 1)
    var acc = parts[0]
    folders.add(acc)
    for (index in 1 until parts.lastIndex) {
        acc = "$acc/${parts[index]}"
        folders.add(acc)
    }
    return folders
}

private val detailDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val detailTime: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun commitLine(details: GitCommitDetails): String {
    val zoned = java.time.Instant.ofEpochSecond(details.authorTime.epochSeconds).atZone(ZoneId.systemDefault())
    return "${details.hash.take(8)} ${details.authorName} <${details.authorEmail}> on ${detailDate.format(zoned)} at ${detailTime.format(zoned)}"
}
