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

    /** Title-bar Pakku glyph — rail width is built around this slot. */
    val brandIconSize = 28.dp

    /** Horizontal inset around the brand icon in the title bar. */
    val brandIconPadding = 8.dp

    /** Left navigation rail — matches title-bar brand icon slot (pad + icon + pad). */
    val railWidth = brandIconPadding * 2 + brandIconSize

    /** Amber — warning / provider mismatch / pending update */
    val amber = Color(0xFFF5A623)

    /** Green — success / applied update */
    val green = Color(0xFF3DDC97)

    /** Coral — danger (e.g. Remove) */
    val coral = Color(0xFFEF6461)

    /** Alpha badge yellow */
    val alphaYellow = Color(0xFFF4D35E)

    // -- Cards (project cards, Overview islands) --

    val cardCornerRadius = 16.dp
    val cardPadding = 12.dp
    val cardBorderWidth = 1.dp
    val cardBorderColor = Color.Gray.copy(alpha = 0.28f)
    val cardTitleSize: TextUnit = 14.sp
    val cardCaptionSize: TextUnit = 12.sp

    /** Island background: content color at this alpha over the panel. */
    const val islandTintAlpha = 0.03f
    val islandSpacing = 12.dp

    /** Gap between a section title and its fields, and between fields. */
    val cardContentSpacing = 8.dp

    /** Gap between a control and the icon button beside it. */
    val controlSpacing = 4.dp

    /** Map sub-rows (paths, loaders) share this label column so values line up. */
    val settingLabelWidth = 140.dp

    /** Secondary copy: descriptions, placeholders, empty values. */
    const val secondaryContentAlpha = 0.62f

    /** Chip fill behind list entries such as override globs. */
    const val chipBackgroundAlpha = 0.08f

    /** Typing in an inline editor writes to disk after this pause. */
    const val inlineEditDebounceMillis = 400L

    /** JSON diff viewer height in the Overview tab. */
    val jsonDiffHeight = 480.dp

    /** Edit / remove / reset buttons next to a property value. */
    val inlineIconButtonSize = 25.dp
    val multilineInputHeight = 120.dp
}
