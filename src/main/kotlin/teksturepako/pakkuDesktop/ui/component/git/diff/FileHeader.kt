package teksturepako.pakkuDesktop.ui.component.git.diff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.ui.viewmodel.state.DiffContent

@Composable
fun FileHeader(diffContent: DiffContent)
{
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = diffContent.newPath,
            color = Color(0xFF4A7A94),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )

        if (diffContent.oldPath != null && diffContent.oldPath != diffContent.newPath)
        {
            Text(
                text = "(was: ${diffContent.oldPath})",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}