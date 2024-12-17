package pakkupro.view.modpack.tabs.projects.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.Result
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import pakkupro.view.components.ProjectFileName
import pakkupro.view.components.ProjectName
import pakkupro.view.components.ProjectProperties
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide

@Composable
fun ProjectsTabSidebar(
    selectedProject: MutableState<Project?>,
    onSideSelect: (ProjectSide?) -> Result<ProjectSide?, ActionError?>
)
{
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.3f)
            .padding(vertical = 4.dp)
            .background(JewelTheme.globalColors.panelBackground)
            .verticalScroll(scrollState)
    ) {
        if (selectedProject.value != null)
        {
            val project = selectedProject.value!!

            // -- NAME --

            ProjectName(project)

            // -- FILE NAMES --

            project.files.forEach { projectFile ->
                ProjectFileName(projectFile)
            }

            Spacer(
                Modifier.padding(top = 16.dp).background(JewelTheme.globalColors.borders.disabled).height(1.dp)
                    .fillMaxWidth()
            )

            // -- PROPERTIES --

            var editable by remember { mutableStateOf(false) }

            // Disable when selecting other projects
            LaunchedEffect(project)
            {
                editable = false
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Properties",
                        Modifier
                            .padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    ActionButton(
                        onClick = { editable = !editable}
                    ) {
                        Text("Edit")
                    }
                }
            }

            Row(
                Modifier.padding(horizontal = 16.dp)
            ) {
                ProjectProperties(project, editable = editable, onSideSelect = onSideSelect)
            }
        }
    }

    VerticalScrollbar(
        scrollState = scrollState,
        modifier = Modifier
    )
}
