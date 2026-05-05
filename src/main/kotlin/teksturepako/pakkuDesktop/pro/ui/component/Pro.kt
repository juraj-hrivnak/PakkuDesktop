/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.ui.component

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.app.ui.LocalAppModel

@Composable
fun Pro(content: @Composable () -> Unit) {
    val model = LocalAppModel.current
    if (model.isProActivated == true) {
        content()
    }
}
