package pakkupro.view.components.project

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import org.jetbrains.jewel.ui.component.SegmentedControl
import org.jetbrains.jewel.ui.component.SegmentedControlButtonData
import org.jetbrains.jewel.ui.component.Text
import pakkupro.view.components.ContentBox
import pakkupro.viewmodel.MainViewModel
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide
import com.github.michaelbull.result.fold as resultFold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Side(
    project: Project,
    editable: Boolean,
    onSelect: (ProjectSide?) -> Result<ProjectSide?, ActionError?>
)
{
    var propertyState: ProjectSide? by remember { mutableStateOf(null) }

//    LaunchedEffect(project)
//    {
        propertyState = project.side
//    }

    val buttons = ProjectSide.entries.map { projectSide ->
        SegmentedControlButtonData(
            selected = propertyState == projectSide,
            content = { _ -> Text(projectSide.name) },
            onSelect = {
                onSelect(propertyState).resultFold(
                    failure = {

                    },
                    success = {
                        propertyState = it
                    }
                )
            },
        )
    }

    if (editable)
    {
        FlowRow(
            Modifier.padding(vertical = 6.dp)
        ) {
            ContentBox(Modifier.padding(2.dp)) {
                Text("Side:")
            }

            SegmentedControl(buttons, Modifier.padding(2.dp))
        }
    }
    else if (propertyState != null)
    {
        FlowRow(
            Modifier.padding(vertical = 6.dp)
        ) {
            ContentBox(Modifier.padding(2.dp)) {
                Text("Side:")
            }

            ContentBox(
                Modifier
                    .padding(2.dp)
            ) {
                Text(propertyState!!.name)
            }
        }
    }

//    LaunchedEffect(project, propertyState)
//    {
//        project.slug.values.firstOrNull()?.let { slug ->
//            MainViewModel.configFile?.projects
//                ?.getOrPut(slug) { ConfigFile.ProjectConfig() }
//                ?.apply { side = propertyState?.let { ProjectSide.valueOf(it.name) } }
//        }
//
//        MainViewModel.configFile?.write()?.let {
//            println(it.rawMessage)
//        }
//    }
}


