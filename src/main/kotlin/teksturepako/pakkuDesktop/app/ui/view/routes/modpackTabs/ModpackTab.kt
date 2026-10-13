/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackContentMaxWidth
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackMetaProperties
import teksturepako.pakkuDesktop.app.ui.model.ModpackTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.RecomposeProbe

@Composable
fun ModpackTab(publish: (ModpackMsg) -> Unit, model: ModpackTabModel) {
    RecomposeProbe("ModpackTab")
    if (model.configFile?.get() == null || model.lockFile?.get() == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Load a modpack to edit metadata.",
                color = JewelTheme.contentColor.copy(alpha = 0.65f),
            )
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = ModpackContentMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            ModpackMetaProperties(publish, model)
        }
    }
}
