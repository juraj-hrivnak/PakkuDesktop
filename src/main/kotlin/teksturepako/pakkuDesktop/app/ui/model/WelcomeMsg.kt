/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

import teksturepako.pakkuDesktop.app.data.ProfileData

// ---------------------------------------------------------------------------
// WelcomeDropdown child model & messages
// ---------------------------------------------------------------------------

data class WelcomeDropdownModel(
    val profileData: ProfileData = ProfileData(),
)

sealed interface WelcomeDropdownMsg {
    // Cross-cutting — parent handles, child returns model unchanged
    data object NewModpack : WelcomeDropdownMsg
    data class RecentProfile(val path: String) : WelcomeDropdownMsg
}

// ---------------------------------------------------------------------------
// WelcomeMsg
// ---------------------------------------------------------------------------

sealed interface WelcomeMsg {
    data object ShowNewModpack : WelcomeMsg
    data object ShowSettings : WelcomeMsg
    data class DirectoryPicked(val path: String) : WelcomeMsg
    /** Fractal delegation — wraps all WelcomeDropdown child messages. */
    data class WelcomeDropdown(val msg: WelcomeDropdownMsg) : WelcomeMsg
}
