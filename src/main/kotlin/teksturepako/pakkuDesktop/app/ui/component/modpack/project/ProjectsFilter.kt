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
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.ProjectListQuery
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.projectListQuery
import teksturepako.pakkuDesktop.app.ui.model.ProjectsTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

private val FilterProviderSerialNames = setOf("curseforge", "modrinth", "github")

/**
 * Search + collapsible filter toggles (type, side, provider, redistributable, updates).
 */
@Composable
fun ProjectFilter(
    publish: (ModpackMsg) -> Unit,
    model: ProjectsTabModel,
    focusRequester: FocusRequester,
) {
    val query = model.projectListQuery()
    ProjectFilterBar(
        query = query,
        onQuery = { next -> publishProjectListQuery(publish, query, next) },
        focusRequester = focusRequester,
        onFocusChanged = { publish(ModpackMsg.ProjectsFilterFocusChanged(it)) },
        showUpdates = model.updatePreviews != null,
        updatesEnabled = model.actionName == null,
    )
    LaunchedEffect(model.wantsFocusProjectsFilter) {
        if (!model.wantsFocusProjectsFilter) return@LaunchedEffect
        focusRequester.requestFocus()
        publish(ModpackMsg.FocusProjectsFilterConsumed)
    }
}

private fun publishProjectListQuery(
    publish: (ModpackMsg) -> Unit,
    current: ProjectListQuery,
    next: ProjectListQuery,
) {
    if (next.text != current.text) publish(ModpackMsg.FilterTextChanged(next.text))
    if (next.sides != current.sides) publish(ModpackMsg.FilterSidesChanged(next.sides))
    if (next.missingSide != current.missingSide) publish(ModpackMsg.FilterMissingSideChanged(next.missingSide))
    if (next.types != current.types) publish(ModpackMsg.FilterTypesChanged(next.types))
    if (next.providers != current.providers) publish(ModpackMsg.FilterProvidersChanged(next.providers))
    if (next.redistributable != current.redistributable) {
        publish(ModpackMsg.FilterRedistributableChanged(next.redistributable))
    }
    if (next.updatesOnly != current.updatesOnly) publish(ModpackMsg.FilterUpdatesOnlyChanged(next.updatesOnly))
}

/**
 * Search field and the same filter toggles as the Projects tab.
 * [showUpdates] reveals "Has update" when an update check has results to filter on.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectFilterBar(
    query: ProjectListQuery,
    onQuery: (ProjectListQuery) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    showUpdates: Boolean = false,
    updatesEnabled: Boolean = true,
) {
    val textFieldState = rememberTextFieldState(query.text)

    val filterProviders = remember {
        Provider.providers.filter { it.serialName in FilterProviderSerialNames }
    }

    val activeCount = query.types.size +
        query.sides.size +
        (if (query.missingSide) 1 else 0) +
        query.providers.size +
        (if (query.redistributable != null) 1 else 0) +
        (if (query.updatesOnly) 1 else 0)
    val anyFilterActive = activeCount > 0
    var filtersExpanded by remember { mutableStateOf(anyFilterActive) }
    LaunchedEffect(anyFilterActive) {
        if (anyFilterActive) filtersExpanded = true
    }

    LaunchedEffect(textFieldState.text) {
        val text = textFieldState.text.toString()
        if (text != query.text) onQuery(query.copy(text = text))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
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
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
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
                            onQuery(
                                query.copy(
                                    types = emptySet(),
                                    sides = emptySet(),
                                    missingSide = false,
                                    providers = emptySet(),
                                    redistributable = null,
                                    updatesOnly = false,
                                ),
                            )
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
                val active = type in query.types
                FilterToggle(
                    label = type.prettyName,
                    active = active,
                    onClick = {
                        val next = if (active) query.types - type else query.types + type
                        onQuery(query.copy(types = next))
                    },
                )
            }

            ProjectSide.entries.forEach { side ->
                val active = side in query.sides
                FilterToggle(
                    label = side.prettyName.replaceFirstChar { it.titlecase() },
                    active = active,
                    onClick = {
                        val next = if (active) query.sides - side else query.sides + side
                        onQuery(query.copy(sides = next))
                    },
                )
            }
            FilterToggle(
                label = "No side",
                active = query.missingSide,
                onClick = { onQuery(query.copy(missingSide = !query.missingSide)) },
            )

            filterProviders.forEach { provider ->
                val serial = provider.serialName
                val active = serial in query.providers
                FilterToggle(
                    label = provider.name,
                    active = active,
                    icon = { ProviderIcon(provider, Modifier.size(14.dp)) },
                    onClick = {
                        val next = if (active) query.providers - serial else query.providers + serial
                        onQuery(query.copy(providers = next))
                    },
                )
            }

            FilterToggle(
                label = "Redistributable",
                active = query.redistributable == true,
                onClick = {
                    onQuery(query.copy(redistributable = if (query.redistributable == true) null else true))
                },
            )
            FilterToggle(
                label = "Not redistributable",
                active = query.redistributable == false,
                onClick = {
                    onQuery(query.copy(redistributable = if (query.redistributable == false) null else false))
                },
            )

            if (showUpdates) {
                FilterToggle(
                    label = "Has update",
                    active = query.updatesOnly,
                    enabled = updatesEnabled,
                    onClick = { onQuery(query.copy(updatesOnly = !query.updatesOnly)) },
                )
            }
            }
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value

@Composable
internal fun FilterToggle(
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
