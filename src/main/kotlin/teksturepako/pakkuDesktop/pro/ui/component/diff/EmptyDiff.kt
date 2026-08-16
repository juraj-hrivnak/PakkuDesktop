/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.HorizontalBar

@Composable
fun EmptyDiff(
    modifier: Modifier = Modifier,
) {
    SelectionContainer {
        Box(modifier = modifier.fillMaxSize()) {
            HorizontalBar {
                Text("Diff", Modifier.padding(PakkuDesktopConstants.commonPaddingSize / 4))
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Select a file to preview the diff.",
                    modifier = Modifier.padding(PakkuDesktopConstants.commonPaddingSize),
                    style = JewelTheme.defaultTextStyle,
                    color = JewelTheme.contentColor.copy(alpha = 0.55f),
                )
            }
        }
    }
}
