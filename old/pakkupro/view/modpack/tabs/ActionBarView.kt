package pakkupro.view.modpack.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

@Composable
fun ActionBarView(title: String, content: @Composable () -> Unit)
{
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                Modifier.padding(start = 14.dp),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )

            content()
        }

        Spacer(Modifier.background(JewelTheme.globalColors.borders.disabled).height(1.dp).fillMaxWidth())
    }
}