package teksturepako.pakkupro.actions

import com.github.michaelbull.result.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path

sealed interface GitError
{
    data class Output(
        val message: String,
    ) : GitError

    data class Command(
        val code: Int,
        val message: String,
    ) : GitError

    data class Execution(
        val cause: Throwable,
    ) : GitError
}

sealed interface GitEvent
{
    data class Output(
        val message: String,
    ) : GitEvent

    data class Progress(
        val operation: String,
        val current: Int,
        val total: Int? = null,
        val message: String? = null,
    ) : GitEvent
    {
        val percentage: Float
            get() = total?.let { current.toFloat() / it } ?: 0f
    }
}

suspend infix fun Git.exec(args: String): Flow<Result<GitEvent, GitError>> = withContext(Dispatchers.IO) {
    flow {
        val result = executeCommand(buildCommand(args))
        result.collect { emit(it) }
    }.flowOn(Dispatchers.IO)
}

class Git private constructor(private val repoPath: Path)
{
    fun buildCommand(args: String): List<String> =
        listOf("git") + args.split(" ")

    suspend fun executeCommand(command: List<String>): Flow<Result<GitEvent, GitError>> = withContext(Dispatchers.IO) {
        flow {
            val process = runCatching {
                ProcessBuilder(command)
                    .directory(repoPath.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()
            }.mapError {
                GitError.Execution(it)
            }.getOrElse {
                emit(Err(it))
                return@flow
            }

            merge(
                flow {
                    process.inputStream.bufferedReader().use { reader ->
                        reader.lineSequence().forEach { line ->
                            parseProgress(line)
                                ?.let { progress -> emit(Ok(progress)) }
                                ?: parseError(line)?.let { error -> emit(Err(error)) }
                                ?: emit(Ok(GitEvent.Output(line)))
                        }
                    }
                },
                flow {
                    process.errorStream.bufferedReader().use { reader ->
                        reader.lineSequence().forEach { line ->
                            parseProgress(line)
                                ?.let { progress -> emit(Ok(progress)) }
                                ?: parseError(line)?.let { error -> emit(Err(error)) }
                                ?: emit(Ok(GitEvent.Output(line)))
                        }
                    }
                },
            ).collect { emit(it) }

            val exitCode = process.waitFor()

            if (exitCode != 0)
            {
                emit(Err(GitError.Command(code = exitCode, message = "$command command failed with exit code $exitCode")))
            }
        }
    }.catch { e ->
        emit(Err(GitError.Execution(e)))
    }.flowOn(Dispatchers.IO)

    private fun parseError(line: String): GitError.Output? = if (line.startsWith("error:"))
    {
        GitError.Output(message = line)
    }
    else null

    private fun parseProgress(line: String): GitEvent.Progress?
    {
        val progressRegex = """([\w\s]+):\s+(\d+)%\s+\((\d+)/(\d+)\)""".toRegex()
        return progressRegex.find(line)?.let { match ->
            val (operation, _, current, total) = match.destructured
            GitEvent.Progress(
                operation = operation.trim(),
                current = current.toInt(),
                total = total.toInt(),
                message = line,
            )
        }
    }

    companion object
    {
        fun at(path: Path) = Git(path)
        fun at(file: File) = at(file.toPath())
        fun at(path: String) = at(Path.of(path))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend infix fun Flow<Result<GitEvent, GitError>>.andThen(
    other: Flow<Result<GitEvent, GitError>>,
): Flow<Result<GitEvent, GitError>> = withContext(Dispatchers.IO) {
    flatMapConcat { first ->
        flow {
            var shouldContinue = true
            first.onFailure {
                shouldContinue = false
                emit(Err(it))
            }
            if (shouldContinue)
            {
                other.collect { second ->
                    emit(second)
                }
            }
        }.flowOn(Dispatchers.IO)
    }
}