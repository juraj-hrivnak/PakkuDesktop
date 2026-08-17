/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import io.klogging.logger
import teksturepako.pakku.api.actions.createAdditionRequest
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.AlreadyAdded
import teksturepako.pakku.api.actions.errors.ErrorSeverity
import teksturepako.pakku.api.actions.errors.NotFoundOn
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.platforms.CurseForge
import teksturepako.pakku.api.platforms.GitHub
import teksturepako.pakku.api.platforms.Platform
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakku.api.projects.Project
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData

private val addLogger = logger("AddImpl")

private class SimpleError(override val rawMessage: String) : ActionError()

/**
 * Resolves args via Pakku [createAdditionRequest] and previews deps via [previewDependenciesDesktop].
 * Does not mutate the lock file — deps are auto-added on [applyAdditionPlan].
 */
suspend fun buildAdditionPlan(
    lockFile: LockFile,
    query: String,
    onProgress: suspend (String) -> Unit = {},
    onToast: suspend (ToastData) -> Unit = {},
): AdditionPlan {
    val args = splitQueryArgs(query)
    if (args.isEmpty()) {
        return AdditionPlan(messages = listOf(SimpleError("Enter one or more projects.")))
    }

    val platforms = lockFile.getPlatforms().getOrElse { error ->
        onToast(actionErrorToast(error))
        return AdditionPlan(messages = listOf(error))
    }
    val projectProvider = lockFile.getProjectProvider().getOrElse { error ->
        onToast(actionErrorToast(error))
        return AdditionPlan(messages = listOf(error))
    }

    val entries = mutableListOf<AdditionEntry>()
    val messages = mutableListOf<ActionError>()
    val accountedDeps = mutableListOf<Project>()

    for (arg in args) {
        onProgress("Resolving $arg…")
        val projectIn = resolveProjectWithFiles(arg, lockFile, projectProvider).getOrElse { error ->
            addLogger.error(error.toUiMessage())
            if (messages.none { it.fingerprint() == error.fingerprint() }) {
                messages += error
            }
            onToast(actionErrorToast(error))
            null
        } ?: continue

        val entry = buildEntryForProject(
            projectIn = projectIn,
            lockFile = lockFile,
            platforms = platforms,
            projectProvider = projectProvider,
            onToast = onToast,
            messages = messages,
            onProgress = onProgress,
            accountedDeps = accountedDeps,
            strict = true,
        ) ?: continue

        // Roots also occupy the accounted set so they aren't re-listed as deps of each other.
        if (accountedDeps.none { it isAlmostTheSameAs entry.project }) {
            accountedDeps += entry.project
        }
        entries += entry
    }

    return AdditionPlan(entries = entries, messages = messages.distinctBy { it.fingerprint() })
}

/**
 * Applies accepted entries: add/update + link + auto [resolveDependenciesDesktop] (CLI path).
 * Emits one summary toast of what was added (modal-style [ProjectRef]s).
 */
suspend fun applyAdditionPlan(
    lockFile: LockFile,
    configFile: ConfigFile,
    plan: AdditionPlan,
    resolveDeps: Boolean = true,
    onToast: suspend (ToastData) -> Unit,
) {
    if (plan.isEmpty) {
        onToast(actionInfoToast(plan.messages.firstOrNull()?.toUiMessage() ?: "No projects to add."))
        return
    }

    val platforms = lockFile.getPlatforms().getOrElse { error ->
        onToast(actionErrorToast(error))
        return
    }
    val projectProvider = lockFile.getProjectProvider().getOrElse { error ->
        onToast(actionErrorToast(error))
        return
    }

    val addedLines = mutableListOf<AddedProjectLine>()
    for (entry in plan.entries) {
        entry.project.createAdditionRequest(
            onError = { error ->
                if (error !is AlreadyAdded) {
                    addLogger.error(error.toUiMessage())
                    onToast(actionErrorToast(error))
                }
                // AlreadyAdded: skip silently — user already reviewed this in the modal.
                if (error is CurseForge.Unauthenticated) {
                    onToast(actionInfoToast("Set a CurseForge API key in Settings to add CurseForge projects."))
                }
            },
            onSuccess = { project, _, replacing, reqHandlers ->
                if (replacing == null) lockFile.add(project) else lockFile.update(project)
                lockFile.linkProjectToDependents(project)
                addedLines += AddedProjectLine(project, replacing)
                if (resolveDeps) {
                    project.resolveDependenciesDesktop(
                        reqHandlers = reqHandlers,
                        lockFile = lockFile,
                        projectProvider = projectProvider,
                        platforms = platforms,
                        onDependencyAdded = { dep ->
                            addedLines += AddedProjectLine(dep)
                        },
                    )
                }
            },
            lockFile = lockFile,
            platforms = platforms,
            strict = false,
        )
    }

    if (addedLines.isNotEmpty()) {
        lockFile.write()
        configFile.write()
        onToast(actionAddedToast(addedLines.distinctBy { it.project.uiKey() }))
    } else {
        onToast(actionInfoToast(plan.messages.firstOrNull()?.toUiMessage() ?: "No projects were added."))
    }
}

