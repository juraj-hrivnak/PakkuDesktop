/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

/** Error toast with GUI [ActionErrorContent] (project logos, mapped messages). */
internal fun actionErrorToast(error: ActionError) = ToastData(content = {
    Box(Modifier.padding(16.dp).width(320.dp)) {
        ActionErrorContent(error)
    }
})

internal fun actionErrorToast(message: String) = ToastData(content = {
    Box(Modifier.padding(16.dp).width(300.dp)) {
        Text(message)
    }
})

internal fun actionErrorToast(profileLabel: String, error: ActionError) = ToastData(content = {
    Box(Modifier.padding(16.dp).width(320.dp)) {
        Column {
            Text(profileLabel, fontWeight = FontWeight.Bold)
            ActionErrorContent(error)
        }
    }
})

internal fun actionInfoToast(message: String) = ToastData(content = {
    Box(Modifier.padding(16.dp).width(300.dp)) {
        Text(message)
    }
})
