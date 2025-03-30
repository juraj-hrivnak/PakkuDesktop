package teksturepako.pakkupro.ui.viewmodel.state

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository

data class GitState(
    val gitFiles: List<GitFile> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val currentDiff: DiffContent? = null,
    val commitMessage: String = "",
    val repository: Repository? = null,
    val git: Git? = null,
)
{
    companion object
    {
        fun fromRepository(repository: Repository): GitState = GitState(
            repository = repository,
            git = Git(repository)
        )
    }
}

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
    val path: String,
    val status: GitChange,
    val lastModified: LocalDateTime,
    val size: Long,
    val isSelected: Boolean = false
)
{
    companion object
    {
        fun fromFileInfo(
            path: String,
            status: GitChange,
            modifiedEpochMillis: Long,
            size: Long,
            isSelected: Boolean = false
        ): GitFile = GitFile(
            path = path,
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
)
{
    data class LineNumbers(
        val old: Int?,
        val new: Int?
    )
}

enum class DiffType
{
    ADDED,
    DELETED,
    UNCHANGED,
}