package teksturepako.pakkupro.ui.component.modpack.project.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import teksturepako.pakkupro.ui.component.modpack.project.ProjectFilter

@Composable
fun ProjectsList()
{
    // For shift+click functionality
    val lastClickedIndex = remember { mutableStateOf<Int?>(null) }
    val shiftPressed = remember { mutableStateOf(false) }

    Column {
        Spacer(Modifier.fillMaxWidth().padding(vertical = 4.dp))

        // Filter
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectFilter()
        }

        // Controls
        Column {
            ListControls(lastClickedIndex)
        }

        Spacer(
            Modifier.background(JewelTheme.globalColors.borders.normal).height(1.dp).fillMaxWidth()
        )

        // Main content with scrollbar
        Box(modifier = Modifier.weight(1f)) {
            ListImpl(lastClickedIndex, shiftPressed)
        }

        // Bottom border
        Spacer(Modifier.background(JewelTheme.globalColors.borders.normal).height(1.dp).fillMaxWidth())

        // Actions at bottom
        ListActions()
    }
}