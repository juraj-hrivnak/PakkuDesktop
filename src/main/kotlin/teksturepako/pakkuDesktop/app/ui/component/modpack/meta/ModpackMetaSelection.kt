/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

/**
 * Pack metadata string field. Commits on focus loss or Enter — not on every keystroke.
 */
@Composable
fun ModpackCommitTextField(
    label: String,
    value: String,
    placeholder: String = "—",
    singleLine: Boolean = true,
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    mutate: (ConfigFile, LockFile, String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (label.isNotBlank()) {
            Text(
                label,
                color = JewelTheme.contentColor.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (model.editingModpack) {
            val textFieldState = rememberTextFieldState(value)
            var focused by remember { mutableStateOf(false) }

            LaunchedEffect(value) {
                if (!focused && textFieldState.text.toString() != value) {
                    textFieldState.setTextAndPlaceCursorAtEnd(value)
                }
            }

            fun commit() {
                val text = textFieldState.text.toString()
                if (text == value) return
                publish(
                    ModpackMsg.MetaWriteRequested(
                        MetaWrite { config, lock -> mutate(config, lock, text) },
                    ),
                )
            }

            TextField(
                textFieldState,
                Modifier
                    .fillMaxWidth()
                    .then(if (singleLine) Modifier.height(36.dp) else Modifier.heightIn(min = 72.dp))
                    .onFocusChanged { focus ->
                        val wasFocused = focused
                        focused = focus.isFocused
                        if (wasFocused && !focus.isFocused) commit()
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                if (singleLine) {
                                    commit()
                                    true
                                } else false
                            }
                            else -> false
                        }
                    },
            )
        } else {
            val display = value.ifBlank { placeholder }
            val muted = value.isBlank()
            Text(
                display,
                color = if (muted) {
                    JewelTheme.contentColor.copy(alpha = 0.4f)
                } else {
                    JewelTheme.contentColor
                },
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ModpackChoiceCombo(
    label: String,
    options: List<String>,
    selected: String,
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    mutate: (ConfigFile, LockFile, String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            color = JewelTheme.contentColor.copy(alpha = 0.65f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )

        if (model.editingModpack) {
            val selectedIndex = options.indexOfFirst { it.equals(selected, ignoreCase = true) }
                .coerceAtLeast(0)
            ListComboBox(
                items = options,
                selectedIndex = selectedIndex,
                onSelectedItemChange = { index ->
                    val option = options.getOrNull(index) ?: return@ListComboBox
                    if (option.equals(selected, ignoreCase = true)) return@ListComboBox
                    publish(
                        ModpackMsg.MetaWriteRequested(
                            MetaWrite { config, lock -> mutate(config, lock, option) },
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                selected.ifBlank { "—" },
                color = if (selected.isNotBlank()) {
                    JewelTheme.contentColor
                } else {
                    JewelTheme.contentColor.copy(alpha = 0.4f)
                },
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
fun ModpackLoaderChips(
    selected: String,
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    mutate: (ConfigFile, LockFile, String) -> Unit,
) {
    ModpackChoiceCombo(
        label = "Loader",
        options = ModpackFieldOptions.LOADERS,
        selected = selected,
        publish = publish,
        model = model,
        mutate = mutate,
    )
}

@Composable
fun ModpackTargetChips(
    selected: String,
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    mutate: (ConfigFile, LockFile, String) -> Unit,
) {
    ModpackChoiceCombo(
        label = "Target",
        options = ModpackFieldOptions.TARGETS,
        selected = selected,
        publish = publish,
        model = model,
        mutate = mutate,
    )
}
