package teksturepako.pakkupro.ui.viewmodel.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.jewel.foundation.theme.JewelTheme

sealed interface GitChange
{
    val path: String
    val displayName: String get() = path.substringAfterLast('/')

    data class Untracked(override val path: String) : GitChange
    data class Modified(override val path: String) : GitChange
    data class Added(override val path: String) : GitChange
    data class Deleted(override val path: String) : GitChange
}

data class GitFile(
    val status: GitChange,
    val lastModified: LocalDateTime,
    val size: Long,
    val isSelected: Boolean = false
) {
    companion object {
        fun fromFileInfo(
            path: String,
            status: GitChange,
            modifiedEpochMillis: Long,
            size: Long,
            isSelected: Boolean = false
        ): GitFile = GitFile(
            status = status,
            lastModified = Instant
                .fromEpochMilliseconds(modifiedEpochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
            size = size,
            isSelected = isSelected
        )
    }
}

data class DiffContent(
    val oldPath: String?,
    val newPath: String,
    val hunks: List<DiffHunk>
)

data class DiffHunk(
    val header: String,
    val lines: List<DiffLine>
)

data class DiffLine(
    val number: LineNumbers,
    val content: String,
    val type: DiffType
) {
    data class LineNumbers(
        val old: Int?,
        val new: Int?
    )
}

enum class DiffType {
    ADDED,
    DELETED,
    UNCHANGED,
    MODIFIED;

    @Composable
    fun backgroundColor() = when(this) {
        ADDED -> Color(0xff2a8441)
        DELETED -> Color(0xffaf5c5c)
        MODIFIED -> Color.Transparent
        UNCHANGED -> Color.Transparent
    }

    @Composable
    fun textColor() = when(this) {
        ADDED -> Color(0xFF50FA7B)
        DELETED -> Color(0xFFFF5555)
        MODIFIED -> JewelTheme.contentColor
        UNCHANGED -> JewelTheme.contentColor
    }
}