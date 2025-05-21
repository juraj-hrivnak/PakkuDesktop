package teksturepako.pakkuDesktop.ui.component.modpack.project

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
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakkuDesktop.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.ui.component.button.CopyToClipboardButton
import teksturepako.pakkuDesktop.ui.viewmodel.ProfileViewModel

@Composable
fun ProjectFileCard(projectFile: ProjectFile)
{
    val profileData by ProfileViewModel.profileData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                when (projectFile.type)
                {
                    "curseforge" ->
                    {
                        Icon(
                            PakkuDesktopIcons.Platforms.curseForge, projectFile.type, modifier = Modifier.size(25.dp)
                        )
                    }
                    "github"     ->
                    {
                        Icon(
                            PakkuDesktopIcons.Platforms.gitHub,
                            projectFile.type,
                            modifier = Modifier.size(25.dp),
                            tint = if (profileData.intUiTheme.isDark()) Color.White else Color.Black
                        )
                    }
                    "modrinth"   ->
                    {
                        Icon(
                            PakkuDesktopIcons.Platforms.modrinth, projectFile.type, modifier = Modifier.size(25.dp)
                        )
                    }

                    else         ->
                    {
                        Text(projectFile.type)
                    }
                }

                SelectionContainer {
                    Text(projectFile.fileName)
                }
            }

            CopyToClipboardButton(
                text = projectFile.fileName, modifier = Modifier.size(25.dp)
            )
        }
    }
}