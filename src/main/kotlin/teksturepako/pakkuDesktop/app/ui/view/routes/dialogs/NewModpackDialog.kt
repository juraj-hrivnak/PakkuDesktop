/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog

@Composable
fun NewModpackDialog(profileData: ProfileData, onDismiss: () -> Unit) {
    PkUiDialog(
        visible = true,
        onDismiss = onDismiss,
        title = "Modpack '${profileData.currentProfile?.name}' is not initialized.",
    ) {
        Text(text = "Do you want to create a new modpack?", modifier = Modifier.padding(vertical = 4.dp))

        FlowRow(verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
                onClick = { /* TODO: create new modpack */ },
                modifier = Modifier.padding(vertical = 4.dp)
            ) { Text("Yes") }
            DefaultButton(
                onClick = onDismiss,
                modifier = Modifier.padding(vertical = 4.dp)
            ) { Text("No") }
        }
    }
}