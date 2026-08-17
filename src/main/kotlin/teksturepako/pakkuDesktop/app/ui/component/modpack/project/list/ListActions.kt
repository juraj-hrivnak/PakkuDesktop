/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListActions(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    val selectedCount = model.selectedPakkuIds.size
    val allProjects = model.lockFile?.get()?.getAllProjects() ?: emptyList()
    val totalCount = allProjects.size
    val busy = model.actionName != null

    val selectionLabel = when
    {
        selectedCount == 0 -> "0 of $totalCount selected"
        selectedCount == totalCount && totalCount > 0 -> "All $selectedCount of $totalCount selected"
        else -> "$selectedCount of $totalCount selected"
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = selectionLabel,
            color = JewelTheme.contentColor.copy(alpha = 0.7f),
            fontSize = 12.5.sp,
        )

        Spacer(Modifier.weight(1f))

        DefaultButton(
            onClick = { publish(ModpackMsg.UpdateRequested(model.selectedPakkuIds)) },
            enabled = selectedCount > 0 && !busy,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Update selected")
                Icon(
                    AllIconsKeys.Actions.CheckOut,
                    "update",
                    tint = JewelTheme.contentColor,
                    hints = arrayOf(),
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
