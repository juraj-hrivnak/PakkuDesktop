/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PakkuDesktopConstants
{
    val headerSize: TextUnit = 24.sp
    val commonPaddingSize = 16.dp
    val highlightColor = Color(27, 204, 234)

    /** Amber — warning / provider mismatch / pending update */
    val amber = Color(0xFFF5A623)

    /** Green — success / applied update */
    val green = Color(0xFF3DDC97)

    /** Coral — danger (e.g. Remove) */
    val coral = Color(0xFFEF6461)

    /** Alpha badge yellow */
    val alphaYellow = Color(0xFFF4D35E)
}
