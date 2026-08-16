/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.elm.animatedColor

@Composable
fun ProviderIcon(
    provider: Provider,
    modifier: Modifier = Modifier.size(18.dp),
) {
    val contentColor = animatedColor(JewelTheme.contentColor)
    when (provider.serialName) {
        "curseforge" -> Icon(
            PakkuDesktopIcons.Platforms.curseForge,
            contentDescription = provider.name,
            modifier = modifier,
        )
        "modrinth" -> Icon(
            PakkuDesktopIcons.Platforms.modrinth,
            contentDescription = provider.name,
            modifier = modifier,
        )
        "github" -> Icon(
            PakkuDesktopIcons.Platforms.gitHub,
            contentDescription = provider.name,
            modifier = modifier,
            tint = contentColor,
        )
        else -> Text(provider.shortName, color = Color.Gray, fontSize = 11.sp)
    }
}

/**
 * Inline project identity for GUI: type + provider logos grouped by slug
 * (same visual language as [ProjectCard]).
 */
@Composable
fun ProjectRef(
    project: Project,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
    fontSize: TextUnit = 13.sp,
    showType: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showType) {
            Text(
                text = project.type.prettyName,
                color = Color.Gray,
                fontSize = fontSize,
            )
        }
        project.getProviders().groupBy { provider ->
            project.slug[provider.serialName]
        }.forEach { (slug, providers) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                providers.forEach { provider ->
                    ProviderIcon(provider, Modifier.size(iconSize))
                }
                slug?.let {
                    Text(text = it, fontSize = fontSize)
                }
            }
        }
        if (project.getProviders().isEmpty()) {
            val fallback = project.name.values.firstOrNull()
                ?: project.slug.values.firstOrNull()
                ?: project.pakkuId
                ?: "?"
            Text(text = fallback, fontSize = fontSize)
        }
    }
}
