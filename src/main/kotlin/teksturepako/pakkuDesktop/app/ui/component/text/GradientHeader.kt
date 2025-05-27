/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants

@Composable
fun GradientHeader(headerText: String, modifier: Modifier = Modifier)
{
    val gradientColors = listOf(Color(27, 204, 234), Color(81, 203, 255))

    Text(
        headerText,
        fontWeight = FontWeight.Bold,
        fontSize = PakkuDesktopConstants.headerSize,
        style = TextStyle(
            brush = Brush.linearGradient(
                colors = gradientColors
            )
        ),
        modifier = modifier
    )
}
