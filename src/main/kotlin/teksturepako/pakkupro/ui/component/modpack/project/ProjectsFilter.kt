package teksturepako.pakkupro.ui.component.modpack.project

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel

@Composable
fun ProjectFilter()
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    TextField(
        modpackUiState.projectsFilterTextFieldState,
        Modifier
            .height(35.dp)
            .width(300.dp)
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
        placeholder = {
            Text("Filter projects")
        }
    )

    LaunchedEffect(modpackUiState.projectsFilterTextFieldState)
    {
        ModpackViewModel.updateProjectsFilter { project ->
            project.name.values.any { value ->
                modpackUiState.projectsFilterTextFieldState.text.toString().lowercase() in value.lowercase()
            } || modpackUiState.projectsFilterTextFieldState.text.toString() in project
        }
    }
}
