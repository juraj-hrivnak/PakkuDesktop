/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TriStateCheckbox
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.colorPalette
import org.jetbrains.jewel.ui.theme.treeStyle
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants

val TreeRowMinHeight = 30.dp

private val TreeIndentPerLevel = 20.dp
private val TreeChevronSlot = 20.dp
private val TreeChevronGap = 4.dp
private val TreeCheckSlot = 32.dp

/** Status letter (A/M/D) or folder icon: same width keeps file and folder names aligned. */
private val TreeStatusSlot = 20.dp
private val TreeSelectionStripeWidth = 3.dp

enum class TreeStatus
{
    Added, Modified, Deleted, Untracked,
}

fun TreeStatus.letter(): String = when (this)
{
    TreeStatus.Added     -> "A"
    TreeStatus.Modified  -> "M"
    TreeStatus.Deleted   -> "D"
    TreeStatus.Untracked -> "U"
}

@Composable
fun TreeStatus.color(): Color
{
    val p = JewelTheme.colorPalette
    val fallback = JewelTheme.contentColor
    fun pick(ramps: List<Color>): Color =
        ramps.getOrNull((ramps.lastIndex * 5 / 10).coerceIn(0, ramps.lastIndex)) ?: fallback
    return when (this)
    {
        TreeStatus.Added     -> pick(p.green)
        TreeStatus.Modified  -> pick(p.yellow)
        TreeStatus.Deleted   -> pick(p.red)
        TreeStatus.Untracked -> JewelTheme.globalColors.text.disabled
    }
}

/** When set, folder tri-state uses these counts instead of [PathTreeRow.Folder] totals (Files tab). */
data class TreeFolderSelectionScope(
    val itemCount: Int,
    val selectedCount: Int,
)

