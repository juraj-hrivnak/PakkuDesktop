/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.menuStyle
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.elm.RecomposeProbe
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown
import teksturepako.pakkuDesktop.pkui.component.PkUiTooltip

@Composable
internal fun MetaMenuLabel(text: String) {
    Text(
        text,
        color = JewelTheme.menuStyle.colors.itemColors.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Pakku dropdown anchored on the chip. The menu is at least as wide as the chip. */
@Composable
internal fun MetaMenuChip(label: String, menu: MenuScope.() -> Unit) {
    RecomposeProbe("ModpackMenuChip")
    PkUiDropdown(menuContent = menu) {
        Text(
            label,
            fontSize = PakkuDesktopConstants.cardCaptionSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun MetaAddRow(placeholder: String, onAdd: (String) -> Unit) {
    RecomposeProbe("ModpackAddRow")
    val input = rememberTextFieldState()

    fun submit() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        onAdd(text)
        input.clearText()
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PakkuDesktopConstants.controlSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            input,
            Modifier
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            submit()
                            true
                        }
                        else -> false
                    }
                },
            placeholder = { Text(placeholder) },
        )
        PkUiTooltip({ Text("Add") }) {
            IconButton(
                onClick = ::submit,
                enabled = input.text.isNotBlank(),
                modifier = Modifier.size(PakkuDesktopConstants.inlineIconButtonSize),
            ) {
                Icon(AllIconsKeys.General.Add, "Add", tint = JewelTheme.contentColor)
            }
        }
    }
}

@Composable
internal fun MetaChipVersionField(version: String, placeholder: String, onCommit: (String) -> Unit) {
    RecomposeProbe("ModpackChipVersionField")
    val state = rememberTextFieldState(version)
    var armed by remember { mutableStateOf(false) }
    fun commit() {
        if (!armed) return
        armed = false
        onCommit(state.text.toString().trim())
    }
    TextField(
        state,
        Modifier
            .width(PakkuDesktopConstants.settingLabelWidth)
            .onFocusChanged { focus ->
                if (focus.isFocused) armed = true else commit()
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        commit()
                        true
                    }
                    Key.Escape -> {
                        armed = false
                        onCommit(version)
                        true
                    }
                    else -> false
                }
            },
        placeholder = { Text(placeholder) },
    )
}
