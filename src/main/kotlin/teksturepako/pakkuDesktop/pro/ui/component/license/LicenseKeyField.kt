/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.license

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.integration.readClipboardText
import teksturepako.pakkuDesktop.app.ui.component.FadeIn
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.component.text.SelectableText
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LicenseKeyField(
    isProActivated: Boolean?,
    licenseKeyError: ActionError?,
    onSubmitLicenseKey: (String) -> Unit,
) {
    val delay = 1.seconds

    FadeIn(delay = delay) {
        FlowColumn(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center,
        ) {
            when (isProActivated) {
                true -> {
                    GradientHeader("Pakku Pro is activated!")
                }
                false -> {
                    Header("Please enter your license key.")

                    val licenseKeyText = rememberTextFieldState()
                    var pasteHint by remember { mutableStateOf<String?>(null) }

                    TextField(
                        state = licenseKeyText,
                        modifier = Modifier
                            .width(445.dp)
                            .padding(vertical = 16.dp),
                        textStyle = JewelTheme.editorTextStyle,
                        placeholder = { Text("PAKKU-PRO-00000000-0000-0000-0000-000000000000") },
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                val pasted = readClipboardText()?.trim().orEmpty()
                                if (pasted.isNotEmpty()) {
                                    licenseKeyText.setTextAndPlaceCursorAtEnd(pasted)
                                    pasteHint = null
                                } else {
                                    pasteHint =
                                        "Clipboard empty or unreadable. On Wayland, install wl-clipboard, copy the key again, then Paste."
                                }
                            },
                        ) {
                            Text("Paste")
                        }
                        DefaultButton(
                            onClick = {
                                onSubmitLicenseKey(licenseKeyText.text.toString().trim())
                            },
                        ) {
                            Text("Submit")
                        }
                    }

                    pasteHint?.let { hint ->
                        Text(
                            hint,
                            color = JewelTheme.contentColor.copy(alpha = 0.75f),
                            modifier = Modifier.padding(vertical = 4.dp).widthIn(max = 445.dp),
                        )
                    }

                    if (licenseKeyError != null) {
                        SelectableText(
                            licenseKeyError.rawMessage,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
                null -> { }
            }
        }
    }
}
