/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Chip
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.PopupContainer
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

/**
 * Search field + collapsed type/side/provider filters (menu).
 * Active filters appear as compact dismissible chips only when set.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectFilter(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    focusRequester: FocusRequester,
) {
    val textFieldState = rememberTextFieldState(model.projectsFilterText)
    var filtersExpanded by remember { mutableStateOf(false) }

    val activeCount = model.filterTypes.size + model.filterSides.size + model.filterProviders.size

    LaunchedEffect(textFieldState.text) {
        val text = textFieldState.text.toString()
        if (text != model.projectsFilterText) {
            publish(ModpackMsg.FilterTextChanged(text))
        }
    }

    LaunchedEffect(model.wantsFocusProjectsFilter) {
        if (!model.wantsFocusProjectsFilter) return@LaunchedEffect
        focusRequester.requestFocus()
        publish(ModpackMsg.FocusProjectsFilterConsumed)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                textFieldState,
                Modifier
                    .height(35.dp)
                    .widthIn(min = 200.dp)
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { publish(ModpackMsg.ProjectsFilterFocusChanged(it.isFocused)) },
                placeholder = { Text("Filter projects...") },
            )

            Box {
                OutlinedButton(onClick = { filtersExpanded = !filtersExpanded }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            AllIconsKeys.General.Filter,
                            contentDescription = "Filters",
                            modifier = Modifier.size(14.dp),
                        )
                        Text(if (activeCount > 0) "Filters · $activeCount" else "Filters")
                    }
                }

                if (filtersExpanded) {
                    PopupContainer(
                        onDismissRequest = { filtersExpanded = false },
                        horizontalAlignment = Alignment.Start,
                        popupProperties = PopupProperties(
                            focusable = true,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                        ),
                        modifier = Modifier.width(260.dp),
                    ) {
                        FiltersMenuContent(publish = publish, model = model)
                    }
                }
            }

            val busy = model.actionName != null
            val pendingUpdateCount = model.updatePreviews?.count { !it.value.applied }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Checkbox(
                    checked = model.filterUpdatesOnly,
                    onCheckedChange = { checked ->
                        if (model.updatePreviews != null) {
                            publish(ModpackMsg.FilterUpdatesOnlyChanged(checked))
                        }
                    },
                    enabled = !busy && model.updatePreviews != null,
                )
                Text(
                    when (pendingUpdateCount) {
                        null -> "Updates"
                        0 -> "Updates (0)"
                        else -> "Updates ($pendingUpdateCount)"
                    },
                    fontSize = 12.sp,
                    color = JewelTheme.contentColor.copy(
                        alpha = if (!busy && model.updatePreviews != null) 0.85f else 0.4f,
                    ),
                )
            }

            OutlinedButton(
                onClick = { publish(ModpackMsg.StatusCheckRequested) },
                enabled = !busy,
            ) {
                Text(if (busy && model.actionName == "Checking updates") "Checking…" else "Check updates")
            }
        }

        if (activeCount > 0) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                model.filterTypes.forEach { type ->
                    Chip(onClick = { publish(ModpackMsg.FilterTypesChanged(model.filterTypes - type)) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(type.serialName)
                            Icon(
                                AllIconsKeys.General.CloseSmall,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
                model.filterSides.forEach { side ->
                    Chip(onClick = { publish(ModpackMsg.FilterSidesChanged(model.filterSides - side)) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(side.name.lowercase())
                            Icon(
                                AllIconsKeys.General.CloseSmall,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
                model.filterProviders.forEach { serial ->
                    Chip(onClick = { publish(ModpackMsg.FilterProvidersChanged(model.filterProviders - serial)) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(serial)
                            Icon(
                                AllIconsKeys.General.CloseSmall,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
                Link(
                    text = "Clear",
                    onClick = {
                        publish(ModpackMsg.FilterTypesChanged(emptySet()))
                        publish(ModpackMsg.FilterSidesChanged(emptySet()))
                        publish(ModpackMsg.FilterProvidersChanged(emptySet()))
                    },
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Composable
private fun FiltersMenuContent(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
) {
    Column(
        Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilterGroup(title = "Type") {
            ProjectType.entries.forEach { type ->
                FilterCheckRow(
                    label = type.serialName,
                    checked = type in model.filterTypes,
                    onCheckedChange = { checked ->
                        val next = if (checked) model.filterTypes + type else model.filterTypes - type
                        publish(ModpackMsg.FilterTypesChanged(next))
                    },
                )
            }
        }
        FilterGroup(title = "Side") {
            ProjectSide.entries.forEach { side ->
                FilterCheckRow(
                    label = side.name.lowercase(),
                    checked = side in model.filterSides,
                    onCheckedChange = { checked ->
                        val next = if (checked) model.filterSides + side else model.filterSides - side
                        publish(ModpackMsg.FilterSidesChanged(next))
                    },
                )
            }
        }
        FilterGroup(title = "Provider") {
            Provider.providers.forEach { provider ->
                FilterCheckRow(
                    label = provider.serialName,
                    checked = provider.serialName in model.filterProviders,
                    onCheckedChange = { checked ->
                        val next = if (checked) {
                            model.filterProviders + provider.serialName
                        } else {
                            model.filterProviders - provider.serialName
                        }
                        publish(ModpackMsg.FilterProvidersChanged(next))
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = JewelTheme.contentColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 2.dp),
        )
        content()
    }
}

@Composable
private fun FilterCheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, fontSize = 13.sp)
    }
}
