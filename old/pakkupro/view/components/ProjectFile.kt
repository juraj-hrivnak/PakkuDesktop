package pakkupro.view.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import pakkupro.viewmodel.PakkuDesktopIcons
import teksturepako.pakku.api.projects.ProjectFile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectFileName(projectFile: ProjectFile)
{
    FlowRow(
        Modifier.padding(start = 16.dp),
    ) {
        Column {
            val provIcon = when (projectFile.type)
            {
                "curseforge" -> PakkuDesktopIcons.Platforms.curseForge
                "github"     -> PakkuDesktopIcons.Platforms.gitHub
                "modrinth"   -> PakkuDesktopIcons.Platforms.modrinth
                else         -> null
            }

            provIcon?.let {
                Icon(it, projectFile.type, Modifier.padding(2.dp).size(25.dp))
            }
        }
        Column(Modifier.offset(y = 3.dp)) { Text(": ") }
        SelectionContainer {
            Column(Modifier.offset(y = 3.dp)) { Text(projectFile.fileName) }
        }
        Column {
            CopyButton(
                onClick = {
                    it.setText(AnnotatedString((projectFile.fileName)))
                },
                Modifier
                    .size(25.dp)
                    .padding(4.dp),
                tooltip = "project file name"
            )
        }
    }
}
