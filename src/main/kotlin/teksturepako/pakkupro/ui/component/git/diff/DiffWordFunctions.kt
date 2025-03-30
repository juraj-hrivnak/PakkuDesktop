package teksturepako.pakkupro.ui.component.git.diff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import teksturepako.pakkupro.ui.viewmodel.state.DiffLine
import teksturepako.pakkupro.ui.viewmodel.state.DiffType

private data class Change(
    val startIndex: Int,
    val endIndex: Int
)

private fun findWordDifferences(str1: String, str2: String): List<Change> {
    val changes = mutableListOf<Change>()

    // Find the common prefix
    var i = 0
    while (i < str1.length && i < str2.length && str1[i] == str2[i]) {
        i++
    }

    // Find the common suffix
    var j = str1.length - 1
    var k = str2.length - 1
    while (j > i && k > i && str1[j] == str2[k]) {
        j--
        k--
    }

    // If there's a difference, mark it
    if (i <= j) {
        changes.add(Change(i, j + 1))
    }

    return changes
}

fun highlightChanges(line: DiffLine, otherLines: List<DiffLine>): AnnotatedString = buildAnnotatedString {
    if (line.type == DiffType.UNCHANGED)
    {
        append(line.content)
        return@buildAnnotatedString
    }

    // Keep any leading whitespace
    val leadingWhitespace = line.content.takeWhile { it.isWhitespace() }

    if (leadingWhitespace.isNotEmpty())
    {
        append(leadingWhitespace)
    }

    val content = line.content.substring(leadingWhitespace.length)

    when (line.type)
    {
        DiffType.DELETED ->
        {
            // Find the most similar added line
            val addedLine = otherLines
                .filter { it.type == DiffType.ADDED }
                .minByOrNull { levenshteinDistance(content, it.content) }

            if (addedLine != null)
            {
                val changes = findWordDifferences(content, addedLine.content)
                var lastIndex = 0

                for (change in changes)
                {
                    // Append unchanged part
                    append(content.substring(lastIndex, change.startIndex))

                    // Append changed part with highlight
                    withStyle(
                        SpanStyle(
                        color = Color(0xFFFF5555), background = Color(0x33FF5555)
                    )
                    ) {
                        append(content.substring(change.startIndex, change.endIndex))
                    }

                    lastIndex = change.endIndex
                }

                // Append remaining unchanged part
                if (lastIndex < content.length)
                {
                    append(content.substring(lastIndex))
                }
            }
            else
            {
                withStyle(SpanStyle(color = Color(0xFFFF5555))) {
                    append(content)
                }
            }

            return@buildAnnotatedString
        }
        DiffType.ADDED   ->
        {
            // Find the most similar deleted line
            val deletedLine = otherLines
                .filter { it.type == DiffType.DELETED }
                .minByOrNull { levenshteinDistance(content, it.content) }

            if (deletedLine != null)
            {
                val changes = findWordDifferences(content, deletedLine.content)
                var lastIndex = 0

                for (change in changes)
                {
                    // Append unchanged part
                    append(content.substring(lastIndex, change.startIndex))

                    // Append changed part with highlight
                    withStyle(
                        SpanStyle(
                        color = Color(0xFF50FA7B), background = Color(0x3350FA7B)
                    )
                    ) {
                        append(content.substring(change.startIndex, change.endIndex))
                    }

                    lastIndex = change.endIndex
                }

                // Append remaining unchanged part
                if (lastIndex < content.length)
                {
                    append(content.substring(lastIndex))
                }
            }
            else
            {
                withStyle(SpanStyle(color = Color(0xFF50FA7B))) {
                    append(content)
                }
            }

            return@buildAnnotatedString
        }
        else -> append(content)
    }
}

private fun levenshteinDistance(str1: String, str2: String): Int {
    val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }

    for (i in 0..str1.length) {
        for (j in 0..str2.length) {
            when {
                i == 0 -> dp[i][j] = j
                j == 0 -> dp[i][j] = i
                else -> {
                    dp[i][j] = minOf(
                        dp[i - 1][j - 1] + if (str1[i - 1] == str2[j - 1]) 0 else 1,
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1
                    )
                }
            }
        }
    }

    return dp[str1.length][str2.length]
}
