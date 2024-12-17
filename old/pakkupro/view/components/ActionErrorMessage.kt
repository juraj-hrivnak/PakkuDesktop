package pakkupro.view.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icon.PathIconKey
import pakkupro.viewmodel.Icons

@Composable
@Preview
fun ActionErrorMessage(message: String?, modifier: Modifier = Modifier)
{
    SelectionContainer {
        Row(
            modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TooltipBox {
                Row {
                    Column(Modifier.fillMaxWidth(0.05f)) {
                        Icon(
                            key = PathIconKey(
                                "icons/exclamation-triangle.svg", Icons::class.java
                            ),
                            contentDescription = "exclamation-triangle",
                            tint = Color.White,
                            modifier = Modifier.size(25.dp),
                            hints = arrayOf()
                        )
                    }
                    Column {
                        Text(
                            "An error occurred:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Text(
                            message ?: "Unknown error",
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}
