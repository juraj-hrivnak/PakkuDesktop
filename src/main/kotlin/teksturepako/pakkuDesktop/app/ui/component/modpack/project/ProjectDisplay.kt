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
import teksturepako.pakkuDesktop.app.ui.LocalAppModel
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun ProjectDisplay(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    val project = model.selectedProject ?: return
    val isDark = LocalAppModel.current.profile.data.intUiTheme.isDark()
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProjectCard(project, isDark = isDark) {
                GradientHeader(it)
            }

            // Project Files Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Project Files")

                project.files.forEach { projectFile ->
                    ProjectFileCard(projectFile)
                }
            }

            Spacer(
                modifier = Modifier
                    .background(JewelTheme.globalColors.borders.normal)
                    .height(1.dp)
                    .fillMaxWidth()
            )

            ProjectProperties(publish, model)
        }

        VerticalScrollbar(
            scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}