/** DnD / auto: apply only [AdditionEntry.isRecommended] entries (CLI default = yes). */
suspend fun addSuspend(
    lockFile: LockFile,
    configFile: ConfigFile,
    query: String,
    onToast: suspend (ToastData) -> Unit,
) {
    val plan = buildAdditionPlan(lockFile, query, onToast = onToast)
    if (plan.isEmpty) {
        onToast(actionInfoToast(plan.messages.firstOrNull()?.toUiMessage() ?: "No projects were added."))
        return
    }

    val accepted = plan.entries.filter { it.isRecommended }
    for (entry in plan.entries.filterNot { it.isRecommended }) {
        val why = entry.warnings.firstOrNull()?.toUiMessage() ?: "not recommended"
        onToast(actionInfoToast("Skipped ${entry.project.displayLabel()} ($why)."))
    }
    if (accepted.isEmpty()) return

    applyAdditionPlan(lockFile, configFile, AdditionPlan(accepted, plan.messages), onToast = onToast)
}

private suspend fun buildEntryForProject(
    projectIn: Project,
    lockFile: LockFile,
    platforms: List<Platform>,
    projectProvider: Provider,
    onToast: suspend (ToastData) -> Unit,
    messages: MutableList<ActionError>,
    onProgress: suspend (String) -> Unit,
    accountedDeps: MutableList<Project>,
    strict: Boolean,
): AdditionEntry? {
    val entryWarnings = mutableListOf<ActionError>()
    var result: AdditionEntry? = null
    var retryWithoutStrict = false

    projectIn.createAdditionRequest(
        onError = { error ->
            when {
                error is AlreadyAdded -> {
                    entryWarnings += error
                }
                error is NotFoundOn && strict -> {
                    // Expected when probing all platforms; retry without strict — do not surface.
                    retryWithoutStrict = true
                }
                error is NotFoundOn -> {
                    // Soft multi-provider miss after success path — omit from GUI (row already shows project).
                }
                else -> {
                    entryWarnings += error
                    when {
                        error is CurseForge.Unauthenticated -> {
                            onToast(actionErrorToast(error))
                            onToast(actionInfoToast("Set a CurseForge API key in Settings to add CurseForge projects."))
                        }
                        error.severity == ErrorSeverity.FATAL -> {
                            onToast(actionErrorToast(error))
                        }
                    }
                }
            }
        },
        onSuccess = { project, isRecommended, replacing, _ ->
            result = AdditionEntry(
                project = project,
                isRecommended = isRecommended,
                replacing = replacing,
                warnings = entryWarnings.distinctBy { it.fingerprint() },
            )
        },
        lockFile = lockFile,
        platforms = platforms,
        strict = strict,
    )

    if (retryWithoutStrict && result == null) {
        return buildEntryForProject(
            projectIn, lockFile, platforms, projectProvider, onToast, messages, onProgress, accountedDeps, strict = false,
        )
    }

    val entry = result
    if (entry == null) {
        // True failures only — not duplicated under a row.
        for (error in entryWarnings.distinctBy { it.fingerprint() }) {
            if (messages.none { it.fingerprint() == error.fingerprint() }) {
                messages += error
            }
        }
        return null
    }

    onProgress("Resolving dependencies for ${entry.project.displayLabel()}…")
    val deps = entry.project.previewDependenciesDesktop(
        lockFile, projectProvider, platforms, accountedDeps,
    )
    return entry.copy(deps = deps)
}

internal fun splitQueryArgs(query: String): List<String> =
    query
        .lines()
        .flatMap { line -> line.split(',', ' ', '\t') }
        .map { it.trim().replace('=', ':') }
        .filter { it.isNotEmpty() }

/**
 * Resolves a free-form arg to a project with files.
 * Supports `mr:slug`, `cf:slug`, `gh:owner/repo`, `owner/repo`, `slug`, and `slug:fileId`.
 */
private suspend fun resolveProjectWithFiles(
    arg: String,
    lockFile: LockFile,
    defaultProvider: Provider,
): Result<Project, ActionError> {
    val mcVersions = lockFile.getMcVersions()
    val loaders = lockFile.getLoaders()

    val prefixed = arg.split(':', limit = 3)
    if (prefixed.size >= 2) {
        val provider = Provider.providers.find {
            it.shortName.equals(prefixed[0], ignoreCase = true) ||
                it.serialName.equals(prefixed[0], ignoreCase = true)
        }
        if (provider != null) {
            val rest = prefixed.drop(1).joinToString(":")
            return if (provider.serialName == GitHub.serialName) {
                val (input, tag) = splitGitHubInput(rest)
                provider.requestProjectWithFiles(emptyList(), emptyList(), input, tag)
            } else {
                val (input, fileId) = splitCommonInput(rest)
                provider.requestProjectWithFiles(mcVersions, loaders, input, fileId)
            }
        }
    }

    if ('/' in arg) {
        val github = Provider.getProvider("github")
            ?: return Err(SimpleError("GitHub provider is unavailable"))
        val cleaned = arg.removePrefix("https://").removePrefix("http://").removePrefix("github.com/")
        val (input, tag) = splitGitHubInput(cleaned)
        return github.requestProjectWithFiles(emptyList(), emptyList(), input, tag)
    }

    val (input, fileId) = splitCommonInput(arg)
    return defaultProvider.requestProjectWithFiles(mcVersions, loaders, input, fileId)
}

private fun splitCommonInput(arg: String): Pair<String, String?> {
    val parts = arg.split(':', limit = 2)
    return parts[0] to parts.getOrNull(1)
}

private fun splitGitHubInput(arg: String): Pair<String, String?> {
    val atSplit = arg.split('@', limit = 2)
    if (atSplit.size == 2) return atSplit[0] to atSplit[1]
    val parts = arg.split('/', limit = 3)
    return if (parts.size >= 2) {
        "${parts[0]}/${parts[1]}" to parts.getOrNull(2)
    } else {
        arg to null
    }
}
