/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui

import teksturepako.pakkuDesktop.app.data.ProfileData
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackDropdownModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.ProfileModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeDropdownModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import kotlin.test.Test
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
    fun confirmCloseDialogWithoutDialog() = assertNoOp(AppModel(), AppMsg.ConfirmCloseDialog)

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
