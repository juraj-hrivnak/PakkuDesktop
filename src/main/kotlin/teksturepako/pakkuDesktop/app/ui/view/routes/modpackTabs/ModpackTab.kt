/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackMetaProperties
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun ModpackTab(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    Column(Modifier.fillMaxSize()) {
        if (model.configFile?.get() == null || model.lockFile?.get() == null) {
            Text(
                "Load a modpack to edit metadata.",
                Modifier.padding(16.dp),
                color = JewelTheme.contentColor.copy(alpha = 0.65f),
            )
            return
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            ModpackMetaProperties(publish, model)
        }
    }
}
