package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text

@Composable
fun LineNumber(number: Int?)
{
    Box(modifier = Modifier.width(50.dp)) {
        Text(
            text = number?.toString()?.padStart(4) ?: "    ",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = Color(0xFF808080)
        )
    }
}