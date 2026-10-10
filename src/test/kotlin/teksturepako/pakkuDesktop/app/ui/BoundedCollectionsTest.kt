/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.Instant
import teksturepako.pakkuDesktop.app.actions.AppToast
import teksturepako.pakkuDesktop.app.data.Profile
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.ui.model.MAX_TOASTS
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.pkui.component.toast.ToastData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class BoundedCollectionsTest {
    @Test
    fun toastsKeepNewestUpToCap() {
        val toasts = (0 until MAX_TOASTS + 3).map { ToastData(id = "t$it", body = AppToast.Message("m$it")) }
        val model = toasts.fold(ModpackModel()) { m, toast -> modpackUpdate(ModpackMsg.ToastAdded(toast), m) }
        assertEquals(toasts.takeLast(MAX_TOASTS), model.toasts)
    }

    @Test
    fun recentProfilesKeepNewestUpToCap() {
        val max = ProfileData.MAX_RECENT_PROFILES
        val start = Instant.fromEpochSeconds(0)
        val data = (0 until max + 5).fold(ProfileData()) { d, i ->
            d.withCurrentProfile(Profile(name = "p$i", path = "/p$i", lastOpened = start + i.seconds))
        }
        assertEquals("/p${max + 4}", data.currentProfile?.path)
        assertEquals((5 until max + 5).map { "/p$it" }.toSet(), data.recentProfiles.map { it.path }.toSet())
        assertEquals(max, data.recentProfiles.size)
    }

    @Test
    fun recentProfilesCapOnOversizedPersistedList() {
        val max = ProfileData.MAX_RECENT_PROFILES
        val start = Instant.fromEpochSeconds(0)
        val oversized = (0 until max + 10).map { i ->
            Profile(name = "p$i", path = "/p$i", lastOpened = start + i.seconds)
        }.toPersistentList()
        val data = ProfileData(recentProfiles = oversized).cappedRecentProfiles()
        assertEquals(max, data.recentProfiles.size)
        assertEquals((10 until max + 10).map { "/p$it" }.toSet(), data.recentProfiles.map { it.path }.toSet())
    }
}
