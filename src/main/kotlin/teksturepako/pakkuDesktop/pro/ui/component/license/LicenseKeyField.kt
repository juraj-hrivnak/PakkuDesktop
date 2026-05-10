/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component.license

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.ui.component.FadeIn
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.component.text.Header
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

                    TextField(
                        licenseKeyText,
                        Modifier
                            .size(width = 445.dp, height = 62.dp)
                            .padding(vertical = 16.dp),
                        textStyle = JewelTheme.editorTextStyle,
                        placeholder = { Text("PAKKU-PRO-00000000-0000-0000-0000-000000000000") },
                    )

                    DefaultButton(
                        modifier = Modifier.padding(vertical = 4.dp),
                        onClick = {
                            onSubmitLicenseKey(licenseKeyText.text.toString())
                        },
                    ) {
                        Text("Submit")
                    }

                    if (licenseKeyError != null) {
                        SelectionContainer {
                            Text(
                                licenseKeyError.rawMessage,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
                null -> { }
            }
        }
    }
}
