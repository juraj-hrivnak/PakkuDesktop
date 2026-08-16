/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlphaLabel()
{
    val yellow = PakkuDesktopConstants.alphaYellow
    val ink = Color(0xFF3A2F00)

    Tooltip(
        tooltip = { Text("Pakku Desktop is currently in alpha! Expect unfinished and buggy experience!") },
    ) {
        Row(
            modifier = Modifier
                .background(color = yellow, shape = RoundedCornerShape(20.dp))
                .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("α", color = yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "alpha",
                color = ink,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}
