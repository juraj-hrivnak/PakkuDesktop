/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.LaunchedEffect
import com.github.michaelbull.result.get
import io.klogging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakkuDesktop.app.data.Profile
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.elm.Driver
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

private val logger = logger("ProfileDiskDriver")

// -- profileDiskDriver --

val profileDiskDriver: Driver<AppModel, AppMsg> = { publish, model, content ->
    // Load once on startup
    LaunchedEffect(Unit) {
        val data = withContext(Dispatchers.IO) { ProfileData.readOrNew() }
        // Restore workingPath for the already-selected profile so that
        // modpackDiskDriver reads files from the correct directory.
        data.currentProfile?.path?.let { path ->
            workingPath = path
            logger.info { "workingPath restored to [$workingPath] from saved profile" }
        }
        publish(AppMsg.ProfileLoaded(data))
    }

    // React to pending path (user picked a directory)
    LaunchedEffect(model.profile.pendingPath, model.profile.loaded) {
        val pending = model.profile.pendingPath ?: return@LaunchedEffect
        if (!model.profile.loaded) return@LaunchedEffect

        val path = Path(pending)

        withContext(Dispatchers.IO) {
            val modpackName = ConfigFile.readToResultFrom(
                Path("$path/${ConfigFile.FILE_NAME}")
            ).get()?.getName() ?: path.fileName.pathString

            val newProfile = Profile(
                name       = modpackName,
                path       = path.absolutePathString(),
                lastOpened = Clock.System.now()
            )

            val existing = model.profile.data
            val updatedRecentProfiles = buildList {
                val existingIdx = existing.recentProfiles.indexOfFirst { it.path == newProfile.path }
                if (existingIdx >= 0) {
                    addAll(existing.recentProfiles.toMutableList().also { it[existingIdx] = newProfile })
                } else {
                    addAll(existing.recentProfiles + newProfile)
                }
            }

            val withOldCurrentInRecents = if (existing.currentProfile != null &&
                updatedRecentProfiles.none { it.path == existing.currentProfile.path }
            ) {
                updatedRecentProfiles + existing.currentProfile
            } else updatedRecentProfiles

            val newData = existing.copy(
                currentProfile = newProfile,
                recentProfiles = withOldCurrentInRecents,
            )

            workingPath = path.absolutePathString()
            logger.info { "workingPath set to [$workingPath]" }
            newData.write()

            withContext(Dispatchers.Main) {
                publish(AppMsg.ProfileCurrentResolved(newData))
            }
        }
    }

    // back to Welcome when current profile is cleared
    LaunchedEffect(model.profile.data.currentProfile, model.profile.loaded) {
        if (!model.profile.loaded) return@LaunchedEffect
        if (model.profile.data.currentProfile == null && model.screen == AppScreen.Welcome) {
            withContext(Dispatchers.IO) {
                workingPath = "."
                model.profile.data.write()
            }
        }
    }

    LaunchedEffect(model.profile.data.theme, model.profile.loaded) {
        if (!model.profile.loaded) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            model.profile.data.write()
        }
    }

    content()
}

