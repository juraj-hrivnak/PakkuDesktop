/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import teksturepako.pakkuDesktop.app.data.ProfileData

// -- WelcomeDropdown --

data class WelcomeDropdownModel(
    val profileData: ProfileData = ProfileData(),
)

sealed interface WelcomeDropdownMsg {
    // parent
    data object NewModpack : WelcomeDropdownMsg
    data object ShowSettings : WelcomeDropdownMsg
    data class RecentProfile(val path: String) : WelcomeDropdownMsg
}

// -- WelcomeMsg --

sealed interface WelcomeMsg {
    data object ShowNewModpack : WelcomeMsg
    data object ShowSettings : WelcomeMsg
    data class DirectoryPicked(val path: String) : WelcomeMsg
    data class WelcomeDropdown(val msg: WelcomeDropdownMsg) : WelcomeMsg
}
