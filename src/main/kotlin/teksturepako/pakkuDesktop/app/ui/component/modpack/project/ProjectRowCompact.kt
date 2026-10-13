/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip

@Composable
fun ProjectRowCompact(
    project: Project,
    modifier: Modifier = Modifier,
    ghost: Boolean = false,
    continuation: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val nameAlpha = when {
        ghost -> 0.55f
        continuation -> 0.65f
        else -> 0.92f
    }
    val displayName = project.name.values.firstOrNull()
        ?: project.slug.values.firstOrNull()
        ?: project.id.values.firstOrNull()
        ?: "?"

    PkUiTooltip(
        tooltip = {
            ProjectCard(project = project, modifier = Modifier)
        },
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!continuation) {
                project.getProviders().forEach { provider ->
                    ProviderIcon(provider, Modifier.size(16.dp))
                }
            }
            Text(
                text = displayName,
                color = JewelTheme.contentColor.copy(alpha = nameAlpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (!continuation && project.hasProviderVersionMismatch()) {
                Icon(
                    key = AllIconsKeys.General.ExclMark,
                    contentDescription = "Versions differ across providers",
                    tint = PakkuDesktopConstants.amber,
                    modifier = Modifier.size(14.dp),
                )
            }
            trailing()
        }
    }
}
