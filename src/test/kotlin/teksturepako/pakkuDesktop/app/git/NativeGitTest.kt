/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.git

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class NativeGitTest {
    private lateinit var repo: File

    private fun git(vararg args: String): String = runBlocking {
        val out = NativeGit.exec(repo, *args)
        check(out.ok) { "git ${args.joinToString(" ")} failed: ${out.text}" }
        out.text
    }

    private fun write(path: String, text: String) = File(repo, path).apply { parentFile.mkdirs() }.writeText(text)

    @BeforeTest
    fun setUp() {
        repo = Files.createTempDirectory("pakku-native-git").toFile()
        git("init", "-q", "-b", "main")
        git("config", "user.email", "test@example.com")
        git("config", "user.name", "Test")
        git("config", "commit.gpgsign", "false")
        git("config", "core.autocrlf", "false")
        write("a.txt", "a1\n")
        write("b.txt", "b1\n")
        git("add", "-A")
        git("commit", "-q", "-m", "initial")
    }

    @AfterTest
    fun tearDown() {
        repo.deleteRecursively()
    }

    @Test
    fun commitPathsCommitsOnlyThoseAndKeepsOtherStaging() = runBlocking {
        write("a.txt", "a2\n")
        write("b.txt", "b2\n")
        write("new.txt", "n\n")
        git("add", "b.txt")

        val out = NativeGit.commitPaths(repo, "partial", listOf("a.txt", "new.txt"), amend = false)
        assertTrue(out.ok, out.text)

        assertEquals("a.txt\nnew.txt", git("show", "--name-only", "--format=", "HEAD").trim())
        // b.txt stays staged, a.txt / new.txt are clean.
        assertEquals("M  b.txt", git("status", "--porcelain").trim())
    }

    @Test
    fun commitPathsAmendKeepsPreviousChanges() = runBlocking {
        write("a.txt", "a2\n")
        assertTrue(NativeGit.commitPaths(repo, "first", listOf("a.txt"), amend = false).ok)
        write("b.txt", "b2\n")
        assertTrue(NativeGit.commitPaths(repo, "amended", listOf("b.txt"), amend = true).ok)

        assertEquals("2", git("rev-list", "--count", "HEAD").trim())
        assertEquals("a.txt\nb.txt", git("show", "--name-only", "--format=", "HEAD").trim())
        assertEquals("amended", git("log", "-1", "--format=%s").trim())
    }

    @Test
    fun rollbackRestoresTrackedAndUnstagesAdded() = runBlocking {
        write("a.txt", "changed\n")
        File(repo, "b.txt").delete()
        write("added.txt", "x\n")
        git("add", "added.txt")

        val out = NativeGit.rollback(repo, listOf("a.txt", "b.txt"), listOf("added.txt"), deleteAdded = false)
        assertTrue(out.ok, out.text)

        assertEquals("a1\n", File(repo, "a.txt").readText())
        assertTrue(File(repo, "b.txt").exists())
        assertEquals("?? added.txt", git("status", "--porcelain").trim())
    }

    @Test
    fun ignoreAppendsAnchoredEntriesOnce() {
        write(".gitignore", "build")
        NativeGit.ignore(repo, listOf("logs/latest.log"))
        NativeGit.ignore(repo, listOf("logs/latest.log"))
        assertEquals("build\n/logs/latest.log\n", File(repo, ".gitignore").readText())
    }

    @Test
    fun buildStateWithoutUpstreamListsUnpushedCommits() = runBlocking {
        val state = NativeGit.buildState(repo, GitState())
        assertNull(state.upstream)
        assertEquals(0, state.incomingCount)
        assertEquals(1, state.outgoingCommits.size)
        assertEquals("initial", state.headMessage)
        assertEquals("main", state.currentBranch?.name)
        assertTrue(state.stashes.isEmpty())

        write("a.txt", "stash me\n")
        git("stash", "push", "-m", "wip")
        val stashed = NativeGit.buildState(repo, state)
        assertEquals(listOf("stash@{0}"), stashed.stashes.map { it.ref })
        assertTrue(stashed.stashes.single().message.endsWith("wip"))
        assertFalse(stashed.gitFiles.any { it.path == "a.txt" })
    }

    @Test
    fun computeDiffParsesASmallEdit() = runBlocking {
        write("a.txt", "a2\n")
        val file = NativeGit.buildState(repo, GitState()).gitFiles.single { it.path == "a.txt" }
        val diff = NativeGit.computeDiff(repo, file)
        val texts = diff.hunks.flatMap { it.lines.map { line -> line.content } }
        assertTrue(texts.any { it == "a2" })
    }

    @Test
    fun computeDiffDoesNotLoadAnOversizedFile() = runBlocking {
        val big = "x".repeat(600_000)
        write("big.txt", big)
        git("add", "big.txt")
        git("commit", "-q", "-m", "big")
        write("big.txt", "y".repeat(600_000))
        val file = NativeGit.buildState(repo, GitState()).gitFiles.single { it.path == "big.txt" }
        val diff = NativeGit.computeDiff(repo, file)
        val text = diff.hunks.flatMap { it.lines }.joinToString("\n") { it.content }
        assertTrue(text.contains("too large"), text)
        assertFalse(text.contains("yyyy"))
    }

    @Test
    fun computeDiffDoesNotLoadADeletedOversizedFile() = runBlocking {
        write("big.txt", "x".repeat(600_000))
        git("add", "big.txt")
        git("commit", "-q", "-m", "big")
        File(repo, "big.txt").delete()
        val file = NativeGit.buildState(repo, GitState()).gitFiles.single { it.path == "big.txt" }
        val diff = NativeGit.computeDiff(repo, file)
        val text = diff.hunks.flatMap { it.lines }.joinToString("\n") { it.content }
        assertTrue(text.contains("too large"), text)
        assertFalse(text.contains("xxxx"))
    }
}
