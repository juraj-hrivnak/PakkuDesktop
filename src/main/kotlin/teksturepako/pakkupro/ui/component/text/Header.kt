package teksturepako.pakkupro.ui.component.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkupro.ui.PakkuDesktopConstants

@Composable
fun Header(headerText: String)
{
    Text(
        headerText,
        fontWeight = FontWeight.Bold,
        fontSize = PakkuDesktopConstants.headerSize,
    )
}
