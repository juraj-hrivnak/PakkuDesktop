package teksturepako.pakkuDesktop.ui.component.license

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkuDesktop.ui.component.FadeIn
import teksturepako.pakkuDesktop.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.ui.component.text.Header
import teksturepako.pakkuDesktop.ui.viewmodel.LicenseKeyViewModel
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LicenseKeyField(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
)
{
    val delay = 1.seconds

    FadeIn(delay = delay) {
        FlowColumn(
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center,
        ) {
            when (LicenseKeyViewModel.isActivated)
            {
                true  ->
                {
                    GradientHeader("Pakku Pro is activated!")
                }
                false ->
                {
                    Header("Please enter your license key.")

                    val licenseKeyText = rememberTextFieldState()

                    TextField(
                        licenseKeyText,
                        Modifier
                            .size(width = 445.dp, height = 62.dp)
                            .padding(vertical = 16.dp),
                        textStyle = JewelTheme.editorTextStyle,
                        placeholder = { Text("PAKKU-PRO-00000000-0000-0000-0000-000000000000") }
                    )

                    DefaultButton(
                        modifier = Modifier.padding(vertical = 4.dp),
                        onClick = {
                            LicenseKeyViewModel.process(licenseKeyText.text.toString(), coroutineScope)
                        }
                    ) {
                        Text("Submit")
                    }

                    if (LicenseKeyViewModel.error != null)
                    {
                        SelectionContainer {
                            Text(
                                LicenseKeyViewModel.error!!.rawMessage,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
                null  -> { }
            }
        }
    }
}