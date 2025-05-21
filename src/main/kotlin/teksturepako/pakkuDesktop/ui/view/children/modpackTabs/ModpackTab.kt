package teksturepako.pakkuDesktop.ui.view.children.modpackTabs

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.ui.component.*
import teksturepako.pakkuDesktop.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.ui.viewmodel.ModpackViewModel

@Composable
fun ModpackTab()
{
    ToastExample()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanelExample()
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    HoverablePanel(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .padding(PakkuDesktopConstants.commonPaddingSize),
                contentAlignment = Alignment.Center
            ) {
                FlowColumn(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GradientHeader(
                        modpackUiState.configFile?.get()?.getName() ?: "Modpack",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    var isChecked by remember { mutableStateOf(false) }

                    Switch(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it }
                    )

                    modpackUiState.configFile?.get()?.getVersion()?.let { version ->
                        Text(
                            text = version,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    modpackUiState.configFile?.get()?.getAuthor()?.let { author ->
                        Text(
                            text = author,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    modpackUiState.configFile?.get()?.getDescription()?.let { description ->
                        Text(
                            text = description,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToastExample() {
    // State for managing toasts
    val toasts = remember { mutableStateOf(listOf<ToastData>()) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Content with buttons to trigger different toasts
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Basic notification
            HoverablePanel(
                onClick = {
                    toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text("Hello, juraj-hrivnak!")
                        }
                    }
                }
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Show Welcome Toast"
                )
            }

            // Custom content toast
            HoverablePanel(
                onClick = {
                    toasts.showToast {
                        Box(
                            modifier = Modifier.padding(16.dp).width(300.dp)
                        ) {
                            Column {
                                Text("Custom Notification")
                                Spacer(Modifier.height(8.dp))
                                Text("This is a multi-line toast with custom content!")
                                Spacer(Modifier.height(8.dp))
                                Switch(checked = true, onCheckedChange = { })
                            }
                        }
                    }
                }
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Show Custom Toast"
                )
            }
        }

        // Toast host that displays all notifications
        SonnerToastHost(
            toasts,
            alignment = Alignment.TopCenter,
            spacing = 8.dp
        )
    }
}
