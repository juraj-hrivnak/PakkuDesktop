/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git.diff

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants

@Composable
fun EmptyDiff(
    modifier: Modifier = Modifier,
) {
    SelectionContainer {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Diff", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Divider(Orientation.Horizontal)
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
