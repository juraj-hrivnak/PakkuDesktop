/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants

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
