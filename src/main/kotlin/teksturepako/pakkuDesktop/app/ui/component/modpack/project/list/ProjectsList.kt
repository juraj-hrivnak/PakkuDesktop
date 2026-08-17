/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectFilter
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun ProjectsList(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    filterFocusRequester: FocusRequester,
) {
    val lastClickedIndex = remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize()) {
        // Toolbar: search / filters / updates + select-all / sort
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PakkuDesktopConstants.commonPaddingSize)
                .padding(top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProjectFilter(
                publish = publish,
                model = model,
                focusRequester = filterFocusRequester,
            )
            ListControls(publish, model, lastClickedIndex)
        }

        Divider(Orientation.Horizontal)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            ListImpl(publish, model, lastClickedIndex)
            ListFloatingActions(publish, model)
        }

        Divider(Orientation.Horizontal)

        ListActions(publish, model)
    }
}
