/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.elm.animatedColor

@Composable
fun ProjectCard(
    project: Project,
    modifier: Modifier = Modifier,
    name: @Composable (String) -> Unit = { Text(it) },
) {
    val contentColor = animatedColor(JewelTheme.contentColor)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SelectionContainer {
                project.name.values.firstOrNull()?.let { name(it) }
            }
            SelectionContainer {
                Text(text = project.type.name, color = Color.Gray)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            project.getProviders().groupBy { provider -> project.slug[provider.serialName] }
                .forEach { (slug, providers) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        providers.forEach { provider ->
                            when (provider.serialName) {
                                "curseforge" -> Icon(PakkuDesktopIcons.Platforms.curseForge, provider.name, Modifier.size(25.dp))
                                "github"     -> Icon(PakkuDesktopIcons.Platforms.gitHub, provider.name, Modifier.size(25.dp), tint = contentColor)
                                "modrinth"   -> Icon(PakkuDesktopIcons.Platforms.modrinth, provider.name, Modifier.size(25.dp))
                                else         -> Text(provider.name)
                            }
                        }
                        slug?.let { SelectionContainer { Text(text = it, color = Color.Gray) } }
                    }
                }
        }
    }
}