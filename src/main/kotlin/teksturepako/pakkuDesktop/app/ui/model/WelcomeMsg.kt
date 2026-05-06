/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.model

sealed interface WelcomeMsg {
    data object ShowNewModpack : WelcomeMsg
    data object ShowSettings : WelcomeMsg
    data class DirectoryPicked(val path: String) : WelcomeMsg
}

