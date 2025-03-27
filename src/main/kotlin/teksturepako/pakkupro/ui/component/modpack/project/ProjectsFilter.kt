package teksturepako.pakkupro.ui.component.modpack.project

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel

@Composable
fun ProjectFilter()
{
    val textFieldState = rememberTextFieldState()

    TextField(
        textFieldState,
        Modifier
            .height(60.dp)
            .width(180.dp)
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
        placeholder = {
            Text("Filter projects")
        }
    )

    LaunchedEffect(textFieldState)
    {
        ModpackViewModel.updateProjectsFilter { project ->
            project.name.values.any { value -> textFieldState.text.toString().lowercase() in value.lowercase() }
                    || textFieldState.text.toString() in project
        }
    }
}
