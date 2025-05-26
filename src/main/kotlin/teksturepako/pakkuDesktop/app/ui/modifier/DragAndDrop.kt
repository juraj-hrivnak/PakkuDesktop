package teksturepako.pakkuDesktop.app.ui.modifier

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.theme.tooltipStyle
import java.net.URI

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DragAndDropTarget(showTargetBorder: MutableState<Boolean>) = remember {
    object : DragAndDropTarget
    {
        override fun onStarted(event: DragAndDropEvent)
        {
            showTargetBorder.value = true
        }

        override fun onEnded(event: DragAndDropEvent)
        {
            showTargetBorder.value = false
        }

        override fun onDrop(event: DragAndDropEvent): Boolean
        {
            println("Action at the target: ${event.action}")

            if (event.dragData() !is DragData.FilesList) return false

            val pathUri = (event.dragData() as DragData.FilesList)
                .readFiles()
                .first()
                .let(::URI)

            println(pathUri.path)

            return true
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.allowDragAndDrop(): Modifier
{
    val showTargetBorder = remember { mutableStateOf(false) }
    val dragAndDropTarget = DragAndDropTarget(showTargetBorder)

    return this
        .then(
            if (showTargetBorder.value) Modifier.border(
                width = 3.dp,
                color = JewelTheme.globalColors.outlines.focused,
                shape = RoundedCornerShape(JewelTheme.tooltipStyle.metrics.cornerSize),
            )
            else Modifier
        )
        .dragAndDropTarget(
            shouldStartDragAndDrop = { true },
            target = dragAndDropTarget
        )
}

