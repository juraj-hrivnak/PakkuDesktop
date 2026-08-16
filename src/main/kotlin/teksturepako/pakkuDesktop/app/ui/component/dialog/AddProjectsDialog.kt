/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.github.michaelbull.result.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkuDesktop.app.actions.AdditionEntry
import teksturepako.pakkuDesktop.app.actions.AdditionPlan
import teksturepako.pakkuDesktop.app.actions.buildAdditionPlan
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.component.ActionErrorContent
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectRef
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import teksturepako.pakkuDesktop.pkui.component.dialogConfirmCancelKeys

private sealed interface AddStep {
    data object Input : AddStep
    data class Resolving(val status: String) : AddStep
    data class Confirm(val plan: AdditionPlan) : AddStep
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddProjectsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    model: ModpackModel,
    onConfirmPlan: (AdditionPlan) -> Unit,
) {
    if (!visible) return

    val lockFile = model.lockFile?.get()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    val inputFocus = remember { FocusRequester() }
    var step by remember { mutableStateOf<AddStep>(AddStep.Input) }
    var accepted by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var resolveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(step) {
        if (step is AddStep.Input) {
            kotlinx.coroutines.yield()
            runCatching { inputFocus.requestFocus() }
        }
    }

    fun dismiss() {
        resolveJob?.cancel()
        resolveJob = null
        step = AddStep.Input
        accepted = emptySet()
        onDismiss()
    }

    fun goBackToInput() {
        resolveJob?.cancel()
        resolveJob = null
        step = AddStep.Input
        accepted = emptySet()
    }

    fun submitQuery() {
        val query = textFieldState.text.toString()
        if (query.isBlank() || lockFile == null) return
        resolveJob?.cancel()
        resolveJob = scope.launch {
            step = AddStep.Resolving("Resolving…")
            val plan = withContext(Dispatchers.IO) {
                buildAdditionPlan(
                    lockFile = lockFile,
                    query = query,
                    onProgress = { status ->
                        withContext(Dispatchers.Main) { step = AddStep.Resolving(status) }
                    },
                )
            }
            accepted = plan.entries.mapIndexedNotNull { i, e -> if (e.isRecommended) i else null }.toSet()
            step = AddStep.Confirm(plan)
        }
    }

    fun confirm() {
        when (val current = step) {
            is AddStep.Confirm -> {
                if (current.plan.isEmpty) {
                    goBackToInput()
                    return
                }
                val selected = current.plan.entries.filterIndexed { i, _ -> i in accepted }
                if (selected.isEmpty()) return
                onConfirmPlan(AdditionPlan(selected, current.plan.messages))
                dismiss()
            }
            is AddStep.Input -> submitQuery()
            else -> Unit
        }
    }

    fun onEscape() {
        when (step) {
            is AddStep.Input -> dismiss()
            else -> goBackToInput()
        }
    }

    Dialog(onDismissRequest = { dismiss() }) {
        ContentBox(
            Modifier
                .dialogConfirmCancelKeys(onDismiss = { onEscape() }, onConfirm = { confirm() })
                .widthIn(min = 360.dp, max = 520.dp),
        ) {
            Column(
                Modifier
                    .padding(PakkuDesktopConstants.commonPaddingSize)
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header("Add projects")

                when (val s = step) {
                    is AddStep.Input -> {
                        Text(
                            "Project names, slugs, or URLs — comma-separated.",
                            color = JewelTheme.contentColor.copy(alpha = 0.65f),
                        )
                        TextField(
                            state = textFieldState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(inputFocus),
                            placeholder = { Text("sodium, iris, gh:owner/repo") },
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DefaultButton(onClick = { submitQuery() }) { Text("Add") }
                            OutlinedButton(onClick = { dismiss() }) { Text("Cancel") }
                        }
                    }

                    is AddStep.Resolving -> {
                        Text(s.status, color = JewelTheme.contentColor.copy(alpha = 0.75f))
                    }

                    is AddStep.Confirm -> {
                        val onlyBack = s.plan.isEmpty
                        s.plan.messages.forEach { ActionErrorContent(it) }
                        if (!s.plan.isEmpty) {
                            GroupHeader("Projects")
                            VerticallyScrollableContainer(
                                Modifier.fillMaxWidth().heightIn(max = 280.dp),
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(end = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    s.plan.entries.forEachIndexed { index, entry ->
                                        YnPrompt(
                                            question = { AdditionQuestion(entry) },
                                            yes = index in accepted,
                                            onYes = { accepted = accepted + index },
                                            onNo = { accepted = accepted - index },
                                            recommended = entry.isRecommended,
                                            warnings = entry.warnings,
                                        )
                                    }
                                }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onlyBack) {
                                DefaultButton(onClick = { goBackToInput() }) { Text("Back") }
                                OutlinedButton(onClick = { dismiss() }) { Text("Cancel") }
                            } else {
                                DefaultButton(
                                    onClick = { confirm() },
                                    enabled = accepted.isNotEmpty(),
                                ) { Text("Add") }
                                OutlinedButton(onClick = { goBackToInput() }) { Text("Back") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdditionQuestion(entry: AdditionEntry) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        if (entry.replacing != null) {
            Text("Do you want to replace ", fontSize = 13.sp)
            ProjectRef(entry.replacing)
            Text(" with ", fontSize = 13.sp)
            ProjectRef(entry.project)
            Text("?", fontSize = 13.sp)
        } else {
            Text("Do you want to add ", fontSize = 13.sp)
            ProjectRef(entry.project)
            Text("?", fontSize = 13.sp)
        }
    }
}

/**
 * Recommended answer is [DefaultButton] (filled), the other is [OutlinedButton].
 */
@Composable
internal fun YnPrompt(
    question: @Composable () -> Unit,
    yes: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit,
    recommended: Boolean,
    warnings: List<ActionError> = emptyList(),
    indent: Boolean = false,
) {
    Column(
        Modifier.padding(start = if (indent) 12.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        question()
        warnings.forEach { ActionErrorContent(it, compact = true) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (recommended) {
                DefaultButton(onClick = onYes) { Text("Yes") }
                OutlinedButton(onClick = onNo) { Text("No") }
            } else {
                OutlinedButton(onClick = onYes) { Text("Yes") }
                DefaultButton(onClick = onNo) { Text("No") }
            }
        }
    }
}
