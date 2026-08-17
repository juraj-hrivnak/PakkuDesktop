/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

/** Thin status strip: totals / updates on the left, selection on the right. */
@Composable
fun ListActions(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    val selectedCount = model.selectedProjectKeys.size
    val allProjects = model.lockFile?.get()?.getAllProjects() ?: emptyList()
    val totalCount = allProjects.size
    val pendingUpdateCount = model.updatePreviews?.count { !it.value.applied }

    val totalsLabel = buildString {
        append("$totalCount total")
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
    }

    val selectionLabel = when {
        selectedCount == 0 -> "0 of $totalCount selected"
        selectedCount == totalCount && totalCount > 0 -> "All $selectedCount of $totalCount selected"
        else -> "$selectedCount of $totalCount selected"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PakkuDesktopConstants.commonPaddingSize, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = totalsLabel,
            color = JewelTheme.contentColor.copy(alpha = 0.55f),
            fontSize = 12.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = selectionLabel,
            color = JewelTheme.contentColor.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
    }
}
