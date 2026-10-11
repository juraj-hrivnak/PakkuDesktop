/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.official.plugin.pro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import kotlin.time.Duration.Companion.seconds

private val headerSize = 24.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LicenseKeyField(
    isProActivated: Boolean?,
    busy: Boolean,
    licenseKeyError: String?,
    onSubmitLicenseKey: (String) -> Unit,
) {
    FadeIn {
        FlowColumn(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center,
        ) {
            when (isProActivated) {
                true -> {
                    GradientHeader("Pakku Pro is activated!")
                }
                false -> {
                    Text("Please enter your license key.", fontWeight = FontWeight.Bold, fontSize = headerSize)

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
                            enabled = !busy,
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
                        // Not Jewel Text: its context menu crashes on Nucleus (`TextManager.getCut`).
                        SelectionContainer(Modifier.padding(vertical = 4.dp)) {
                            BasicText(
                                licenseKeyError,
                                style = JewelTheme.defaultTextStyle.merge(TextStyle(color = JewelTheme.contentColor)),
                            )
                        }
                    }
                }
                null -> { }
            }
        }
    }
}

@Composable
private fun GradientHeader(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = headerSize,
        style = TextStyle(brush = Brush.linearGradient(listOf(Color(27, 204, 234), Color(81, 203, 255)))),
    )
}

@Composable
private fun FadeIn(content: @Composable () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(2.seconds)
        isVisible = true
    }
    AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(2_000))) {
        content()
    }
}
