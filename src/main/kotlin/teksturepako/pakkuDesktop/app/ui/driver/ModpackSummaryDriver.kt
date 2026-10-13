/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import com.github.michaelbull.result.get
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.ModpackSummary
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import teksturepako.pakkuDesktop.elm.Driver
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

// -- modpackSummaryDriver --

val modpackSummaryDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    LaunchedEffect(model.screen, model.welcome.profileData.recentProfiles) {
        if (model.screen != AppScreen.Welcome) return@LaunchedEffect
        val paths = model.welcome.profileData.recentProfilesFiltered.map { it.path }
        val summaries = withContext(Dispatchers.IO) {
            paths.associateWith { readModpackSummary(Path(it)) }.toPersistentMap()
        }
        publish(AppMsg.Welcome(WelcomeMsg.SummariesLoaded(summaries)))
    }

    content()
}

private suspend fun readModpackSummary(dir: Path): ModpackSummary {
    if (!dir.isDirectory()) return ModpackSummary(ModpackSummary.State.Missing)

    val config = ConfigFile.readToResultFrom(dir.resolve(ConfigFile.FILE_NAME)).get()
    val base = ModpackSummary(
        state = ModpackSummary.State.NotInitialized,
        name = config?.getName()?.takeIf { it.isNotBlank() },
        version = config?.getVersion()?.takeIf { it.isNotBlank() },
        author = config?.getAuthor()?.takeIf { it.isNotBlank() },
    )

    val lockPath = dir.resolve(LockFile.FILE_NAME)
    if (!lockPath.exists()) return base
    val lock = LockFile.readToResultFrom(lockPath, inheritConfig = false).get()
        ?: return base.copy(state = ModpackSummary.State.Unreadable)

    return base.copy(
        state = ModpackSummary.State.Ready,
        mcVersions = lock.getMcVersions().toPersistentList(),
        loaders = lock.getLoadersWithVersions().toPersistentList(),
        target = lock.getProjectProvider().get()?.name,
        projectCount = lock.getAllProjects().size,
    )
}
