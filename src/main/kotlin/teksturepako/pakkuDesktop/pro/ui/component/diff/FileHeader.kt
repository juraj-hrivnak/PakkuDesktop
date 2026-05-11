/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent

@Composable
fun FileHeader(diffContent: DiffContent)
{
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Text(
            text = diffContent.newPath,
            style = JewelTheme.defaultTextStyle.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            ),
            color = diffFileTitleColor(),
        )

        if (diffContent.oldPath != null && diffContent.oldPath != diffContent.newPath)
        {
            Text(
                text = "(was: ${diffContent.oldPath})",
                style = JewelTheme.defaultTextStyle.copy(fontFamily = FontFamily.Monospace),
                color = diffFileSubtitleColor(),
            )
        }
    }
}