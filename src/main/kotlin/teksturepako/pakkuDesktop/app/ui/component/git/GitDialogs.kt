/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.git

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.git.GitChange
import teksturepako.pakkuDesktop.app.git.GitCommit
import teksturepako.pakkuDesktop.app.git.GitDialog
import teksturepako.pakkuDesktop.app.git.GitFile
import teksturepako.pakkuDesktop.app.git.GitOp
import teksturepako.pakkuDesktop.app.git.GitRef
import teksturepako.pakkuDesktop.app.git.GitRefType
import teksturepako.pakkuDesktop.app.git.GitResetMode
import teksturepako.pakkuDesktop.app.git.GitState
import teksturepako.pakkuDesktop.app.git.toTreeStatus
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.tree.color
import teksturepako.pakkuDesktop.app.ui.component.tree.letter
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.gitBusy
import teksturepako.pakkuDesktop.pkui.component.PkUiChip
import teksturepako.pakkuDesktop.pkui.component.PkUiDialog
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogButtons
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogNotice
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSectionLabel
import teksturepako.pakkuDesktop.pkui.component.PkUiDialogSize
import teksturepako.pakkuDesktop.pkui.component.PkUiNoticeKind

/** The open [ModpackModel.gitDialog], if any. */
@Composable
fun GitDialogs(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    val dialog = model.gitDialog ?: return
    val hide = { publish(ModpackMsg.HideGitDialog) }
    val run = { op: GitOp -> publish(ModpackMsg.GitOpRequested(op)) }
    val git = model.git
    val busy = model.gitBusy()

    when (dialog) {
        GitDialog.Commit -> GitCommitDialog(git, hide, run, publish)
        GitDialog.Update -> GitUpdateDialog(git, hide, run)
        GitDialog.Push -> GitPushDialog(git, hide, run)
        GitDialog.Rollback -> GitRollbackDialog(git, hide, run)
        is GitDialog.NewBranch -> GitNewBranchDialog(git, dialog, hide, run)
        is GitDialog.DeleteBranch -> GitDeleteBranchDialog(dialog, hide, run)
        GitDialog.Stash -> GitStashDialog(git, hide, run)
        GitDialog.Unstash -> GitUnstashDialog(git, busy, hide, run)
        is GitDialog.Reset -> GitResetDialog(dialog, hide, run)
        is GitDialog.NewTag -> GitNewTagDialog(git, dialog, hide, run)
        is GitDialog.DeleteTag -> GitDeleteTagDialog(dialog, hide, run)
    }
}

// -- Commit --

