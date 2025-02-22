package teksturepako.pakkupro.ui.component.modpack.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakkupro.ui.PakkuDesktopIcons
import teksturepako.pakkupro.ui.component.ProjectCard
import teksturepako.pakkupro.ui.component.button.CopyToClipboardButton
import teksturepako.pakkupro.ui.component.text.GradientHeader
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel

@Composable
fun ProjectDisplay() {
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()
    val project = modpackUiState.selectedProject ?: return
    val scrollState = rememberScrollState()

    LaunchedEffect(project.pakkuId) {
        ModpackViewModel.editProject(false)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProjectCard(project) {
                GradientHeader(it)
            }

            // Project Files Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Project Files")

                project.files.forEach { projectFile ->
                    ProjectFileName(projectFile)
                }
            }

            Spacer(
                modifier = Modifier
                    .background(JewelTheme.globalColors.borders.normal)
                    .height(1.dp)
                    .fillMaxWidth()
            )

            ProjectProperties()
        }

        VerticalScrollbar(
            scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}

@Composable
fun ProjectFileName(projectFile: ProjectFile) {
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform Icon
                val provIcon = when (projectFile.type) {
                    "curseforge" -> PakkuDesktopIcons.Platforms.curseForge
                    "github" -> PakkuDesktopIcons.Platforms.gitHub
                    "modrinth" -> PakkuDesktopIcons.Platforms.modrinth
                    else -> null
                }

                provIcon?.let {
                    Icon(
                        it,
                        contentDescription = projectFile.type,
                        modifier = Modifier.size(25.dp)
                    )
                } ?: Text(projectFile.type)

                SelectionContainer {
                    Text(projectFile.fileName)
                }
            }

            CopyToClipboardButton(
                text = projectFile.fileName,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}