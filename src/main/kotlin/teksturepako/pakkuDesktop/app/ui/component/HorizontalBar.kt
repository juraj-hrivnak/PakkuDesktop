package teksturepako.pakkuDesktop.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme

@Composable
fun HorizontalBar(content: @Composable ColumnScope.() -> Unit = { })
{
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content(this@Column)
        }
        Spacer(Modifier.background(JewelTheme.globalColors.borders.normal).height(1.dp).fillMaxWidth())
    }
}
