/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakkuDesktop.app.actions.uiKey
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun ProjectDisplay(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    val project = model.selectedProject ?: return
    val scrollState = rememberScrollState()
    val projectKey = project.uiKey()
    val updateInfo = model.updatePreviews?.get(projectKey)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(PakkuDesktopConstants.commonPaddingSize),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProjectCard(
                project = project,
                focused = true,
                updateInfo = updateInfo,
                statusChecked = model.updatePreviews != null,
            ) {
                GradientHeader(it)
            }

            GroupHeader("Project files")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                project.files.forEach { projectFile ->
                    val provider = Provider.getProvider(projectFile.type)
                    val shortName = provider?.shortName ?: projectFile.type
                    val change = updateInfo?.fileChanges?.firstOrNull {
                        it.providerShortName == shortName && it.oldFile.fileName == projectFile.fileName
                    }

                    if (change != null && updateInfo?.applied != true) {
                        ProjectFileUpdateCard(
                            change = change,
                            onSelectFile = { fileId ->
                                publish(
                                    ModpackMsg.UpdateFileSelected(
                                        projectKey = projectKey,
                                        providerShortName = change.providerShortName,
                                        fileId = fileId,
                                    ),
                                )
                            },
                        )
                    } else {
                        ProjectFileCard(
                            project = project,
                            projectFile = projectFile,
                            configFile = model.configFile?.get(),
                        )
                    }
                }
            }

            ProjectProperties(publish, model)
        }

        VerticalScrollbar(
            scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
        )
    }
}
