/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.welcome

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Badge
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.badgeStyle
import teksturepako.pakkuDesktop.app.data.Profile
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.component.HoverablePanel
import teksturepako.pakkuDesktop.app.ui.model.ModpackSummary
import kotlin.time.Duration.Companion.milliseconds

private val CardWidth = 340.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentModpackCard(
    profile: Profile,
    summary: ModpackSummary?,
    onClick: () -> Unit,
) {
    val muted = JewelTheme.contentColor.copy(alpha = 0.6f)

    HoverablePanel(onClick = onClick) {
        Column(
            Modifier.width(CardWidth).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    summary?.name ?: profile.name,
                    Modifier.weight(1f, fill = false),
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                summary?.version?.let { Text(it, color = muted, fontSize = 13.sp, maxLines = 1) }
            }

            summary?.author?.let {
                Text("by $it", color = muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            when (summary?.state) {
                ModpackSummary.State.Ready          -> FlowRow(
                    Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    summary.mcVersions.firstOrNull()?.let { mc ->
                        val more = summary.mcVersions.size - 1
                        Badge(style = JewelTheme.badgeStyle.graySecondary) {
                            Text(if (more > 0) "$mc +$more" else mc)
                        }
                    }
                    summary.loaders.forEach { (loader, version) ->
                        Badge(style = JewelTheme.badgeStyle.graySecondary) {
                            Text(if (version.isNotBlank()) "$loader $version" else loader)
                        }
                    }
                    summary.target?.let {
                        Badge(style = JewelTheme.badgeStyle.graySecondary) { Text(it) }
                    }
                    summary.projectCount?.let {
                        Badge(style = JewelTheme.badgeStyle.graySecondary) {
                            Text(if (it == 1) "1 project" else "$it projects")
                        }
                    }
                }
                ModpackSummary.State.NotInitialized -> WarningLine("Not a Pakku modpack yet")
                ModpackSummary.State.Unreadable     -> WarningLine("Lock file could not be read")
                ModpackSummary.State.Missing        -> WarningLine("Folder not found")
                null                                -> Unit
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    abbreviateHome(profile.path),
                    Modifier.fillMaxWidth(),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                Text(openedAgo(profile), color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun WarningLine(text: String) {
    Row(
        Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            key = PakkuDesktopIcons.exclamationTriangle,
            contentDescription = null,
            tint = PakkuDesktopConstants.amber,
            modifier = Modifier.size(14.dp),
        )
        Text(text, color = PakkuDesktopConstants.amber, fontSize = 12.sp)
    }
}

private fun abbreviateHome(path: String): String {
    val home = System.getProperty("user.home").orEmpty()
    return if (home.isNotEmpty() && path.startsWith(home)) "~" + path.removePrefix(home) else path
}

private fun openedAgo(profile: Profile): String {
    val elapsed = (System.currentTimeMillis() - profile.lastOpened.toEpochMilliseconds()).coerceAtLeast(0).milliseconds
    val days = elapsed.inWholeDays
    return when {
        elapsed.inWholeMinutes < 1 -> "just now"
        elapsed.inWholeHours < 1   -> "${elapsed.inWholeMinutes} min ago"
        days < 1                   -> "${elapsed.inWholeHours} h ago"
        days < 2                   -> "yesterday"
        days < 30                  -> "$days days ago"
        days < 365                 -> (days / 30).let { if (it == 1L) "1 month ago" else "$it months ago" }
        else                       -> (days / 365).let { if (it == 1L) "1 year ago" else "$it years ago" }
    }
}
