/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import teksturepako.pakkuDesktop.app.git.NativeGit
import teksturepako.pakkuDesktop.app.git.log.GitLogGraph
import teksturepako.pakkuDesktop.app.git.log.GitLogTopology
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.elm.Driver
import java.io.File

private const val SUBJECT_PAGE = 400
private const val SUBJECT_MARGIN = 100
private const val SUBJECT_CAP = 5_000
private const val SUBJECT_KEEP = 2_000

private data class ResolvedText(
    val text: String,
    val matchCase: Boolean,
    val regex: Boolean,
    /** Null when the text filter is off. */
    val matches: Set<String>?,
)

/**
 * Builds the Log graph off the UI thread and pages commit subjects around the rows on screen.
 * The topology reloads when branch and tag tips change, not on every file-watch refresh.
 */
val gitLogDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    val latest by rememberUpdatedState(model)
    val modpack = model.modpack
    val active = model.screen == AppScreen.Modpack && modpack.loaded &&
        modpack.selectedTab == SelectedTab.LOG
    val path = model.profile.data.currentProfile?.path
    val head = modpack.git.headHash
    val refs = modpack.git.refs
    val stamp = remember(refs, head) {
        buildString {
            append(head)
            append('\n')
            refs.forEach { append(it.type).append(' ').append(it.name).append(' ').append(it.hash).append('\n') }
        }
    }
    val filters = modpack.gitLog.filters

    var topology by remember { mutableStateOf(GitLogTopology.EMPTY) }
    var loadedStamp by remember { mutableStateOf<String?>(null) }
    var resolved by remember { mutableStateOf(ResolvedText("", false, false, null)) }
    var prep by remember { mutableStateOf<GitLogPrep?>(null) }

    LaunchedEffect(active, path, stamp) {
        if (!active || path == null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) { NativeGit.readLogTopology(File(path), head) }
        topology = loaded
        loadedStamp = stamp
    }

    LaunchedEffect(active, path, loadedStamp, filters.text, filters.matchCase, filters.regex) {
        if (!active || path == null || loadedStamp != stamp) return@LaunchedEffect
        val text = filters.text
        if (text.isBlank()) {
            resolved = ResolvedText(text, filters.matchCase, filters.regex, null)
            return@LaunchedEffect
        }
        delay(250)
        val root = File(path)
        val messages = withContext(Dispatchers.IO) {
            NativeGit.readMessageMatches(root, head, text, filters.matchCase, filters.regex)
        }
        val matches = HashSet(messages)
        val query = if (filters.matchCase) text else text.lowercase()
        for (hash in topology.hashes) {
            val shown = if (filters.matchCase) hash else hash.lowercase()
            if (shown.startsWith(query)) matches.add(hash)
        }
        resolved = ResolvedText(text, filters.matchCase, filters.regex, matches)
    }

    val branch = modpack.git.currentBranch?.name
    val upstream = modpack.git.upstream
    LaunchedEffect(active, path, loadedStamp, filters, resolved, branch, upstream) {
        if (!active || path == null || loadedStamp != stamp) return@LaunchedEffect
        val textReady = filters.text.isBlank() ||
            (resolved.text == filters.text && resolved.matchCase == filters.matchCase && resolved.regex == filters.regex)
        if (!textReady) return@LaunchedEffect
        val built = withContext(Dispatchers.Default) {
            GitLogGraph.build(topology, refs, head, branch, upstream, filters, resolved.matches)
        }
        prep = GitLogPrep(built)
    }

    val visibleRows = modpack.gitLog.visibleRows
    LaunchedEffect(active, path, prep, visibleRows) {
        val graph = prep?.graph ?: return@LaunchedEffect
        if (!active || path == null || graph.rows == 0 || visibleRows.isEmpty()) return@LaunchedEffect
        val subjects = latest.modpack.gitLog.subjects
        val from = (visibleRows.first - SUBJECT_MARGIN).coerceAtLeast(0)
        val to = (visibleRows.last + SUBJECT_MARGIN).coerceAtMost(graph.rows - 1)
        val missing = ArrayList<String>()
        for (row in from..to) {
            val hash = graph.hashAt(row)
            if (hash !in subjects) missing.add(hash)
        }
        val overCap = subjects.size + missing.size > SUBJECT_CAP
        if (missing.isEmpty() && !overCap) return@LaunchedEffect
        val loaded = LinkedHashMap<String, String>()
        if (missing.isNotEmpty()) {
            val root = File(path)
            withContext(Dispatchers.IO) {
                for (chunk in missing.chunked(SUBJECT_PAGE)) {
                    loaded.putAll(NativeGit.readSubjects(root, chunk))
                }
            }
        }
        val keep = if (overCap) {
            val keepFrom = (visibleRows.first - SUBJECT_KEEP).coerceAtLeast(0)
            val keepTo = (visibleRows.last + SUBJECT_KEEP).coerceAtMost(graph.rows - 1)
            HashSet<String>(keepTo - keepFrom + 1).apply {
                for (row in keepFrom..keepTo) add(graph.hashAt(row))
            }
        } else {
            null
        }
        publish(AppMsg.Modpack(ModpackMsg.GitLogSubjectsLoaded(loaded, keep)))
    }

    val selected = modpack.gitLog.selectedHash
    LaunchedEffect(active, path, selected) {
        val hash = selected ?: return@LaunchedEffect
        if (!active || path == null) return@LaunchedEffect
        val root = File(path)
        val details = withContext(Dispatchers.IO) { NativeGit.readCommitDetails(root, hash) } ?: return@LaunchedEffect
        publish(AppMsg.Modpack(ModpackMsg.GitLogDetailsLoaded(details)))
        val branches = withContext(Dispatchers.IO) { NativeGit.readContainingBranches(root, hash) }
        publish(AppMsg.Modpack(ModpackMsg.GitLogContainingBranchesLoaded(hash, branches)))
    }

    val selectedChange = modpack.gitLog.selectedChange
    val detailsHash = modpack.gitLog.details?.hash
    LaunchedEffect(active, path, selected, selectedChange, detailsHash) {
        val hash = selected ?: return@LaunchedEffect
        val change = selectedChange ?: return@LaunchedEffect
        val details = latest.modpack.gitLog.details
        if (!active || path == null || details == null || details.hash != hash) return@LaunchedEffect
        val diff = withContext(Dispatchers.IO) {
            NativeGit.computeCommitDiff(File(path), hash, details.parents.firstOrNull(), change)
        }
        publish(AppMsg.Modpack(ModpackMsg.GitLogDiffComputed(hash, change, diff)))
    }

    CompositionLocalProvider(LocalGitLogPrep provides prep, content = content)
}
