/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

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
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.modpack.meta.ModpackMetaProperties
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackTabModel
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

    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(PakkuDesktopConstants.commonPaddingSize),
        ) {
            ModpackMetaProperties(publish, model)
        }

        VerticalScrollbar(
            scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
        )
    }
}
