package teksturepako.pakkupro.ui.view.children.modpackTabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakkupro.ui.PakkuDesktopConstants
import teksturepako.pakkupro.ui.component.text.GradientHeader
import teksturepako.pakkupro.ui.component.text.Header
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModpackTab() {
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    Box(
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
                        modpackUiState.configFile?.getName() ?: "Modpack",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    modpackUiState.configFile?.getVersion()?.let { version ->
                        Text(
                            text = version,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    modpackUiState.configFile?.getAuthor()?.let { author ->
                        Text(
                            text = author,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    modpackUiState.configFile?.getDescription()?.let { description ->
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