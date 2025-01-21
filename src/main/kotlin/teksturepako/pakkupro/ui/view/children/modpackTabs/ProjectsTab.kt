package teksturepako.pakkupro.ui.view.children.modpackTabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.SplitLayoutState
import org.jetbrains.jewel.ui.component.rememberSplitLayoutState
import teksturepako.pakkupro.ui.component.HorizontalBar
import teksturepako.pakkupro.ui.component.modpack.ProjectDisplay
import teksturepako.pakkupro.ui.component.modpack.ProjectFilter
import teksturepako.pakkupro.ui.component.modpack.ProjectsList

@Composable
fun ProjectsTab()
{
    val outerSplitState: SplitLayoutState = rememberSplitLayoutState(0.2F)

    Column(Modifier.fillMaxSize()) {
        HorizontalSplitLayout(
            state = outerSplitState,
            first = {
                Column {
                    Row {
                        ProjectDisplay()
                    }
                }
            },
            second = {
                Column {
                    Row {
                        HorizontalBar {
                            ProjectFilter()
                        }
                    }
                    Row {
                        ProjectsList(rememberCoroutineScope())
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            firstPaneMinWidth = 200.dp,
            secondPaneMinWidth = 200.dp,
            draggableWidth = 16.dp
        )
    }
}
