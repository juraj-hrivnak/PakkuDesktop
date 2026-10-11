/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import teksturepako.pakkuDesktop.app.data.HeapFootprint
import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.data.withHistoryFrom
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.InternalMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackDropdownModel
import teksturepako.pakkuDesktop.app.ui.model.SettingsCredentials
import teksturepako.pakkuDesktop.app.ui.model.withoutInternal
import teksturepako.pakkuDesktop.app.ui.model.SettingsTab
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProfileModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeDropdownModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AppUpdateSameInstanceTest {
    private fun assertNoOp(model: AppModel, msg: AppMsg) =
        assertSame(model, appUpdate(msg, model), msg.toString())

    @Test
    fun noOpModpackMsgKeepsAppModel() =
        assertNoOp(AppModel(), AppMsg.Modpack(ModpackMsg.ToastDismissed("missing")))

    @Test
    fun noOpListPrefMsgKeepsAppModel() =
        assertNoOp(AppModel(), AppMsg.Modpack(ModpackMsg.FilterTypesChanged(emptySet())))

    @Test
    fun hideNewModpackWhenHidden() = assertNoOp(AppModel(), AppMsg.HideNewModpack)

    @Test
    fun settingsTabSelectedWhenAlreadySelected() =
        assertNoOp(AppModel(), AppMsg.SettingsTabSelected(SettingsTab.General))

    @Test
    fun heapHistogramRequestedWhenAlreadyPending() =
        assertNoOp(AppModel(wantsHeapHistogram = true), AppMsg.HeapHistogramRequested)

    @Test
    fun heapFootprintSampledUnchangedKeepsModel() {
        val footprint = HeapFootprint(rssBytes = 100, heapUsedBytes = 20, epochMs = 1)
        assertNoOp(
            AppModel(heapFootprint = footprint),
            AppMsg.HeapFootprintSampled(HeapFootprint(rssBytes = 100, heapUsedBytes = 20, epochMs = 99)),
        )
    }

    @Test
    fun heapFootprintSampledRecordsActionChange() {
        val prev = HeapFootprint(rssBytes = 100, heapUsedBytes = 20, epochMs = 1).withHistoryFrom(null)
        val next = appUpdate(
            AppMsg.HeapFootprintSampled(
                HeapFootprint(rssBytes = 100, heapUsedBytes = 20, epochMs = 2),
                "Exporting",
            ),
            AppModel(heapFootprint = prev),
        )
        assertEquals("Exporting", next.heapFootprint?.history?.last()?.action)
    }

    @Test
    fun confirmCloseDialogWithoutDialog() = assertNoOp(AppModel(), AppMsg.ConfirmCloseDialog)

    @Test
    fun reportProActivatedSetsFlag() {
        val next = appUpdate(AppMsg.Internal(InternalMsg.ProActivationReported(true)), AppModel())
        assertEquals(true, next.isProActivated)
    }

    @Test
    fun reportProActivatedSameValueKeepsModel() =
        assertNoOp(AppModel(isProActivated = true), AppMsg.Internal(InternalMsg.ProActivationReported(true)))

    @Test
    fun credentialsLoadedGoesToInternalModel() {
        val next = appUpdate(AppMsg.Internal(InternalMsg.CredentialsLoaded("cf", "gh")), AppModel(wantsLoadCredentials = true))
        assertEquals(false, next.wantsLoadCredentials)
        assertEquals(SettingsCredentials("cf", "gh"), next.internal.settingsCredentials)
        assertEquals(AppModel(), next.withoutInternal())
    }

    @Test
    fun welcomeChildUnchangedKeepsWelcome() {
        val model = AppModel()
        assertSame(model.welcome, appUpdate(AppMsg.Welcome(WelcomeMsg.ShowNewModpack), model).welcome)
    }

    @Test
    fun syncDropdownProfileDataKeepsSyncedChildren() {
        val data = ProfileData()
        val model = AppModel(
            profile = ProfileModel(data = data),
            welcome = WelcomeModel(profileData = data, dropdown = WelcomeDropdownModel(profileData = data)),
            modpack = ModpackModel(modpackDropdown = ModpackDropdownModel(profileData = data)),
        )
        val next = appUpdate(AppMsg.ThemeChanged(data), model)
        assertSame(model.welcome, next.welcome)
        assertSame(model.modpack, next.modpack)
    }
}
