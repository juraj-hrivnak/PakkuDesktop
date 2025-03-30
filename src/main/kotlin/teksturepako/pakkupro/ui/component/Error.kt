package teksturepako.pakkupro.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.TextArea
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkupro.ui.PakkuDesktopConstants
import teksturepako.pakkupro.ui.component.button.CopyToClipboardButton
import teksturepako.pakkupro.ui.component.text.Header

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Error(error: ActionError)
{
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.padding(PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    error::class.simpleName?.let {
                        Header(
                            text = "Error of type '$it' occurred.",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    CopyToClipboardButton(error.rawMessage, Modifier.size(35.dp), useSimpleTooltip = true)
                }
                TextArea(
                    TextFieldState(error.rawMessage),
                    readOnly = true,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textStyle = JewelTheme.consoleTextStyle
                )
            }
        }
    }
}
