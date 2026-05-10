/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application

import teksturepako.pakku.VERSION

const val PAKKU_DESKTOP_NAME = "Pakku Desktop"

fun appName(isProActivated: Boolean?) = buildString {
    append(PAKKU_DESKTOP_NAME)
    if (isProActivated == true) {
        append(" Pro")
    }
}

fun appNameWithVersion(isProActivated: Boolean?) = buildString {
    append(PAKKU_DESKTOP_NAME)
    append(" $VERSION")
    if (isProActivated == true) {
        append(" Pro")
    }
}