@Composable
fun TreeFolderRow(
    row: PathTreeRow.Folder,
    onToggleExpanded: () -> Unit,
    onToggleSelected: (() -> Unit)?,
    modifier: Modifier = Modifier,
    selectionScope: TreeFolderSelectionScope? = null,
)
{
    val interaction = remember(row.fullPath) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val subtreeItemCount = selectionScope?.itemCount ?: row.subtreeItemCount
    val selectedInSubtree = selectionScope?.selectedCount ?: row.selectedInSubtree
    val folderCheckState = when
    {
        subtreeItemCount == 0              -> ToggleableState.Off
        selectedInSubtree == 0             -> ToggleableState.Off
        selectedInSubtree == subtreeItemCount -> ToggleableState.On
        else                               -> ToggleableState.Indeterminate
    }
    val stripe = Color.Transparent
    val bg = treeRowBackground(
        selectedForDiff = false,
        hovered = hovered,
        folderBand = true,
    )
    val treeChevron = JewelTheme.treeStyle.icons.chevron(isExpanded = row.expanded, isSelected = false)
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = TreeRowMinHeight).background(bg).hoverable(interaction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TreeSelectionStripe(stripe, selectedForDiff = false)
        Row(
            modifier = Modifier.weight(1f).padding(vertical = 4.dp, horizontal = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(TreeIndentPerLevel * row.depth))
            Box(
                modifier = Modifier.size(TreeChevronSlot).clickable(
                        indication = null,
                        interactionSource = remember(row.fullPath) { MutableInteractionSource() },
                    ) {
                        onToggleExpanded()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    key = treeChevron,
                    contentDescription = if (row.expanded) "Collapse" else "Expand",
                    tint = JewelTheme.contentColor.copy(alpha = 0.65f),
                    hints = arrayOf(),
                    modifier = Modifier.size(13.dp),
                )
            }
            Spacer(Modifier.width(TreeChevronGap))
            Box(
                modifier = Modifier.width(TreeCheckSlot).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (onToggleSelected != null)
                {
                    TriStateCheckbox(
                        state = folderCheckState,
                        onClick = onToggleSelected,
                    )
                }
            }
            Box(
                modifier = Modifier.width(TreeStatusSlot).fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    key = AllIconsKeys.Nodes.Folder,
                    contentDescription = null,
                    tint = JewelTheme.contentColor.copy(alpha = 0.85f),
                    hints = arrayOf(),
                    modifier = Modifier.size(17.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).clickable(
                        indication = null,
                        interactionSource = remember(row.fullPath) { MutableInteractionSource() },
                    ) {
                        onToggleExpanded()
                    },
            ) {
                Text(
                    text = row.displayName,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = JewelTheme.contentColor,
                    style = JewelTheme.defaultTextStyle.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
fun TreeItemRow(
    depth: Int,
    label: String,
    status: TreeStatus?,
    checked: Boolean?,
    onCheckedChange: (Boolean) -> Unit,
    highlighted: Boolean,
    dimmed: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionKey: String = label,
    /** When false, focus/selection uses row background only (Files tab split row). */
    paintHighlightOnStripe: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
)
{
    val interaction = remember(interactionKey) { MutableInteractionSource() }
    val diffClickInteraction = remember(interactionKey, "diffOpen") { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val statusLetter = status?.letter()
    val statusColor = status?.color()
    val stripe = if (highlighted) PakkuDesktopConstants.highlightColor else Color.Transparent
    val bg = treeRowBackground(
        selectedForDiff = highlighted,
        hovered = hovered,
        folderBand = false,
    )
    val nameAlpha = when
    {
        dimmed      -> 0.55f
        highlighted -> 1f
        else        -> 0.92f
    }

    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = TreeRowMinHeight).background(bg).hoverable(interaction),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TreeSelectionStripe(
            stripe,
            selectedForDiff = highlighted,
            paintHighlight = paintHighlightOnStripe,
        )
        Row(
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(TreeIndentPerLevel * depth))
            Spacer(Modifier.width(TreeChevronSlot))
            Spacer(Modifier.width(TreeChevronGap))
            Box(
                modifier = Modifier.width(TreeCheckSlot).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (checked != null)
                {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                    )
                }
            }
            // Transparent overlay: Jewel Text can consume pointer input, so parent Row.clickable
            // often never fires on short names (e.g. pakku.json) — overlay receives hits first.
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.width(TreeStatusSlot).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (statusLetter != null && statusColor != null)
                        {
                            Text(
                                text = statusLetter,
                                color = statusColor,
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = JewelTheme.contentColor.copy(alpha = nameAlpha),
                        style = JewelTheme.defaultTextStyle.copy(
                            fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal,
                        ),
                    )
                    trailing()
                }
                Box(
                    Modifier.matchParentSize().clickable(
                            interactionSource = diffClickInteraction,
                            indication = null,
                            onClick = onClick,
                        ),
                )
            }
        }
    }
}

@Composable
private fun TreeSelectionStripe(
    color: Color,
    selectedForDiff: Boolean,
    paintHighlight: Boolean = true,
)
{
    Spacer(
        Modifier.width(TreeSelectionStripeWidth).fillMaxHeight().background(
                when {
                    paintHighlight && selectedForDiff -> PakkuDesktopConstants.highlightColor
                    paintHighlight && color != Color.Transparent -> color
                    else -> Color.Transparent
                },
            ),
    )
}

@Composable
@ReadOnlyComposable
private fun treeRowBackground(
    selectedForDiff: Boolean,
    hovered: Boolean,
    folderBand: Boolean,
): Color
{
    val content = JewelTheme.contentColor
    val panel = JewelTheme.globalColors.panelBackground
    val selectedFill = PakkuDesktopConstants.highlightColor.copy(alpha = if (JewelTheme.isDark) 0.14f else 0.10f)
    val hoverFill = content.copy(alpha = if (JewelTheme.isDark) 0.07f else 0.06f)
    val folderFill = lerp(panel, content, if (JewelTheme.isDark) 0.06f else 0.04f)
    return when
    {
        selectedForDiff && hovered -> lerp(selectedFill, hoverFill, 0.35f)
        selectedForDiff            -> selectedFill
        hovered                    -> hoverFill
        folderBand                 -> folderFill
        else                       -> Color.Transparent
    }
}
