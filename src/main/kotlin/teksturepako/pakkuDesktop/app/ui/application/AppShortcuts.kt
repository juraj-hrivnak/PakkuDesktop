/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.application

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.github.michaelbull.result.getError
import teksturepako.pakku.api.actions.errors.FileNotFound
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.AppScreen
import teksturepako.pakkuDesktop.app.ui.model.CloseDialogRequest
import teksturepako.pakkuDesktop.app.ui.model.GitDropdownMsg
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.SelectedTab
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg

/**
 * Cross-platform in-window accelerators for Pakku Desktop (Tao — no AWT MenuBar).
 * Returns true when the event was handled.
 */
fun handleAppShortcut(event: KeyEvent, model: AppModel, publish: (AppMsg) -> Unit): Boolean
{
    if (event.type != KeyEventType.KeyDown) return false

    val onProjectsTab = model.screen == AppScreen.Modpack &&
        model.modpack.selectedTab == SelectedTab.PROJECTS
    val filterFocused = model.modpack.projectsFilterFocused
    val accelerator = event.isCtrlPressed || event.isMetaPressed

    // Esc: topmost modal first (window preview runs before dialog content).
    if (!accelerator && event.key == Key.Escape)
    {
        return handleEscape(model, publish)
    }

    // Projects-tab keys that do not require Ctrl/Cmd
    if (onProjectsTab && !accelerator)
    {
        when (event.key)
        {
            Key.Delete, Key.Backspace ->
            {
                if (!filterFocused &&
                    !model.hasModalOverlay() &&
                    model.modpack.selectedPakkuIds.isNotEmpty() &&
                    model.modpack.actionName == null
                ) {
                    publish(AppMsg.Modpack(ModpackMsg.ShowRemoveDialog))
                    return true
                }
            }
            Key.Enter, Key.NumPadEnter ->
            {
                if (!filterFocused &&
                    !model.hasModalOverlay() &&
                    (model.modpack.selectedPakkuIds.isNotEmpty() || model.modpack.selectedProject != null)
                ) {
                    publish(AppMsg.Modpack(ModpackMsg.OpenDetailRequested))
                    return true
                }
            }
            else -> Unit
        }
    }

    if (!accelerator) return false

    return when (event.key)
    {
        Key.O ->
        {
            publish(AppMsg.OpenDirectoryPickerRequested)
            true
        }
        Key.Comma ->
        {
            when (model.screen)
            {
                AppScreen.Welcome,
                AppScreen.Activation -> publish(AppMsg.Welcome(WelcomeMsg.ShowSettings))
                AppScreen.Modpack -> publish(AppMsg.Modpack(ModpackMsg.ShowSettings))
            }
            true
        }
        Key.W ->
        {
            if (model.screen == AppScreen.Modpack)
            {
                publish(AppMsg.Modpack(ModpackMsg.CloseRequested()))
                true
            }
            else false
        }
        Key.E ->
        {
            if (model.screen == AppScreen.Modpack && model.modpack.actionName == null)
            {
                publish(AppMsg.Modpack(ModpackMsg.ExportRequested))
                true
            }
            else false
        }
        Key.A ->
        {
            if (onProjectsTab && !filterFocused && !model.hasModalOverlay())
            {
                publish(AppMsg.Modpack(ModpackMsg.SelectAllFilteredRequested))
                true
            }
            else false
        }
        Key.F ->
        {
            when
            {
                // Ctrl/Cmd+Shift+F — Fetch projects
                event.isShiftPressed && model.screen == AppScreen.Modpack && model.modpack.actionName == null ->
                {
                    publish(AppMsg.Modpack(ModpackMsg.FetchRequested))
                    true
                }
                // Ctrl/Cmd+F — focus projects filter
                onProjectsTab ->
                {
                    publish(AppMsg.Modpack(ModpackMsg.FocusProjectsFilterRequested))
                    true
                }
                else -> false
            }
        }
        Key.Q ->
        {
            publish(AppMsg.RequestCloseDialog(CloseDialogRequest.Quit()))
            true
        }
        else -> false
    }
}

/**
 * Dismiss the highest-priority open overlay, then the project inspector.
 * Order matches [AppComponent] urgency: close/exit before errors before app dialogs.
 */
private fun handleEscape(model: AppModel, publish: (AppMsg) -> Unit): Boolean = when
{
    model.closeDialog != null ->
    {
        publish(AppMsg.DismissCloseDialog)
        true
    }
    model.lockErrorVisible() ->
    {
        publish(AppMsg.Modpack(ModpackMsg.DismissLockError))
        true
    }
    model.configErrorVisible() ->
    {
        publish(AppMsg.Modpack(ModpackMsg.DismissConfigError))
        true
    }
    model.showNewModpack ->
    {
        publish(AppMsg.HideNewModpack)
        true
    }
    model.showCloneDialog ->
    {
        publish(AppMsg.HideCloneDialog)
        true
    }
    model.showSettings ->
    {
        publish(AppMsg.HideSettings)
        true
    }
    model.modpack.gitDropdown.pushDialogVisible ->
    {
        publish(AppMsg.Modpack(ModpackMsg.GitDropdown(GitDropdownMsg.HidePushDialog)))
        true
    }
    model.modpack.removeDialogVisible ->
    {
        publish(AppMsg.Modpack(ModpackMsg.HideRemoveDialog))
        true
    }
    model.modpack.addDialogVisible ->
    {
        publish(AppMsg.Modpack(ModpackMsg.HideAddDialog))
        true
    }
    model.screen == AppScreen.Modpack && model.modpack.selectedProject != null ->
    {
        publish(AppMsg.Modpack(ModpackMsg.ProjectSelected(null)))
        true
    }
    else -> false
}

private fun AppModel.hasModalOverlay(): Boolean =
    closeDialog != null ||
        lockErrorVisible() ||
        configErrorVisible() ||
        showNewModpack ||
        showCloneDialog ||
        showSettings ||
        modpack.gitDropdown.pushDialogVisible ||
        modpack.removeDialogVisible ||
        modpack.addDialogVisible

private fun AppModel.lockErrorVisible(): Boolean
{
    val error = modpack.lockFile?.getError()?.takeUnless { it is FileNotFound }
    return error != null && !modpack.lockErrorDismissed
}

private fun AppModel.configErrorVisible(): Boolean
{
    val error = modpack.configFile?.getError()?.takeUnless { it is FileNotFound }
    return error != null && !modpack.configErrorDismissed
}
