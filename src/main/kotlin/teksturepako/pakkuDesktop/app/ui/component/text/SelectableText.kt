/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Selectable text that does not use Jewel [org.jetbrains.jewel.ui.component.Text],
 * whose context menu crashes on Nucleus (`TextManager.getCut`).
 */
@Composable
fun SelectableText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = JewelTheme.contentColor,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = JewelTheme.defaultTextStyle,
) {
    val resolved = style.merge(
        TextStyle(
            color = color,
            fontSize = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize,
            fontWeight = fontWeight ?: style.fontWeight,
            fontFamily = fontFamily ?: style.fontFamily,
        ),
    )
    SelectionContainer(modifier = modifier) {
        BasicText(
            text = text,
            style = resolved,
            maxLines = maxLines,
            overflow = overflow,
        )
    }
}
