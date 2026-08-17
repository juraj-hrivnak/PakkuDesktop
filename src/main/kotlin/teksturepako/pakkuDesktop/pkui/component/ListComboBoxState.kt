/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.runtime.Composable
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListState
import org.jetbrains.jewel.foundation.lazy.SelectionMode
import org.jetbrains.jewel.foundation.lazy.rememberSelectableLazyListState

/**
 * Jewel [org.jetbrains.jewel.ui.component.ListComboBox] defaults list state to [SelectionMode.Multiple]
 * while the popup uses [SelectionMode.Single], which logs a mismatch warning.
 */
@Composable
fun rememberListComboBoxState(
    items: List<String>,
    selectedIndex: Int,
): SelectableLazyListState {
    val index = selectedIndex.coerceIn(0, (items.lastIndex).coerceAtLeast(0))
    return rememberSelectableLazyListState(
        initialFirstVisibleItemIndex = index,
        selectionMode = SelectionMode.Single,
        initialSelectedKeys = listOfNotNull(items.getOrNull(index)),
    )
}
