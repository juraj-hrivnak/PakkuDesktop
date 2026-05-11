/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.diff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import teksturepako.pakkuDesktop.pro.ui.viewmodel.state.DiffContent

/**
 * Read-only unified diff text view. Uses TextArea for native scrolling/selection behavior.
 */
@Composable
fun DiffViewer(
    currentDiff: DiffContent?,
    modifier: Modifier = Modifier,
) {
    if (currentDiff == null) {
        EmptyDiff(modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        FileHeader(currentDiff)
        DiffTextArea(
            diff = currentDiff,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
