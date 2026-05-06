/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

// ---------------------------------------------------------------------------
// CompositionLocal — provided to the whole view tree by directoryPickerDriver
// ---------------------------------------------------------------------------

/** Provides a function that launches the directory picker. */
val LocalPickDirectory = compositionLocalOf { {} }

// ---------------------------------------------------------------------------
// directoryPickerDriver — provides LocalPickDirectory CompositionLocal
// ---------------------------------------------------------------------------

fun directoryPickerDriver(): Driver<AppModel, AppMsg> = { publish, _, content ->
    val launcher = rememberDirectoryPickerLauncher(
        title = "Open modpack directory",
    ) { directory ->
        directory?.path?.let { publish(AppMsg.DirectoryPicked(it)) }
    }

    CompositionLocalProvider(LocalPickDirectory provides { launcher.launch() }) {
        content()
    }
}

