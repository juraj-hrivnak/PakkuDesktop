package teksturepako.pakkuDesktop.ui.component.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.ui.PakkuDesktopConstants

@Composable
fun Header(text: String, modifier: Modifier = Modifier)
{
    Text(
        text,
        modifier,
        fontWeight = FontWeight.Bold,
        fontSize = PakkuDesktopConstants.headerSize,
    )
}
