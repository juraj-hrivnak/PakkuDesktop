import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CoroutineScope
import pakkupro.view.components.ActionErrorMessage
import pakkupro.view.modpack.LeftSideBarView
import pakkupro.view.modpack.tabs.projects.ProjectsTabView
import pakkupro.view.modpack.tabs.tabThree
import pakkupro.view.modpack.tabs.tabTwo
import pakkupro.viewmodel.MainViewModel
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.projects.ProjectSide

@Composable
@Preview
fun ModpackView(
    coroutineScope: CoroutineScope,
    onSideSelect: (ProjectSide?) -> Result<ProjectSide?, ActionError?>
)
{
    val tabSelected = remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize(),
        ) {

            LeftSideBarView(tabSelected)

            // -- TAB CONTENT --

            var errorMessage by mutableStateOf<String?>(null)

            MainViewModel.updatePakkuApi()?.let { errorMessage = it }

            if (errorMessage != null)
            {
                Box(Modifier.background(Color.Transparent)) {
                    ActionErrorMessage(errorMessage, Modifier.fillMaxSize())
                }
                return
            }

            when (tabSelected.value)
            {
                0 -> ProjectsTabView(coroutineScope, onSideSelect = onSideSelect)
                1 -> tabTwo(coroutineScope)
                2 -> tabThree(coroutineScope)
            }
        }

    }


}