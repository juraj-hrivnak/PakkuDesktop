/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode

fun filesFooterChangeSummary(links: List<FilesLink>, mode: FilesPreviewMode): String =
    when (mode) {
        FilesPreviewMode.Fetch -> {
            val parts = buildList {
                val downloads = links.count { it.change == FilesChangeKind.Fetch }
                if (downloads > 0) {
                    add(if (downloads == 1) "1 to download" else "$downloads to download")
                }
                val overrides = links.count { it.change == FilesChangeKind.Override }
                if (overrides > 0) {
                    add(
                        if (overrides == 1) "1 manual override"
                        else "$overrides manual overrides",
                    )
                }
                val cleanup = links.count { it.change == FilesChangeKind.Cleanup }
                if (cleanup > 0) {
                    add(if (cleanup == 1) "1 to delete" else "$cleanup to delete")
                }
            }
            val body = parts.joinToString(" · ").ifEmpty { "Nothing to fetch" }
            "$body → file system"
        }
        FilesPreviewMode.Sync -> {
            val parts = buildList {
                val added = links.count { it.change == FilesChangeKind.Added }
                if (added > 0) add(if (added == 1) "1 to add" else "$added to add")
                val updated = links.count { it.change == FilesChangeKind.Updated }
                if (updated > 0) {
                    add(if (updated == 1) "1 to update" else "$updated to update")
                }
                val removed = links.count { it.change == FilesChangeKind.Removed }
                if (removed > 0) add(if (removed == 1) "1 to remove" else "$removed to remove")
            }
            val body = parts.joinToString(" · ").ifEmpty { "Nothing to sync" }
            "$body → Pakku"
        }
        FilesPreviewMode.None -> {
            val missing = links.count { it.state == DiskState.Missing }
            val untracked = links.count { it.state == DiskState.Untracked }
            if (missing == 0 && untracked == 0) {
                "In sync"
            } else {
                buildList {
                    if (missing > 0) add(if (missing == 1) "1 missing" else "$missing missing")
                    if (untracked > 0) {
                        add(if (untracked == 1) "1 untracked" else "$untracked untracked")
                    }
                }.joinToString(" · ")
            }
        }
    }

@Composable
fun FilesSideLabel(
    title: String,
    subtitle: String,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = JewelTheme.contentColor.copy(alpha = if (emphasized) 1f else 0.55f)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text(" · $subtitle", fontSize = 13.sp, color = color.copy(alpha = color.alpha * 0.85f))
    }
}
