package pakkupro.view.components.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import pakkupro.view.components.ContentBox
import pakkupro.viewmodel.MainViewModel
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Redistributable(
    project: Project,
    editable: Boolean,
    textModifier: Modifier = Modifier,
)
{
    var propertyState by remember { mutableStateOf(project.redistributable) }

    FlowRow(
        Modifier.padding(vertical = 6.dp)
    ) {
        ContentBox(Modifier.padding(2.dp)) {
            Text("Redistributable:", textModifier)
        }

        ContentBox(
            if (editable)
            {
                Modifier
                    .padding(2.dp)
                    .clickable {
                        propertyState = !propertyState
                    }
            }
            else Modifier.padding(2.dp)
        ) {
            Text(propertyState.toString(), textModifier)
        }
    }

    LaunchedEffect(project, propertyState)
    {
        project.slug.values.firstOrNull()?.let { slug ->
            MainViewModel.configFile?.projects
                ?.getOrPut(slug) { ConfigFile.ProjectConfig() }
                    ?.apply { redistributable = propertyState }
        }

        MainViewModel.configFile?.write()
    }
}