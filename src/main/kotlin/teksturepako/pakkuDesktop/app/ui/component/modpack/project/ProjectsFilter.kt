/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.ProjectSide
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

private val FilterProviderSerialNames = setOf("curseforge", "modrinth", "github")

/**
 * Search + collapsible filter toggles (type, side, provider, redistributable, updates).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectFilter(
    publish: (ModpackMsg) -> Unit,
    model: ModpackModel,
    focusRequester: FocusRequester,
) {
    val textFieldState = rememberTextFieldState(model.projectsFilterText)

    val filterProviders = remember {
        Provider.providers.filter { it.serialName in FilterProviderSerialNames }
    }

    val activeCount = model.filterTypes.size +
        model.filterSides.size +
        (if (model.filterMissingSide) 1 else 0) +
        model.filterProviders.size +
        (if (model.filterRedistributable != null) 1 else 0) +
        (if (model.filterUpdatesOnly) 1 else 0)
    val anyFilterActive = activeCount > 0
    var filtersExpanded by remember { mutableStateOf(anyFilterActive) }

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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                textFieldState,
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { publish(ModpackMsg.ProjectsFilterFocusChanged(it.isFocused)) },
                placeholder = { Text("Search projects...") },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { filtersExpanded = !filtersExpanded },
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        key = if (filtersExpanded) {
                            AllIconsKeys.General.ChevronDown
                        } else {
                            AllIconsKeys.General.ChevronRight
                        },
                        contentDescription = if (filtersExpanded) "Hide filters" else "Show filters",
                        tint = JewelTheme.contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (anyFilterActive) "Filters · $activeCount" else "Filters",
                        color = JewelTheme.contentColor.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                    )
                }
                if (anyFilterActive) {
                    Link(
                        text = "Clear",
                        onClick = {
                            publish(ModpackMsg.FilterSidesChanged(emptySet()))
                            publish(ModpackMsg.FilterMissingSideChanged(false))
                            publish(ModpackMsg.FilterTypesChanged(emptySet()))
                            publish(ModpackMsg.FilterProvidersChanged(emptySet()))
                            publish(ModpackMsg.FilterRedistributableChanged(null))
                            publish(ModpackMsg.FilterUpdatesOnlyChanged(false))
                        },
                    )
                }
            }
        }

        if (filtersExpanded) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
            ProjectType.entries.forEach { type ->
                val active = type in model.filterTypes
                FilterToggle(
                    label = type.prettyName,
                    active = active,
                    onClick = {
                        val next = if (active) model.filterTypes - type else model.filterTypes + type
                        publish(ModpackMsg.FilterTypesChanged(next))
                    },
                )
            }

            ProjectSide.entries.forEach { side ->
                val active = side in model.filterSides
                FilterToggle(
                    label = side.prettyName.replaceFirstChar { it.titlecase() },
                    active = active,
                    onClick = {
                        val next = if (active) model.filterSides - side else model.filterSides + side
                        publish(ModpackMsg.FilterSidesChanged(next))
                    },
                )
            }
            FilterToggle(
                label = "No side",
                active = model.filterMissingSide,
                onClick = {
                    publish(ModpackMsg.FilterMissingSideChanged(!model.filterMissingSide))
                },
            )

            filterProviders.forEach { provider ->
                val serial = provider.serialName
                val active = serial in model.filterProviders
                FilterToggle(
                    label = provider.name,
                    active = active,
                    icon = { ProviderIcon(provider, Modifier.size(14.dp)) },
                    onClick = {
                        val next = if (active) {
                            model.filterProviders - serial
                        } else {
                            model.filterProviders + serial
                        }
                        publish(ModpackMsg.FilterProvidersChanged(next))
                    },
                )
            }

            FilterToggle(
                label = "Redistributable",
                active = model.filterRedistributable == true,
                onClick = {
                    publish(
                        ModpackMsg.FilterRedistributableChanged(
                            if (model.filterRedistributable == true) null else true,
                        ),
                    )
                },
            )
            FilterToggle(
                label = "Not redistributable",
                active = model.filterRedistributable == false,
                onClick = {
                    publish(
                        ModpackMsg.FilterRedistributableChanged(
                            if (model.filterRedistributable == false) null else false,
                        ),
                    )
                },
            )

            if (model.updatePreviews != null) {
                val busy = model.actionName != null
                FilterToggle(
                    label = "Has update",
                    active = model.filterUpdatesOnly,
                    enabled = !busy,
                    onClick = {
                        publish(ModpackMsg.FilterUpdatesOnlyChanged(!model.filterUpdatesOnly))
                    },
                )
            }
            }
        }
    }
}

@Composable
private fun FilterToggle(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = when {
        !enabled -> JewelTheme.contentColor.copy(alpha = 0.12f)
        active -> PakkuDesktopConstants.highlightColor.copy(alpha = 0.85f)
        else -> JewelTheme.contentColor.copy(alpha = 0.22f)
    }
    val background = when {
        !enabled -> Color.Transparent
        active -> PakkuDesktopConstants.highlightColor.copy(alpha = 0.14f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.invoke()
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = JewelTheme.contentColor.copy(
                alpha = when {
                    !enabled -> 0.4f
                    active -> 1f
                    else -> 0.72f
                },
            ),
        )
    }
}
