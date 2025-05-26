package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.modifier.clickableHover
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel
import teksturepako.pakkuDesktop.app.ui.viewmodel.state.SortOrder

@Composable
fun ListControls(lastClickedIndex: MutableState<Int?>)
{
    val modpackUiState by ModpackViewModel.modpackUiState.collectAsState()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(40.dp).padding(end = 4.dp)
        ) {
            Checkbox(
                checked = modpackUiState.lockFile?.get()?.getAllProjects()
                    ?.filter(modpackUiState.projectsFilter)
                    ?.all { ModpackViewModel.ProjectsSelection.isSelected(it) } == true,
                onCheckedChange = { checked ->
                    val filteredProjects = modpackUiState.lockFile?.get()
                        ?.getAllProjects()
                        ?.filter(modpackUiState.projectsFilter)
                        ?: return@Checkbox

                    if (checked)
                    {
                        ModpackViewModel.ProjectsSelection.select(filteredProjects)
                    }
                    else
                    {
                        ModpackViewModel.ProjectsSelection.clear()
                    }

                    lastClickedIndex.value = null
                },
                modifier = Modifier.padding(4.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickableHover(scaleOnHover = true) {
                ModpackViewModel.updateSortOrder(
                    when
                    {
                        modpackUiState.sortOrder is SortOrder.Name && modpackUiState.sortOrder.ascending ->
                        {
                            SortOrder.Name(ascending = false)
                        }

                        else                                                                             ->
                        {
                            SortOrder.Name(ascending = true)
                        }
                    }
                )
            }) {
            Text(
                text = "Name", color = JewelTheme.contentColor
            )

            when
            {
                modpackUiState.sortOrder is SortOrder.Name && modpackUiState.sortOrder.ascending ->
                {
                    AllIconsKeys.Gutter.Fold
                }

                modpackUiState.sortOrder is SortOrder.Name                                       ->
                {
                    AllIconsKeys.Gutter.FoldBottom
                }

                else                                                                             -> null
            }?.let {
                Icon(
                    it,
                    contentDescription = "Sort direction",
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickableHover(scaleOnHover = true) {
                ModpackViewModel.updateSortOrder(
                    when
                    {
                        modpackUiState.sortOrder is SortOrder.LastUpdated && !modpackUiState.sortOrder.ascending ->
                        {
                            SortOrder.LastUpdated(ascending = true)
                        }

                        else                                                                                     ->
                        {
                            SortOrder.LastUpdated(ascending = false)
                        }
                    }
                )
            }) {
            Text(
                text = "Last Updated", color = JewelTheme.contentColor
            )

            when
            {
                modpackUiState.sortOrder is SortOrder.LastUpdated && modpackUiState.sortOrder.ascending ->
                {
                    AllIconsKeys.Gutter.Fold
                }

                modpackUiState.sortOrder is SortOrder.LastUpdated                                       ->
                {
                    AllIconsKeys.Gutter.FoldBottom
                }

                else                                                                                    -> null
            }?.let {
                Icon(
                    it,
                    contentDescription = "Sort direction",
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }
    }
}
