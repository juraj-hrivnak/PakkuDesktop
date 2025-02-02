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
fun ModpackTab()
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5F)
                .padding(PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowColumn(
                verticalArrangement = Arrangement.Center,
            ) {
                GradientHeader(modpackUiState.configFile?.getName() ?: "Modpack")
                modpackUiState.configFile?.getVersion()?.let { Text(it) }
                modpackUiState.configFile?.getAuthor()?.let { Text(it) }
                modpackUiState.configFile?.getDescription()?.let { Text(it) }
            }
        }

        val scrollState = rememberScrollState()

        Row(
            Modifier
                .padding(PakkuDesktopConstants.commonPaddingSize)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Header("Recent Modpacks")
        }

        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(PakkuDesktopConstants.commonPaddingSize)
                .verticalScroll(scrollState),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 60.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
            }
        }

        VerticalScrollbar(scrollState = scrollState)
    }
}
