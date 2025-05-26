package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.ui.component.Text

@Composable
fun EmptyDiff()
{
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select a file to view its changes",
        )
    }
}