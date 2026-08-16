/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakku.api.platforms.Provider
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.animatedColor

@Composable
fun ProjectDisplay(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    val project = model.selectedProject ?: return
    val borderColor = animatedColor(JewelTheme.globalColors.borders.normal)
    val scrollState = rememberScrollState()
    val updateInfo = project.pakkuId?.let { model.updatePreviews?.get(it) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProjectCard(
                project = project,
                selected = true,
                updateInfo = updateInfo,
            ) {
                GradientHeader(it)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Project Files")

                project.files.forEach { projectFile ->
                    val provider = Provider.getProvider(projectFile.type)
                    val shortName = provider?.shortName ?: projectFile.type
                    val change = updateInfo?.fileChanges?.firstOrNull {
                        it.providerShortName == shortName && it.oldFile.fileName == projectFile.fileName
                    }

                    if (change != null && updateInfo?.applied != true && project.pakkuId != null) {
                        val pakkuId = project.pakkuId!!
                        ProjectFileUpdateCard(
                            change = change,
                            onSelectFile = { fileId ->
                                publish(
                                    ModpackMsg.UpdateFileSelected(
                                        pakkuId = pakkuId,
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
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .background(borderColor)
                    .height(1.dp)
                    .fillMaxWidth(),
            )

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
