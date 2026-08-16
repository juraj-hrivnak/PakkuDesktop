/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.driver

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.path
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.elm.Driver

// -- CompositionLocal --

/** Provides a function that launches the modpack directory picker. */
val LocalPickDirectory = compositionLocalOf { {} }

// -- directoryPickerDriver --

fun directoryPickerDriver(): Driver<AppModel, AppMsg> = { publish, model, content ->
    val launcher = rememberDirectoryPickerLauncher(
        dialogSettings = FileKitDialogSettings(
            title = "Open modpack directory",
        ),
    ) { directory ->
        directory?.path?.let { publish(AppMsg.DirectoryPicked(it)) }
    }

    val cloneParentLauncher = rememberDirectoryPickerLauncher(
        dialogSettings = FileKitDialogSettings(
            title = "Choose parent directory",
        ),
    ) { directory ->
        directory?.path?.let { publish(AppMsg.CloneParentPicked(it)) }
    }

    LaunchedEffect(model.wantsDirectoryPicker) {
        if (!model.wantsDirectoryPicker) return@LaunchedEffect
        launcher.launch()
        publish(AppMsg.DirectoryPickerLaunched)
    }

    LaunchedEffect(model.wantsCloneParentPicker) {
        if (!model.wantsCloneParentPicker) return@LaunchedEffect
        cloneParentLauncher.launch()
        publish(AppMsg.CloneParentPickerLaunched)
    }

    CompositionLocalProvider(LocalPickDirectory provides { launcher.launch() }) {
        content()
    }
}
