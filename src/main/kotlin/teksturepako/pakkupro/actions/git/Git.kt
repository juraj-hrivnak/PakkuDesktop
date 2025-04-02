package teksturepako.pakkupro.actions.git

import com.github.michaelbull.result.*
import io.klogging.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path

sealed interface GitError
{
    val message: String

    data class Output(
        override val message: String,
    ) : GitError

    data class Command(
        val code: Int,
        override val message: String,
    ) : GitError

    data class Execution(
        val cause: Throwable,
        override val message: String = cause.message ?: cause.stackTraceToString()
    ) : GitError
}

sealed class GitEvent
{
    data class Output(
        val message: String,
    ) : GitEvent()

    data class Progress(
        val operation: String,
        val current: Int,
        val total: Int? = null,
        val message: String? = null,
    ) : GitEvent()
    {
        val percentage: Float
            get() = total?.let { current.toFloat() / it } ?: 0f
    }

    fun output() = if (this is Output) this else null
}

suspend infix fun Git.exec(args: String): Flow<Result<GitEvent, GitError>> = withContext(Dispatchers.IO) {
    flow {
        val result = executeCommand(buildCommand(args))
        result.collect { emit(it) }
    }.flowOn(Dispatchers.IO)
}

fun gitRepoOf(path: Path) = Git(path)
fun gitRepoOf(file: File) = Git(file.toPath())
fun gitRepoOf(path: String) = Git(Path.of(path))

class Git(private val repoPath: Path)
{
    private val logger = logger(this::class)

    suspend fun buildCommand(args: String): List<String> =
        (listOf("git") + args.split(" "))
            .also { logger.info(it.joinToString(" ")) }

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
                            parseError(line)?.let { error ->
                                emit(Err(error))
                                logger.error(error.message)
                            } ?: emit(Ok(GitEvent.Output(line)))
                                .also { logger.info(line) }
                        }
                    }
                },
                flow {
                    process.errorStream.bufferedReader().use { reader ->
                        reader.lineSequence().forEach { line ->
                            parseProgress(line)
                                ?.let { progress -> emit(Ok(progress)) }
                                ?: parseError(line)?.let { error ->
                                    emit(Err(error))
                                    logger.error(error.message)
                                }
                                ?: emit(Ok(GitEvent.Output(line)))
                                    .also { logger.info(line) }
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

suspend fun Flow<Result<GitEvent, GitError>>.mapToResultMessages(): Pair<String?, String?>
{
    val resultList = this.toList()

    val successMessage = resultList
        .filter { it.isOk }
        .map { result ->
            result.fold(
                success = { event -> event.output()?.message },
                failure = { null }
            )
        }
        .joinToString("\n")
        .takeIf { it.isNotBlank() }

    val errorMessage = resultList
        .filter { it.isErr }
        .map { result ->
            result.fold(
                success = { null },
                failure = { error -> error.message }
            )
        }
        .joinToString("\n")
        .takeIf { it.isNotBlank() }

    return successMessage to errorMessage
}

suspend fun Flow<Result<GitEvent, GitError>>.output(
    success: suspend (String) -> Unit,
    failure: suspend (String) -> Unit,
    progress: (GitEvent.Progress) -> Unit = { },
)
{
    val resultList = this.toList()

    resultList
        .filter { it.isOk }
        .mapNotNull { result ->
            result.fold(
                success = { event ->
                    when (event)
                    {
                        is GitEvent.Progress ->
                        {
                            progress(event)
                            null
                        }
                        is GitEvent.Output   ->
                        {
                            event.message
                        }
                    }
                },
                failure = { null }
            )
        }
        .joinToString("\n")
        .takeIf { it.isNotBlank() }
        ?.let { success(it) }

    resultList
        .filter { it.isErr }
        .mapNotNull { result ->
            result.fold(
                success = { null },
                failure = { error -> error.message }
            )
        }
        .joinToString("\n")
        .takeIf { it.isNotBlank() }
        ?.let { failure(it) }
}