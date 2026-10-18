/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.data

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.io.readPathTextOrNull
import teksturepako.pakku.io.writeToFile
import teksturepako.pakkuDesktop.app.ui.application.theme.IntUiThemes
import dev.nucleusframework.core.runtime.NucleusApp
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Update channel. [id] is what generate-update-yml publishes and what
 * Release.yml classifies: alpha tags -> "alpha", beta/rc tags -> "beta",
 * stable tags -> "latest". Never introduce an "rc" id — the pipeline
 * publishes no rc feeds.
 */
enum class UpdateChannel(val id: String, val label: String) {
    ALPHA("alpha", "Alpha (unstable)"),
    BETA("beta", "Beta"),
    STABLE("latest", "Stable");

    /** Derives the default channel from the running packaged build's version string. */
    companion object {
        fun fromRunningVersion(): UpdateChannel {
            val version = NucleusApp.version ?: ""
            return when {
                version.contains("-alpha") -> ALPHA
                version.contains("-beta") || version.contains("-rc") -> BETA
                else -> STABLE
            }
        }
    }
}

@Serializable
data class ProfileData(
    @SerialName("current_profile") val currentProfile: Profile? = null,
    /** at most [MAX_RECENT_PROFILES], newest by [Profile.lastOpened]; enforced in [cappedRecentProfiles] */
    @SerialName("recent_profiles")
    @Serializable(with = ProfileListSerializer::class)
    val recentProfiles: PersistentList<Profile> = persistentListOf(),
    val theme: String = "Dark",
    @SerialName("ui_scale") val uiScale: Float = UI_SCALE_DEFAULT,
    /**
     * Update channel id ("alpha", "beta", "latest"). Stored as a plain String
     * for forward compatibility. Empty string means "not yet set" — falls back
     * to the version-derived default via [updateChannelEnum].
     */
    @SerialName("update_channel") val updateChannel: String = "",

    @Transient val closeDialog: CloseDialogData? = null
) {
    /** Resolves the stored channel string to an enum, falling back to the running version's channel. */
    val updateChannelEnum: UpdateChannel
        get() = if (updateChannel.isBlank()) {
            UpdateChannel.fromRunningVersion()
        } else {
            UpdateChannel.entries.find { it.id == updateChannel }
                ?: UpdateChannel.fromRunningVersion()
        }

    val currentProfilePath: Path?
        get() = currentProfile?.let { Path(it.path) }

    val recentProfilesFiltered: List<Profile>
        get() = recentProfiles
            .filterNot { recentProfiles ->
                recentProfiles.path == currentProfile?.path
            }
            .sortedByDescending { it.lastOpened }

    val intUiTheme
        get() = IntUiThemes.valueOf(this.theme)

    /** Makes [profile] current and records it (and the previous current profile) in [recentProfiles]. */
    fun withCurrentProfile(profile: Profile): ProfileData
    {
        val recents = recentProfiles.mutate { list ->
            val existingIdx = list.indexOfFirst { it.path == profile.path }
            if (existingIdx >= 0) list[existingIdx] = profile else list.add(profile)

            if (currentProfile != null && list.none { it.path == currentProfile.path }) list.add(currentProfile)
        }
        return copy(currentProfile = profile, recentProfiles = recents).cappedRecentProfiles()
    }

    /** Drops oldest-opened profiles beyond [MAX_RECENT_PROFILES]. Same instance if already within the cap. */
    fun cappedRecentProfiles(): ProfileData
    {
        if (recentProfiles.size <= MAX_RECENT_PROFILES) return this
        val newest = recentProfiles.sortedByDescending { it.lastOpened }.take(MAX_RECENT_PROFILES)
        return copy(recentProfiles = newest.toPersistentList())
    }

    data class CloseDialogData(
        val onClose: suspend () -> Unit,
        val forceClose: Boolean = false
    )

    companion object
    {
        const val FILE_NAME = "profile-data.json"
        const val UI_SCALE_MIN = 0.75f
        const val UI_SCALE_MAX = 2f
        const val UI_SCALE_DEFAULT = 1f
        const val MAX_RECENT_PROFILES = 20
        val UI_SCALE_PRESETS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

        fun coerceUiScale(value: Float): Float = value.coerceIn(UI_SCALE_MIN, UI_SCALE_MAX)

        suspend fun readOrNew(): ProfileData
        {
            val text = readPathTextOrNull(Path(FILE_NAME)) ?: return ProfileData()

            return runCatching { jsonEncodeDefaults.decodeFromString<ProfileData>(text) }.getOrNull()
                ?.cappedRecentProfiles()
                ?: ProfileData()
        }
    }

    suspend fun write(): ActionError? = writeToFile<ProfileData>(
        this, FILE_NAME, format = jsonEncodeDefaults
    )
}
