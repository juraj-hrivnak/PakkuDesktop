package teksturepako.pakkupro.ui.component.git.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkupro.ui.viewmodel.state.DiffHunk
import teksturepako.pakkupro.ui.viewmodel.state.DiffLine
import teksturepako.pakkupro.ui.viewmodel.state.DiffType

@Composable
fun DiffLineLeft(
    line: DiffLine,
    hunk: DiffHunk,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(
                when (line.type)
                {
                    DiffType.DELETED -> Color(0xFF40313D).copy(alpha = 0.3f)
                    else             -> Color.Transparent
                }
            )
    ) {
        DisableSelection {
            LineNumber(number = line.number.old)
        }

        Text(
            text = when (line.type)
            {
                DiffType.ADDED -> AnnotatedString("")
                else           -> highlightChanges(line, hunk.lines)
            },
            style = JewelTheme.editorTextStyle,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun DiffLineRight(
    line: DiffLine,
    hunk: DiffHunk,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(
                when (line.type)
                {
                    DiffType.ADDED -> Color(0xFF294436).copy(alpha = 0.3f)
                    else           -> Color.Transparent
                }
            )
    ) {
        DisableSelection {
            LineNumber(number = line.number.new)
        }

        Text(
            text = when (line.type)
            {
                DiffType.DELETED -> AnnotatedString("")
                else             -> highlightChanges(line, hunk.lines)
            },
            style = JewelTheme.editorTextStyle,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
