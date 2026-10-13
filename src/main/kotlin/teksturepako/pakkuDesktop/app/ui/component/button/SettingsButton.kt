/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.button

import androidx.compose.runtime.Composable
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun SettingsButton(
    onClick: () -> Unit
)
{
    ToolbarActionButton(
        key = AllIconsKeys.General.Settings,
        label = "Settings",
        onClick = onClick,
    )
}
