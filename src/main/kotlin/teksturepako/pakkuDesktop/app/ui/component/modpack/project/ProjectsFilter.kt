/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg

/**
 * The filter text field. Uses local [TextFieldState] for UI,
 * publishes [AppMsg.Modpack.FilterTextChanged] on each change.
 *
 * Note: LaunchedEffect(textFieldState.text) publishes a message — this is
 * a pragmatic compromise for text-field-driven filtering.
 */
@Composable
fun ProjectFilter(publish: (AppMsg) -> Unit, model: AppModel) {
    val textFieldState = rememberTextFieldState(model.modpack.projectsFilterText)

    LaunchedEffect(textFieldState.text) {
        publish(AppMsg.Modpack.FilterTextChanged(textFieldState.text.toString()))
    }

    TextField(
        textFieldState,
        Modifier
            .height(35.dp)
            .width(300.dp)
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp),
        placeholder = { Text("Filter projects") }
    )
}
