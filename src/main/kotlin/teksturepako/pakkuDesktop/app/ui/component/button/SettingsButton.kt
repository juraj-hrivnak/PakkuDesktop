/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.button

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun SettingsButton(
    onClick: () -> Unit
)
{
    IconButton(
        onClick = {
            onClick()
        },
        Modifier.size(30.dp),
    ) {
        Icon(
            key = AllIconsKeys.General.Settings,
            contentDescription = "Settings Icon",
            tint = JewelTheme.contentColor,
            hints = arrayOf(),
        )
    }
}