@Composable
private fun GitCommitDialog(
    git: GitState,
    hide: () -> Unit,
    run: (GitOp) -> Unit,
    publish: (ModpackMsg) -> Unit,
) {
    val files = git.selectedFiles.sortedBy { it.path }
    val message = rememberTextFieldState(git.commitMessage)
    val messageFocus = remember { FocusRequester() }
    var amend by remember { mutableStateOf(false) }
    val canAmend = git.headMessage.isNotEmpty()

    LaunchedEffect(message) {
        snapshotFlow { message.text.toString() }
            .distinctUntilChanged()
            .collect { publish(ModpackMsg.GitCommitMessageChanged(it)) }
    }

    val canCommit = message.text.isNotBlank() && (files.isNotEmpty() || amend)
    val canPush = git.defaultRemote != null
    fun commit(push: Boolean) {
        if (!canCommit) return
        run(GitOp.Commit(message.text.toString().trim(), files.map { it.path }, amend, pushAfter = push && canPush))
    }

    PkUiDialog(
        onDismiss = hide,
        title = git.currentBranch?.let { "Commit to ${it.name}" } ?: "Commit",
        icon = AllIconsKeys.Actions.Commit,
        subtitle = when (files.size) {
            0 -> if (amend) "Amend the last commit's message" else "No files selected"
            1 -> "1 file"
            else -> "${files.size} files"
        },
        size = PkUiDialogSize.Large,
        onConfirm = { commit(push = false) },
        confirmNeedsAccelerator = true,
        initialFocus = messageFocus,
        footerStart = {
            Text("Ctrl+Enter to commit", color = JewelTheme.contentColor.copy(alpha = 0.5f))
        },
        footer = {
            PkUiDialogButtons(
                primaryLabel = if (amend) "Amend Commit" else "Commit",
                onPrimary = { commit(push = false) },
                primaryEnabled = canCommit,
                onSecondary = hide,
                extra = {
                    OutlinedButton(onClick = { commit(push = true) }, enabled = canCommit && canPush) {
                        Text(if (amend) "Amend and Push…" else "Commit and Push…")
                    }
                },
            )
        },
    ) {
        TextArea(
            message,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = 220.dp)
                .focusRequester(messageFocus),
            placeholder = { Text("Commit message") },
        )
        CheckboxRow(
            "Amend commit",
            checked = amend,
            enabled = canAmend,
            onCheckedChange = { on ->
                amend = on
                if (on && message.text.isBlank()) message.setTextAndPlaceCursorAtEnd(git.headMessage)
            },
        )
        if (files.isEmpty() && !amend) {
            PkUiDialogNotice("Select files in the Source Control tab to include them in the commit.")
        }
        if (files.isNotEmpty()) {
            PkUiDialogSectionLabel("Changes", icon = AllIconsKeys.Vcs.Changelist, trailing = files.size.toString())
            GitFileList(files)
        }
    }
}

// -- Update Project --

@Composable
private fun GitUpdateDialog(git: GitState, hide: () -> Unit, run: (GitOp) -> Unit) {
    var rebase by remember { mutableStateOf(false) }
    val upstream = git.upstream
    val update = { if (upstream != null) run(GitOp.Pull(rebase)) }

    PkUiDialog(
        onDismiss = hide,
        title = "Update Project",
        icon = AllIconsKeys.Actions.CheckOut,
        subtitle = upstream?.let { "${git.currentBranch?.name ?: "HEAD"} ← $it" },
        size = PkUiDialogSize.Small,
        onConfirm = update,
        footerStart = if (git.remotes.isNotEmpty()) {
            {
                OutlinedButton(onClick = { run(GitOp.Fetch) }) { Text("Fetch Only") }
            }
        } else null,
        footer = {
            PkUiDialogButtons(
                primaryLabel = "Update",
                onPrimary = update,
                primaryEnabled = upstream != null,
                onSecondary = hide,
            )
        },
    ) {
        if (upstream == null) {
            PkUiDialogNotice(
                "The current branch has no upstream. Push it first, or check out a remote branch.",
                kind = PkUiNoticeKind.Warning,
            )
        } else {
            Text(
                when (git.incomingCount) {
                    0 -> "No incoming commits as of the last fetch. Updating fetches first."
                    1 -> "1 incoming commit as of the last fetch."
                    else -> "${git.incomingCount} incoming commits as of the last fetch."
                },
                color = JewelTheme.contentColor.copy(alpha = 0.7f),
            )
            PkUiDialogSectionLabel("Update type", icon = AllIconsKeys.Vcs.Merge)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RadioButtonRow("Merge incoming changes into the current branch", selected = !rebase, onClick = { rebase = false })
                RadioButtonRow("Rebase the current branch on top of incoming changes", selected = rebase, onClick = { rebase = true })
            }
            if (git.gitFiles.any { it.status !is GitChange.Untracked }) {
                PkUiDialogNotice("You have uncommitted changes. Git stops if the update would overwrite them; stash them first.")
            }
        }
    }
}

// -- Push --

