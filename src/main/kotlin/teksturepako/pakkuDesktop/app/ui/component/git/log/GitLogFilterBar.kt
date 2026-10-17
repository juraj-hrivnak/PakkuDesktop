/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Instant
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.separator
import teksturepako.pakkuDesktop.app.git.GitLogDateFilter
import teksturepako.pakkuDesktop.app.git.GitLogFilters
import teksturepako.pakkuDesktop.app.git.GitRef
import teksturepako.pakkuDesktop.app.git.GitRefType
import teksturepako.pakkuDesktop.app.ui.component.button.ToolbarToggleButton
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.PkUiDropdown
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun GitLogFilterBar(
    filters: GitLogFilters,
    refs: List<GitRef>,
    authors: List<String>,
    branchesOpen: Boolean,
    onBranchesOpen: (Boolean) -> Unit,
    publish: (ModpackMsg) -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = rememberTextFieldState(filters.text)
    val latest by rememberUpdatedState(filters)
    LaunchedEffect(text) {
        snapshotFlow { text.text.toString() }
            .distinctUntilChanged()
            .collect { if (it != latest.text) publish(ModpackMsg.GitLogFiltersChanged(latest.copy(text = it))) }
    }

    fun update(next: GitLogFilters) {
        if (next != filters) publish(ModpackMsg.GitLogFiltersChanged(next))
    }

    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolbarToggleButton(
            key = AllIconsKeys.Actions.Preview,
            label = "Branches",
            value = branchesOpen,
            onValueChange = onBranchesOpen,
        )
        TextField(
            state = text,
            modifier = Modifier.widthIn(min = 160.dp, max = 280.dp),
            placeholder = { Text("Text or hash") },
        )
        ToolbarToggleButton(
            key = AllIconsKeys.Actions.Find,
            label = "Match case",
            value = filters.matchCase,
            onValueChange = { update(filters.copy(matchCase = it)) },
        )
        ToolbarToggleButton(
            key = AllIconsKeys.Actions.Regex,
            label = "Regex",
            value = filters.regex,
            onValueChange = { update(filters.copy(regex = it)) },
        )
        FilterMenu(label = branchLabel(filters), modifier = Modifier.widthIn(max = 180.dp)) {
            selectableItem(selected = filters.branches == null, onClick = { update(filters.copy(branches = null)) }) {
                Text("All branches")
            }
            separator()
            refs.filter { it.type != GitRefType.Tag }.forEach { ref ->
                val selected = filters.branches?.contains(ref.name) == true
                selectableItem(selected = selected, onClick = { update(filters.copy(branches = toggle(filters.branches, ref.name))) }) {
                    Text(ref.name)
                }
            }
        }
        FilterMenu(label = userLabel(filters), modifier = Modifier.widthIn(max = 160.dp)) {
            selectableItem(selected = filters.users.isEmpty(), onClick = { update(filters.copy(users = persistentSetOf())) }) {
                Text("All users")
            }
            separator()
            authors.forEach { author ->
                selectableItem(selected = author in filters.users, onClick = {
                    val next = if (author in filters.users) filters.users.remove(author) else filters.users.add(author)
                    update(filters.copy(users = next))
                }) { Text(author) }
            }
        }
        FilterMenu(label = dateLabel(filters), modifier = Modifier.widthIn(max = 140.dp)) {
            selectableItem(selected = filters.date == null, onClick = { update(filters.copy(date = null)) }) { Text("Any time") }
            separator()
            dateChoice("Today", filters, 0, ::update)
            dateChoice("Last 7 days", filters, 7, ::update)
            dateChoice("Last 30 days", filters, 30, ::update)
            dateChoice("Last year", filters, 365, ::update)
        }
    }
}

@Composable
private fun FilterMenu(label: String, modifier: Modifier, menu: MenuScope.() -> Unit) {
    PkUiDropdown(modifier = modifier, menuContent = menu) {
        Text(label, maxLines = 1)
    }
}

private fun MenuScope.dateChoice(label: String, filters: GitLogFilters, days: Int, update: (GitLogFilters) -> Unit) {
    selectableItem(selected = matchesPreset(filters.date, days), onClick = {
        update(filters.copy(date = GitLogDateFilter(sinceDaysAgo(days), null)))
    }) { Text(label) }
}

private fun toggle(current: kotlinx.collections.immutable.PersistentSet<String>?, name: String) =
    when {
        current == null -> persistentSetOf(name)
        name in current -> current.remove(name).takeIf { it.isNotEmpty() }
        else -> current.add(name)
    }

private fun branchLabel(filters: GitLogFilters): String {
    val branches = filters.branches ?: return "Branch"
    return when (branches.size) {
        0 -> "Branch"
        1 -> branches.first()
        else -> "${branches.size} branches"
    }
}

private fun userLabel(filters: GitLogFilters): String = when (filters.users.size) {
    0 -> "User"
    1 -> filters.users.first()
    else -> "${filters.users.size} users"
}

private fun dateLabel(filters: GitLogFilters): String = when {
    filters.date == null -> "Date"
    matchesPreset(filters.date, 0) -> "Today"
    matchesPreset(filters.date, 7) -> "Last 7 days"
    matchesPreset(filters.date, 30) -> "Last 30 days"
    matchesPreset(filters.date, 365) -> "Last year"
    else -> "Date"
}

private fun matchesPreset(date: GitLogDateFilter?, days: Int): Boolean {
    val since = date?.since ?: return false
    if (date.until != null) return false
    if (days == 0) {
        val zone = java.time.ZoneId.systemDefault()
        val start = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().epochSecond
        return since.epochSeconds == start
    }
    val age = java.time.Instant.now().epochSecond - since.epochSeconds
    val target = days * 86_400L
    return age in (target - 2)..(target + 2)
}

private fun sinceDaysAgo(days: Int): Instant {
    val zone = java.time.ZoneId.systemDefault()
    val start = if (days == 0) java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant()
    else java.time.Instant.now().minus(java.time.Duration.ofDays(days.toLong()))
    return Instant.fromEpochSeconds(start.epochSecond)
}
