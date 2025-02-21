package teksturepako.pakkupro.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkupro.ui.PakkuDesktopIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectCard(
    project: Project,
    modifier: Modifier = Modifier,
    name: @Composable (String) -> Unit = { Text(it) }
) {
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
            // Project name
            project.name.values.firstOrNull()?.let {
                name(it)
            }

            // Project type
            Text(
                text = project.type.name,
                color = Color.Gray
            )
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
                        val provIcon = when (provider.serialName) {
                            "curseforge" -> PakkuDesktopIcons.Platforms.curseForge
                            "github" -> PakkuDesktopIcons.Platforms.gitHub
                            "modrinth" -> PakkuDesktopIcons.Platforms.modrinth
                            else -> null
                        }

                        provIcon?.let {
                            Icon(
                                it,
                                contentDescription = provider.name,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }

                    slug?.let {
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