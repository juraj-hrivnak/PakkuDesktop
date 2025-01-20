package pakkupro.view.modpack

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icon.PathIconKey
import pakkupro.viewmodel.Icons
import teksturepako.pakkupro.ui.viewmodel.ModpackViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeftSideBar()
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    Column(
        Modifier
            .fillMaxHeight()
            .width(40.dp)
            .padding(vertical = 4.dp)
            .background(JewelTheme.globalColors.panelBackground),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Tooltip({ Text("Manage Projects") }) {
            IconButton(
                onClick = { modpackUiState.value = 0 },
                Modifier.size(30.dp),
                enabled = tabSelected.value != 0,
            ) {
                Icon(
                    key = PathIconKey(
                        "icons/generic.svg", Icons::class.java
                    ),
                    contentDescription = "icon",
                    tint = if (tabSelected.value == 0) Color.LightGray else Color.Gray,
                    hints = arrayOf()
                )
            }
        }

        Tooltip({ Text("Add Projects") }) {
            IconButton(
                onClick = { tabSelected.value = 1 },
                Modifier.size(30.dp),
                enabled = tabSelected.value != 1,
            ) {
                Icon(
                    key = PathIconKey(
                        "icons/compass.svg", Icons::class.java
                    ),
                    contentDescription = "icon",
                    tint = if (tabSelected.value == 1) Color.LightGray else Color.Gray,
                    hints = arrayOf()
                )
            }
        }

        Tooltip({ Text("Add Projects") }) {
            IconButton(
                onClick = { tabSelected.value = 2 },
                Modifier.size(30.dp),
                enabled = tabSelected.value != 2,
            ) {
                Icon(
                    key = PathIconKey(
                        "icons/settings.svg", Icons::class.java
                    ),
                    contentDescription = "icon",
                    tint = if (tabSelected.value == 2) Color.LightGray else Color.Gray,
                    hints = arrayOf()
                )
            }
        }
    }

    Spacer(Modifier.background(JewelTheme.globalColors.borders.disabled).width(1.dp).fillMaxHeight())
}