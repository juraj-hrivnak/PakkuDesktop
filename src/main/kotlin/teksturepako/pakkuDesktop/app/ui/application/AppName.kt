/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application

import teksturepako.pakku.VERSION
import teksturepako.pakkuDesktop.pro.ui.viewmodel.LicenseKeyViewModel

const val PAKKU_DESKTOP_NAME = "Pakku Desktop"

val appName: String = buildString {
    append(PAKKU_DESKTOP_NAME)
    if (LicenseKeyViewModel.isActivated == true)
    {
        append(" Pro")
    }
}

val appNameWithVersion: String = buildString {
    append(PAKKU_DESKTOP_NAME)
    append(" $VERSION")
    if (LicenseKeyViewModel.isActivated == true)
    {
        append(" Pro")
    }
}
