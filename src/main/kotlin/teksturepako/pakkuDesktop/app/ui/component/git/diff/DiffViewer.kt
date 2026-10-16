/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git.diff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import teksturepako.pakkuDesktop.app.git.DiffContent

/**
 * Read-only unified diff. Soft wrap is on until turned off; unwrapped lines scroll horizontally.
 */
@Composable
fun DiffViewer(
    currentDiff: DiffContent?,
    modifier: Modifier = Modifier,
) {
    var softWrap by remember { mutableStateOf(true) }
    if (currentDiff == null) {
        EmptyDiff(modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        FileHeader(
            diffContent = currentDiff,
            softWrap = softWrap,
            onSoftWrapChange = { softWrap = it },
        )
        DiffTextArea(
            diff = currentDiff,
            softWrap = softWrap,
            modifier = Modifier.weight(1f),
        )
    }
}