@Composable
private fun GitPushDialog(git: GitState, hide: () -> Unit, run: (GitOp) -> Unit) {
    var force by remember { mutableStateOf(false) }
    var pushTags by remember { mutableStateOf(false) }
    val commits = git.outgoingCommits.toList()
    val branch = git.currentBranch?.name ?: "HEAD"
    val remote = git.defaultRemote
    val target = git.upstream ?: remote?.let { "$it/$branch" }
    val canPush = remote != null
    val push = { if (canPush) run(GitOp.Push(force, pushTags)) }
    val localTags = git.refs.filter { it.type == GitRefType.Tag }.sortedBy { it.name }

    PkUiDialog(
        onDismiss = hide,
        title = when (commits.size) {
            0 -> "Push"
            1 -> "Push 1 commit"
            else -> "Push ${commits.size} commits"
        },
        icon = AllIconsKeys.Vcs.Push,
        subtitle = target?.let { "$branch → $it" },
        size = PkUiDialogSize.Large,
        onConfirm = push,
        footerStart = {
            Column {
                CheckboxRow("Force push (with lease)", checked = force, onCheckedChange = { force = it })
                CheckboxRow("Push tags", checked = pushTags, onCheckedChange = { pushTags = it })
            }
        },
        footer = {
            PkUiDialogButtons(
                primaryLabel = if (force) "Force Push" else "Push",
                onPrimary = push,
                primaryEnabled = canPush,
                onSecondary = hide,
            )
        },
    ) {
        when {
            remote == null -> PkUiDialogNotice("No remote is configured for this repository.", kind = PkUiNoticeKind.Error)
            git.upstream == null -> PkUiDialogNotice("'$branch' will be pushed to '$remote' and set as its upstream.")
        }
        if (git.incomingCount > 0 && !force) {
            PkUiDialogNotice(
                "The remote has ${git.incomingCount} commit(s) you don't have. Update the project before pushing.",
                kind = PkUiNoticeKind.Warning,
            )
        }
        if (commits.isNotEmpty()) {
            PkUiDialogSectionLabel("Commits", icon = AllIconsKeys.Actions.Commit, trailing = commits.size.toString())
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                commits.forEach { GitCommitRow(it) }
            }
        }
        if (localTags.isNotEmpty()) {
            PkUiDialogSectionLabel("Tags", icon = AllIconsKeys.Nodes.Tag, trailing = localTags.size.toString())
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                localTags.forEach { ref ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            AllIconsKeys.Nodes.Tag,
                            contentDescription = null,
                            tint = JewelTheme.contentColor.copy(alpha = 0.55f),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(ref.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }
            Text(
                "Tags will be pushed when \"Push tags\" is checked.",
                color = JewelTheme.contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (commits.isEmpty() && localTags.isEmpty()) {
            Text("Nothing to push — the branch matches the remote.", color = JewelTheme.contentColor.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun GitCommitRow(commit: GitCommit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AllIconsKeys.Vcs.CommitNode,
            contentDescription = null,
            tint = PakkuDesktopConstants.highlightColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            commit.hash,
            style = JewelTheme.consoleTextStyle,
            color = JewelTheme.contentColor.copy(alpha = 0.55f),
        )
        Text(commit.message, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

// -- Rollback --

@Composable
private fun GitRollbackDialog(git: GitState, hide: () -> Unit, run: (GitOp) -> Unit) {
    val selected = git.selectedFiles.filterNot { it.status is GitChange.Untracked }
    val files = selected.ifEmpty { git.gitFiles.filterNot { it.status is GitChange.Untracked } }.sortedBy { it.path }
    val added = files.filter { it.status is GitChange.Added }
    val skippedUnversioned = git.selectedFiles.count { it.status is GitChange.Untracked }
    var deleteAdded by remember { mutableStateOf(false) }
    val rollback = {
        if (files.isNotEmpty()) {
            run(
                GitOp.Rollback(
                    trackedPaths = (files - added.toSet()).map { it.path },
                    addedPaths = added.map { it.path },
                    deleteAdded = deleteAdded,
                ),
            )
        }
    }

    PkUiDialog(
        onDismiss = hide,
        title = "Rollback changes",
        icon = AllIconsKeys.Actions.Rollback,
        subtitle = when (files.size) {
            1 -> "1 file"
            else -> "${files.size} files"
        },
        size = PkUiDialogSize.Medium,
        footer = {
            PkUiDialogButtons(
                primaryLabel = "Rollback",
                onPrimary = rollback,
                primaryEnabled = files.isNotEmpty(),
                onSecondary = hide,
            )
        },
    ) {
        PkUiDialogNotice("Local changes to these files will be lost.", kind = PkUiNoticeKind.Warning)
        if (skippedUnversioned > 0) {
            PkUiDialogNotice("$skippedUnversioned unversioned file(s) in the selection are not affected.")
        }
        if (added.isNotEmpty()) {
            CheckboxRow("Delete local copies of added files", checked = deleteAdded, onCheckedChange = { deleteAdded = it })
        }
        GitFileList(files)
    }
}

// -- Branches --

private val INVALID_BRANCH_CHARS = Regex("""[\s~^:?*\[\\]""")

private fun branchNameError(name: String, existing: Set<String>, kind: String = "branch"): String? = when {
    name.isEmpty() -> null
    name in existing -> "A $kind named '$name' already exists."
    INVALID_BRANCH_CHARS.containsMatchIn(name) || ".." in name || "@{" in name ||
        name.startsWith("-") || name.startsWith("/") || name.endsWith("/") ||
        name.endsWith(".") || name.endsWith(".lock") || "//" in name ->
        "'$name' is not a valid $kind name."
    else -> null
}

@Composable
private fun GitNewBranchDialog(git: GitState, dialog: GitDialog.NewBranch, hide: () -> Unit, run: (GitOp) -> Unit) {
    val name = rememberTextFieldState(
        dialog.startPoint?.takeIf { it.isRemote }?.name?.substringAfter('/').orEmpty(),
    )
    val nameFocus = remember { FocusRequester() }
    var checkout by remember { mutableStateOf(true) }
    val trimmed = name.text.toString().trim()
    val existing = remember(git.branches) { git.branches.filterNot { it.isRemote }.map { it.name }.toSet() }
    val error = branchNameError(trimmed, existing)
    val canCreate = trimmed.isNotEmpty() && error == null
    val start = dialog.startRevision ?: dialog.startPoint?.name
    val from = dialog.startRevision?.take(8) ?: dialog.startPoint?.name ?: git.currentBranch?.name ?: "HEAD"
    val create = {
        if (canCreate) run(GitOp.CreateBranch(trimmed, start, checkout))
    }

    PkUiDialog(
        onDismiss = hide,
        title = "Create new branch",
        icon = AllIconsKeys.Vcs.Branch,
        subtitle = "from $from",
        size = PkUiDialogSize.Small,
        onConfirm = create,
        initialFocus = nameFocus,
        footer = {
            PkUiDialogButtons(
                primaryLabel = "Create",
                onPrimary = create,
                primaryEnabled = canCreate,
                onSecondary = hide,
            )
        },
    ) {
        TextField(
            state = name,
            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
            placeholder = { Text("feature/my-change") },
        )
        error?.let { PkUiDialogNotice(it, kind = PkUiNoticeKind.Error) }
        CheckboxRow("Checkout branch", checked = checkout, onCheckedChange = { checkout = it })
    }
}

@Composable
private fun GitDeleteBranchDialog(dialog: GitDialog.DeleteBranch, hide: () -> Unit, run: (GitOp) -> Unit) {
    var force by remember { mutableStateOf(false) }

    PkUiDialog(
        onDismiss = hide,
        title = "Delete branch '${dialog.branch.name}'?",
        icon = AllIconsKeys.General.WarningDialog,
        size = PkUiDialogSize.Small,
        footer = {
            OutlinedButton(onClick = { run(GitOp.DeleteBranch(dialog.branch, force)) }) { Text("Delete") }
            DefaultButton(onClick = hide) { Text("Cancel") }
        },
    ) {
        Text(
            "The local branch is deleted. Its remote branch, if any, is kept.",
            color = JewelTheme.contentColor.copy(alpha = 0.75f),
        )
        CheckboxRow("Delete even if not fully merged", checked = force, onCheckedChange = { force = it })
    }
}

// -- Stash --

@Composable
private fun GitStashDialog(git: GitState, hide: () -> Unit, run: (GitOp) -> Unit) {
    val message = rememberTextFieldState()
    val messageFocus = remember { FocusRequester() }
    val hasUnversioned = git.gitFiles.any { it.status is GitChange.Untracked }
    var includeUntracked by remember { mutableStateOf(false) }
    val stash = { run(GitOp.Stash(message.text.toString(), includeUntracked)) }

    PkUiDialog(
        onDismiss = hide,
        title = "Stash changes",
        icon = AllIconsKeys.Vcs.ShelveSilent,
        subtitle = git.currentBranch?.name?.let { "on $it" },
        size = PkUiDialogSize.Small,
        onConfirm = stash,
        initialFocus = messageFocus,
        footer = {
            PkUiDialogButtons(primaryLabel = "Stash", onPrimary = stash, onSecondary = hide)
        },
    ) {
        TextField(
            state = message,
            modifier = Modifier.fillMaxWidth().focusRequester(messageFocus),
            placeholder = { Text("Message (optional)") },
        )
        if (hasUnversioned) {
            CheckboxRow("Include unversioned files", checked = includeUntracked, onCheckedChange = { includeUntracked = it })
        }
        PkUiDialogNotice("Stashes every local change, not only the selected files.")
    }
}

@Composable
private fun GitUnstashDialog(git: GitState, busy: Boolean, hide: () -> Unit, run: (GitOp) -> Unit) {
    val stashes = git.stashes
    var selectedRef by remember { mutableStateOf(stashes.firstOrNull()?.ref) }
    val selected = stashes.firstOrNull { it.ref == selectedRef } ?: stashes.firstOrNull()
    var pop by remember { mutableStateOf(true) }
    val apply = { if (selected != null && !busy) run(GitOp.Unstash(selected, pop)) }

    PkUiDialog(
        onDismiss = hide,
        title = "Unstash changes",
        icon = AllIconsKeys.Vcs.Unshelve,
        subtitle = when (stashes.size) {
            0 -> "No stashes"
            1 -> "1 stash"
            else -> "${stashes.size} stashes"
        },
        size = PkUiDialogSize.Medium,
        onConfirm = apply,
        footerStart = {
            OutlinedButton(
                onClick = { if (selected != null) run(GitOp.DropStash(selected)) },
                enabled = selected != null && !busy,
            ) { Text("Drop") }
        },
        footer = {
            PkUiDialogButtons(
                primaryLabel = if (pop) "Pop" else "Apply",
                onPrimary = apply,
                primaryEnabled = selected != null && !busy,
                onSecondary = hide,
            )
        },
    ) {
        if (stashes.isEmpty()) {
            Text("There are no stashes.", color = JewelTheme.contentColor.copy(alpha = 0.7f))
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                stashes.forEach { stash ->
                    val isSelected = stash == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) PakkuDesktopConstants.highlightColor.copy(alpha = 0.16f)
                                else Color.Transparent,
                            )
                            .clickable { selectedRef = stash.ref }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(AllIconsKeys.Vcs.ShelveSilent, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(
                            stash.message,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        )
                        PkUiChip(stash.ref, muted = !isSelected)
                    }
                }
            }
            CheckboxRow("Remove the stash after applying (pop)", checked = pop, onCheckedChange = { pop = it })
        }
    }
}

// -- Log --

@Composable
private fun GitResetDialog(dialog: GitDialog.Reset, hide: () -> Unit, run: (GitOp) -> Unit) {
    var mode by remember { mutableStateOf(GitResetMode.Mixed) }
    val confirm = { run(GitOp.Reset(dialog.hash, mode)) }
    PkUiDialog(
        onDismiss = hide,
        title = "Reset current branch to here",
        icon = AllIconsKeys.Actions.Rollback,
        subtitle = dialog.hash.take(8),
        size = PkUiDialogSize.Small,
        onConfirm = confirm,
        footer = {
            PkUiDialogButtons(primaryLabel = "Reset", onPrimary = confirm, onSecondary = hide)
        },
    ) {
        RadioButtonRow("Soft — keep all changes staged", selected = mode == GitResetMode.Soft, onClick = { mode = GitResetMode.Soft })
        RadioButtonRow("Mixed — keep changes, unstage them", selected = mode == GitResetMode.Mixed, onClick = { mode = GitResetMode.Mixed })
        RadioButtonRow("Hard — discard all local changes", selected = mode == GitResetMode.Hard, onClick = { mode = GitResetMode.Hard })
        RadioButtonRow("Keep — discard only changes to tracked files that differ", selected = mode == GitResetMode.Keep, onClick = { mode = GitResetMode.Keep })
        if (mode == GitResetMode.Hard) {
            PkUiDialogNotice("Hard reset deletes uncommitted work in this branch.", kind = PkUiNoticeKind.Warning)
        }
    }
}

@Composable
private fun GitNewTagDialog(git: GitState, dialog: GitDialog.NewTag, hide: () -> Unit, run: (GitOp) -> Unit) {
    val name = rememberTextFieldState("")
    val nameFocus = remember { FocusRequester() }
    val trimmed = name.text.toString().trim()
    val existing = remember(git.refs) { git.refs.filter { it.type == GitRefType.Tag }.map { it.name }.toSet() }
    val error = branchNameError(trimmed, existing, kind = "tag")
    val canCreate = trimmed.isNotEmpty() && error == null
    val create = { if (canCreate) run(GitOp.CreateTag(trimmed, dialog.hash)) }
    PkUiDialog(
        onDismiss = hide,
        title = "Create tag",
        icon = AllIconsKeys.Nodes.Tag,
        subtitle = "at ${dialog.hash.take(8)}",
        size = PkUiDialogSize.Small,
        onConfirm = create,
        initialFocus = nameFocus,
        footer = {
            PkUiDialogButtons(
                primaryLabel = "Create",
                onPrimary = create,
                primaryEnabled = canCreate,
                onSecondary = hide,
            )
        },
    ) {
        TextField(
            state = name,
            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
            placeholder = { Text("v1.0") },
        )
        if (error != null) PkUiDialogNotice(error, kind = PkUiNoticeKind.Error)
    }
}

@Composable
private fun GitDeleteTagDialog(dialog: GitDialog.DeleteTag, hide: () -> Unit, run: (GitOp) -> Unit) {
    val confirm = { run(GitOp.DeleteTag(dialog.name)) }
    PkUiDialog(
        onDismiss = hide,
        title = "Delete tag",
        icon = AllIconsKeys.General.Delete,
        subtitle = dialog.name,
        size = PkUiDialogSize.Small,
        onConfirm = confirm,
        footer = {
            PkUiDialogButtons(primaryLabel = "Delete", onPrimary = confirm, onSecondary = hide)
        },
    ) {
        PkUiDialogNotice("This will delete the tag '${dialog.name}'.", kind = PkUiNoticeKind.Warning)
    }
}

// -- Shared --

/** Compact read-only file list: status letter, file name, muted folder. */
@Composable
private fun GitFileList(files: List<GitFile>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        files.forEach { file ->
            val status = file.status.toTreeStatus()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(14.dp), contentAlignment = Alignment.Center) {
                    Text(status.letter(), color = status.color(), fontWeight = FontWeight.SemiBold)
                }
                Text(file.status.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val folder = file.path.substringBeforeLast('/', missingDelimiterValue = "")
                if (folder.isNotEmpty()) {
                    Text(
                        folder,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                        color = JewelTheme.contentColor.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}
