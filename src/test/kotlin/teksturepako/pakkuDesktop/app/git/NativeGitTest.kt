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
    fun initRepositorySetsDefaultBranch() = runBlocking {
        val dir = Files.createTempDirectory("pakku-git-init").toFile()
        try {
            val out = NativeGit.initRepository(dir, "trunk")
            assertTrue(out.ok, out.text)
            val state = NativeGit.readState(dir)
            assertEquals(true, state.isRepository)
            assertEquals("trunk", state.currentBranch?.name)
        } finally {
            dir.deleteRecursively()
        }
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
    fun readStateWithoutUpstreamListsUnpushedCommits() = runBlocking {
        val state = NativeGit.readState(repo)
        assertNull(state.upstream)
        assertEquals(0, state.incomingCount)
        assertEquals(1, state.outgoingCommits.size)
        assertEquals("initial", state.headMessage)
        assertEquals("main", state.currentBranch?.name)
        assertTrue(state.stashes.isEmpty())

        write("a.txt", "stash me\n")
        git("stash", "push", "-m", "wip")
        val stashed = NativeGit.readState(repo)
        assertEquals(listOf("stash@{0}"), stashed.stashes.map { it.ref })
        assertTrue(stashed.stashes.single().message.endsWith("wip"))
        assertFalse(stashed.gitFiles.any { it.path == "a.txt" })
    }

    @Test
    fun readStateTracksUpstreamAheadAndBehind() = runBlocking {
        val clone = Files.createTempDirectory("pakku-native-git-clone").toFile()
        try {
            git("clone", "-q", repo.absolutePath, clone.absolutePath)
            fun cloneGit(vararg args: String) = runBlocking {
                val out = NativeGit.exec(clone, *args)
                check(out.ok) { "git ${args.joinToString(" ")} failed: ${out.text}" }
            }
            cloneGit("config", "user.email", "test@example.com")
            cloneGit("config", "user.name", "Test")
            cloneGit("config", "commit.gpgsign", "false")

            File(clone, "local.txt").writeText("l\n")
            cloneGit("add", "local.txt")
            cloneGit("commit", "-q", "-m", "local")
            write("remote.txt", "r\n")
            git("add", "remote.txt")
            git("commit", "-q", "-m", "remote")
            cloneGit("fetch", "-q")

            val state = NativeGit.readState(clone)
            assertEquals("origin/main", state.upstream)
            assertEquals(1, state.incomingCount)
            assertEquals(listOf("local"), state.outgoingCommits.map { it.message })
            assertTrue(state.branches.any { it.isRemote && it.name == "origin/main" })
            assertFalse(state.branches.any { it.name == "origin/HEAD" })
            assertEquals(listOf("origin"), state.remotes)
        } finally {
            clone.deleteRecursively()
        }
    }

    @Test
    fun readStateKeepsUnusualPathsAndRenames() = runBlocking {
        write("my mods/Šťastný mod.jar", "x\n")
        git("mv", "b.txt", "renamed b.txt")
        val paths = NativeGit.readState(repo).gitFiles.associate { it.path to it.status }
        assertTrue(paths["my mods/Šťastný mod.jar"] is GitChange.Untracked, paths.toString())
        assertTrue(paths["renamed b.txt"] is GitChange.Modified, paths.toString())
        assertFalse("b.txt" in paths, paths.toString())
    }

    @Test
    fun readStateDetachedHeadHasNoCurrentBranch() = runBlocking {
        git("checkout", "-q", "--detach")
        val state = NativeGit.readState(repo)
        assertNull(state.currentBranch)
        assertTrue(state.branches.any { it.name == "main" && !it.isCurrent })
    }

    @Test
    fun readStateDoesNotRewriteTheIndex() = runBlocking {
        val index = File(repo, ".git/index")
        // Touch a tracked file without changing it, so a locking status would refresh the index.
        File(repo, "a.txt").setLastModified(System.currentTimeMillis() + 10_000)
        val before = index.lastModified()
        NativeGit.readState(repo)
        assertEquals(before, index.lastModified())
    }

    @Test
    fun computeDiffParsesASmallEdit() = runBlocking {
        write("a.txt", "a2\n")
        val file = NativeGit.readState(repo).gitFiles.single { it.path == "a.txt" }
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
        val file = NativeGit.readState(repo).gitFiles.single { it.path == "big.txt" }
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
        val file = NativeGit.readState(repo).gitFiles.single { it.path == "big.txt" }
        val diff = NativeGit.computeDiff(repo, file)
        val text = diff.hunks.flatMap { it.lines }.joinToString("\n") { it.content }
        assertTrue(text.contains("too large"), text)
        assertFalse(text.contains("xxxx"))
    }

    @Test
    fun logTopologyKeepsANegativeAuthorTimezone() = runBlocking {
        write("z.txt", "z\n")
        git("add", "z.txt")
        val env = mapOf("GIT_AUTHOR_DATE" to "1577952245 -0500")
        val out = NativeGit.exec(repo, "commit", "-q", "-m", "zoned", env = env)
        assertTrue(out.ok, out.text)
        val hash = git("rev-parse", "HEAD").trim()
        val topology = NativeGit.readLogTopology(repo, hash)
        val index = topology.indexOf(hash)
        assertNotNull(index)
        // 1577952245 is 2020-01-02 03:04:05 -0500. %at keeps it; the old ISO parser dropped it.
        assertEquals(1_577_952_245L, topology.authorTimes[index])
        assertEquals("Test", topology.authors[index])
    }

    @Test
    fun readStatePeelsTagsAndKeepsLocalBranchesWithSlashes() = runBlocking {
        git("tag", "-a", "v1", "-m", "annotated")
        git("branch", "feature/x")
        val state = NativeGit.readState(repo)
        val head = state.headHash
        assertNotNull(head)
        assertEquals(head, state.refs.single { it.name == "v1" && it.type == GitRefType.Tag }.hash)
        assertEquals(GitRefType.LocalBranch, state.refs.single { it.name == "feature/x" }.type)
        assertFalse(state.branches.any { it.name == "feature/x" && it.isRemote })
    }

    @Test
    fun readStateTagCreatedAtIsPopulated() {
        runBlocking {
            git("tag", "v1.0")
            git("tag", "-a", "v2.0", "-m", "annotated")
            val state = NativeGit.readState(repo)
            val tags = state.refs.filter { it.type == GitRefType.Tag }
            assertEquals(2, tags.size)
            val v1 = tags.single { it.name == "v1.0" }
            val v2 = tags.single { it.name == "v2.0" }
            assertNotNull(v1.createdAt, "Lightweight tag should have createdAt from commit date")
            assertNotNull(v2.createdAt, "Annotated tag should have createdAt from tagger date")
        }
    }

    @Test
    fun subjectsAndCommitDiffComeFromTheFirstParent() = runBlocking {
        write("a.txt", "a2\n")
        git("add", "a.txt")
        git("commit", "-q", "-m", "edit a")
        val hash = git("rev-parse", "HEAD").trim()
        assertEquals("edit a", NativeGit.readSubjects(repo, listOf(hash))[hash])
        val details = NativeGit.readCommitDetails(repo, hash)
        assertNotNull(details)
        assertEquals(1, details.parents.size)
        val change = details.changes.single { it.path == "a.txt" }
        assertTrue(change.status is GitChange.Modified)
        val diff = NativeGit.computeCommitDiff(repo, hash, details.parents.first(), change)
        val texts = diff.hunks.flatMap { it.lines.map { line -> line.content } }
        assertTrue(texts.any { it == "a2" }, texts.toString())
    }

    @Test
    fun nameStatusParsesRenames() {
        val changes = NativeGit.parseNameStatus("R100\u0000old.txt\u0000new.txt\u0000M\u0000kept.txt\u0000")
        val renamed = changes.single { it.path == "new.txt" }
        assertEquals("old.txt", renamed.oldPath)
        assertTrue(renamed.status is GitChange.Modified)
        assertEquals("kept.txt", changes.single { it.oldPath == null }.path)
    }
}
