package teksturepako.pakkupro.ui.component.modpack.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakkupro.ui.component.text.GradientHeader
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel

@Composable
fun ProjectDisplay()
{
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
                    ProjectFileCard(projectFile)
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
