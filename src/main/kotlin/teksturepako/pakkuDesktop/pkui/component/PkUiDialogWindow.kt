/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pkui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import dev.nucleusframework.window.jewel.JewelDecoratedDialog
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.LocalPakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.component.text.Header

/**
 * Separate OS dialog window via Nucleus [JewelDecoratedDialog] (Tao-safe; no AWT DialogWindow).
 */
@Composable
fun PkUiDialogWindow(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    dialogState: DialogState = rememberDialogState(size = DpSize(640.dp, 480.dp)),
    content: @Composable BoxScope.() -> Unit
) {
    val appScope = LocalPakkuApplicationScope.current

    with(appScope.applicationScope) {
        JewelDecoratedDialog(
            visible = visible,
            onCloseRequest = onDismiss,
            state = dialogState,
            title = title.orEmpty(),
        ) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    title?.let {
                        Header(
                            text = title,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            AllIconsKeys.General.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Box(Modifier.padding(top = 32.dp)) {
                    content(this)
                }
            }
        }
    }
}
