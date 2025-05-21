package teksturepako.pakkuDesktop.ui.application

import teksturepako.pakku.VERSION
import teksturepako.pakkuDesktop.ui.viewmodel.LicenseKeyViewModel

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
