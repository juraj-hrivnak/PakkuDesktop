/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Badge
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.badgeStyle
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.text.SelectableText
import teksturepako.pakkuDesktop.app.ui.model.ProjectUpdateInfo

@Composable
fun ProjectCard(
    project: Project,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    updateInfo: ProjectUpdateInfo? = null,
    name: @Composable (String) -> Unit = {
        Text(it, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    },
) {
    val shape = RoundedCornerShape(16.dp)
    val applied = updateInfo?.applied == true
    val hasPendingUpdate = updateInfo != null && !applied
    val borderColor = when {
        selected -> PakkuDesktopConstants.highlightColor
        applied -> PakkuDesktopConstants.green.copy(alpha = 0.75f)
        hasPendingUpdate -> PakkuDesktopConstants.amber.copy(alpha = 0.7f)
        else -> Color.Gray.copy(alpha = 0.3f)
    }
    val latest = project.getLatestFile(project.getProviders().ifEmpty { Provider.providers })
    val published = latest?.datePublished
        ?.takeUnless { it == Instant.DISTANT_PAST }
        ?.formatPublished()

    Column(
        modifier = modifier
            .clip(shape)
            .then(
                when {
                    selected -> Modifier.background(PakkuDesktopConstants.highlightColor.copy(alpha = 0.10f))
                    applied -> Modifier.background(PakkuDesktopConstants.green.copy(alpha = 0.08f))
                    hasPendingUpdate -> Modifier.background(PakkuDesktopConstants.amber.copy(alpha = 0.06f))
                    else -> Modifier
                },
            )
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                project.name.values.firstOrNull()?.let { name(it) }
                when {
                    applied -> Badge(style = JewelTheme.badgeStyle.green) {
                        Text("Updated")
                    }
                    hasPendingUpdate -> Badge(style = JewelTheme.badgeStyle.graySecondary) {
                        Text("Update")
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = project.type.prettyName,
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
                published?.let {
                    Text(
                        text = it,
                        color = Color.Gray.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            project.getProviders().groupBy { provider ->
                project.slug[provider.serialName]
            }.forEach { (slug, providers) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    providers.forEach { provider ->
                        ProviderIcon(provider, Modifier.size(22.dp))
                    }

                    slug?.let {
                        SelectableText(
                            text = it,
                            color = Color.Gray,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        if (project.hasProviderVersionMismatch()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    key = AllIconsKeys.General.ExclMark,
                    contentDescription = "Warning",
                    tint = PakkuDesktopConstants.amber,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Versions differ across providers",
                    color = PakkuDesktopConstants.amber,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
