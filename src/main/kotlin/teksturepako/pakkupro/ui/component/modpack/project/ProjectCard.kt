package teksturepako.pakkupro.ui.component.modpack.project

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkupro.ui.PakkuDesktopIcons
import teksturepako.pakkupro.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectCard(
    project: Project,
    modifier: Modifier = Modifier,
    name: @Composable (String) -> Unit = { Text(it) }
) {
    val profileData by ProfileViewModel.profileData.collectAsState()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SelectionContainer {
                // Project name
                project.name.values.firstOrNull()?.let {
                    name(it)
                }
            }

            SelectionContainer {
                // Project type
                Text(
                    text = project.type.name,
                    color = Color.Gray
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Group providers by unique slugs
            project.getProviders().groupBy { provider ->
                project.slug[provider.serialName]
            }.forEach { (slug, providers) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    providers.forEach { provider ->
                        when (provider.serialName)
                        {
                            "curseforge" ->
                            {
                                Icon(
                                    PakkuDesktopIcons.Platforms.curseForge,
                                    contentDescription = provider.name,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            "github" ->
                            {
                                Icon(
                                    PakkuDesktopIcons.Platforms.gitHub,
                                    contentDescription = provider.name,
                                    modifier = Modifier.size(25.dp),
                                    tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                                )
                            }
                            "modrinth" ->
                            {
                                Icon(
                                    PakkuDesktopIcons.Platforms.modrinth,
                                    contentDescription = provider.name,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            else ->
                            {
                                Text(provider.name)
                            }
                        }
                    }

                    slug?.let {
                        SelectionContainer {
                            Text(
                                text = it,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}