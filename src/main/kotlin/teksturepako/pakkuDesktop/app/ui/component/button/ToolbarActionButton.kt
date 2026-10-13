/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.button

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.ToggleableIconActionButton
import org.jetbrains.jewel.ui.icon.IconKey

/** New UI toolbar action: icon-only, hover background, tooltip carries the label. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolbarActionButton(
    key: IconKey,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconActionButton(
        key = key,
        contentDescription = label,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        focusable = false,
    ) {
        Text(label)
    }
}

/** [ToolbarActionButton] that stays pressed while [value] is true. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolbarToggleButton(
    key: IconKey,
    label: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ToggleableIconActionButton(
        key = key,
        contentDescription = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        focusable = false,
    ) {
        Text(label)
    }
}

/** Vertical separator between toolbar action groups. */
@Composable
fun ToolbarSeparator() {
    Divider(
        orientation = Orientation.Vertical,
        modifier = Modifier.height(16.dp).padding(horizontal = 4.dp).width(1.dp),
        color = JewelTheme.globalColors.borders.normal,
    )
}
