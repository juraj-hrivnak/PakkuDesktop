package pakkupro.view.modpack.tabs.projects.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakku.api.projects.Project

@Composable
fun ProjectFilter(searchFilter: MutableState<(Project) -> Boolean>)
{
    var searchState: String by remember { mutableStateOf("") }

    TextField(
        value = searchState,
        onValueChange = {
            searchState = it
            searchFilter.value = { project ->
                project.name.values.any { value -> searchState.lowercase() in value.lowercase() }
            }
        },
        Modifier
            .height(60.dp)
            .width(180.dp)
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
        placeholder = {
            Text("Filter projects")
        },
    )
}
