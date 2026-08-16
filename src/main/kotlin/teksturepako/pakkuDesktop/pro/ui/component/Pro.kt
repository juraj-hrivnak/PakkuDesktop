/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.app.ui.model.AppModel

/** Renders [content] only when Pakku Pro is activated. */
@Composable
fun Pro(appModel: AppModel, content: @Composable () -> Unit) {
    if (appModel.isProActivated == true) {
        content()
    }
}
