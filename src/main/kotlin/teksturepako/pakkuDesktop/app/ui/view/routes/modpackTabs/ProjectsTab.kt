/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes.modpackTabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.SplitLayoutState
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.ProjectDisplay
import teksturepako.pakkuDesktop.app.ui.component.modpack.project.list.ProjectsList
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.elm.animatedDividerStyle

@OptIn(FlowPreview::class)
@Composable
fun ProjectsTab(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    val filterFocusRequester = remember { FocusRequester() }
    val inspectorOpen = model.selectedProject != null
    val splitState = remember {
        SplitLayoutState(if (inspectorOpen) model.projectsSplitRatio else 1f)
    }
    // Skip persisting divider writes from the open/close animation.
    val suppressPersist = remember { mutableStateOf(false) }

    LaunchedEffect(splitState) {
        snapshotFlow { splitState.dividerPosition }
            .drop(1)
            .distinctUntilChanged()
            .debounce(200)
            .collect { ratio ->
                if (!suppressPersist.value && ratio < 0.98f) {
                    publish(ModpackMsg.ProjectsSplitRatioChanged(ratio))
                }
            }
    }

    // Local visual animation: keep ProjectsList mounted; slide the divider instead of swapping trees.
    LaunchedEffect(inspectorOpen) {
        val target = if (inspectorOpen) {
            model.projectsSplitRatio.coerceIn(0.05f, 0.95f)
        } else {
            1f
        }
        if (kotlin.math.abs(splitState.dividerPosition - target) < 0.001f) return@LaunchedEffect
        suppressPersist.value = true
        try {
            val anim = Animatable(splitState.dividerPosition)
            anim.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            ) {
                splitState.dividerPosition = value
            }
        } finally {
            suppressPersist.value = false
        }
    }

    LaunchedEffect(model.projectsSplitRatio) {
        if (!inspectorOpen || suppressPersist.value) return@LaunchedEffect
        if (kotlin.math.abs(splitState.dividerPosition - model.projectsSplitRatio) > 0.01f) {
            splitState.dividerPosition = model.projectsSplitRatio
        }
    }

    Column(Modifier.fillMaxSize()) {
        HorizontalSplitLayout(
            state = splitState,
            dividerStyle = animatedDividerStyle(),
            first = {
                ProjectsList(
                    publish = publish,
                    model = model,
                    filterFocusRequester = filterFocusRequester,
                )
            },
            second = {
                ProjectDisplay(publish, model)
            },
            modifier = Modifier.fillMaxSize(),
            firstPaneMinWidth = 280.dp,
            secondPaneMinWidth = if (inspectorOpen) 260.dp else 0.dp,
            draggableWidth = 16.dp,
        )
    }
}
