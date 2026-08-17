/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text
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

    val allProjects = model.lockFile?.get()?.getAllProjects() ?: emptyList()
    val pendingUpdateCount = model.updatePreviews?.count { !it.value.applied }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = buildString {
                    append("${allProjects.size} total")
                    if (pendingUpdateCount != null) {
                        append(" · ")
                        append(
                            when (pendingUpdateCount) {
                                0 -> "up to date"
                                1 -> "1 update"
                                else -> "$pendingUpdateCount updates"
                            },
                        )
                    }
                },
                fontSize = 12.sp,
                color = JewelTheme.contentColor.copy(alpha = 0.55f),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            ProjectFilter(
                publish = publish,
                model = model,
                focusRequester = filterFocusRequester,
            )
        }

        Column {
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
