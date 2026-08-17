/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application.titlebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants

@Composable
fun AlphaLabel() {
    val yellow = PakkuDesktopConstants.alphaYellow
    val ink = Color(0xFF3A2F00)
    val panel = Color(0xFF141414)
    val shape = RoundedCornerShape(8.dp)
    val density = LocalDensity.current
    val popupOffsetY = with(density) { 30.dp.roundToPx() }

    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(color = yellow, shape = RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = !expanded }
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

        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, popupOffsetY),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 280.dp, max = 340.dp)
                        .shadow(elevation = 8.dp, shape = shape, ambientColor = yellow.copy(alpha = 0.35f))
                        .clip(shape)
                        .background(panel, shape)
                        .border(width = 2.dp, color = yellow, shape = shape),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(yellow)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            key = AllIconsKeys.General.ExclMark,
                            contentDescription = null,
                            tint = ink,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "ALPHA SOFTWARE NOTICE",
                            color = ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    Text(
                        text = "This build of Pakku Desktop is an alpha release. " +
                            "Features may be incomplete, and the application may exhibit " +
                            "unstable or unexpected behaviour. Please use it with due caution.",
                        color = yellow.copy(alpha = 0.95f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}
