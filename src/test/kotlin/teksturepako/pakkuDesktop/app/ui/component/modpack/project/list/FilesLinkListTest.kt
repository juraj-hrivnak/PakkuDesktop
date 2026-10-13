/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import org.junit.Rule
import org.junit.rules.RuleChain
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.LocalShiftKeyState
import teksturepako.pakkuDesktop.app.ui.ShiftKeyState
import teksturepako.pakkuDesktop.app.ui.component.tree.PathTreeSnapshot
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.pathKey
import teksturepako.pakkuDesktop.elm.RecomposeCounter
import teksturepako.pakkuDesktop.elm.assumeAwtForComposeUiTests

class FilesLinkListTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(assumeAwtForComposeUiTests()).around(composeRule)

    private var probesWereEnabled = false

    @BeforeTest
    fun enableProbes() {
        probesWereEnabled = RecomposeCounter.enabled
        RecomposeCounter.enabled = true
    }

    @AfterTest
    fun restoreProbes() {
        RecomposeCounter.enabled = probesWereEnabled
        RecomposeCounter.reset()
    }

    @Composable
    private fun JewelTestTheme(content: @Composable () -> Unit) {
        val textStyle = remember { JewelTheme.createDefaultTextStyle() }
        val theme = remember { JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle) }
        IntUiTheme(theme, ComponentStyling, swingCompatMode = false) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalShiftKeyState provides remember { ShiftKeyState() },
            ) {
                content()
            }
        }
    }

    @Composable
    private fun LinkListHost(links: List<FilesLink>, mode: FilesPreviewMode = FilesPreviewMode.Fetch) {
        val listState = rememberLazyListState()
        val tree = remember(links) { PathTreeSnapshot.of(links) { it.path } }
        var collapsedFolders by remember { mutableStateOf(emptySet<String>()) }
        val expandedFolders = remember(tree.folderIds, collapsedFolders) {
            tree.folderIds - collapsedFolders
        }
        val split = remember { mutableStateOf(0.5f) }
        val indexByKey = remember(links) { links.withIndex().associate { (i, l) -> l.pathKey to i } }
        FilesLinkList(
            links = links,
            listState = listState,
            splitRatio = split,
            onSplitRatioChange = { split.value = it },
            selectedProjectKeys = persistentSetOf(),
            focusedProjectUiKey = null,
            filesPreviewMode = mode,
            configFile = null,
            expandedFolders = expandedFolders,
            linkIndexByPathKey = indexByKey,
            lastClickedIndex = remember { mutableStateOf(null) },
            publish = { msg ->
                if (msg is ModpackMsg.FilesFolderExpansionToggled) {
                    collapsedFolders = if (msg.folderPath in collapsedFolders) {
                        collapsedFolders - msg.folderPath
                    } else {
                        collapsedFolders + msg.folderPath
                    }
                }
            },
            modifier = Modifier.height(400.dp),
        )
    }

    private fun modProject(name: String = "Test Mod", fileName: String = "foo.jar"): Project {
        val file = ProjectFile(type = "modrinth", id = "f", fileName = fileName)
        return Project(
            type = ProjectType.MOD,
            slug = mutableMapOf("modrinth" to "test-mod"),
            name = mutableMapOf("en" to name),
            id = mutableMapOf("modrinth" to "test-mod"),
            files = mutableSetOf(file),
        )
    }

    private fun trackedLink(
        project: Project,
        path: String,
        state: DiskState,
        change: FilesChangeKind? = null,
        onDisk: Boolean = state != DiskState.Missing,
        selectionKey: String? = null,
    ): FilesLink = FilesLink(
        path = path,
        pathKey = pathKey(path),
        pakku = PakkuEntry.Tracked(project, project.files.first(), emptyList(), continuation = false),
        onDisk = onDisk,
        state = state,
        change = change,
        selectionKey = selectionKey,
    )

    @Test
    fun bothSidesOnOneRow() {
        val project = modProject()
        val links = listOf(
            trackedLink(project, "mods/foo.jar", DiskState.Missing, FilesChangeKind.Fetch),
        )
        composeRule.setContent {
            JewelTestTheme { LinkListHost(links) }
        }
        composeRule.waitForIdle()
        composeRule.onNode(
            hasTestTag("FilesLinkRow") and hasAnyDescendant(hasText("Test Mod")) and
                hasAnyDescendant(hasText("foo.jar")),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun statusLetters() {
        val project = modProject(fileName = "a.jar")
        val links = listOf(
            trackedLink(project, "mods/missing.jar", DiskState.Missing),
            trackedLink(project, "mods/untracked.jar", DiskState.Untracked, onDisk = true),
            trackedLink(project, "mods/modified.jar", DiskState.Modified, onDisk = true),
        )
        composeRule.setContent {
            JewelTestTheme { LinkListHost(links, FilesPreviewMode.None) }
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("D", useUnmergedTree = true).fetchSemanticsNodes().let {
            assertTrue(it.isNotEmpty(), "expected D status letter")
        }
        composeRule.onAllNodesWithText("U", useUnmergedTree = true).fetchSemanticsNodes().let {
            assertTrue(it.isNotEmpty(), "expected U status letter")
        }
        composeRule.onAllNodesWithText("M", useUnmergedTree = true).fetchSemanticsNodes().let {
            assertTrue(it.isNotEmpty(), "expected M status letter")
        }
    }

    @Test
    fun arrowPerDirection() {
        val project = modProject()
        val links = listOf(
            FilesLink(
                path = "mods/delete-me.jar",
                pathKey = pathKey("mods/delete-me.jar"),
                pakku = null,
                onDisk = true,
                state = DiskState.Untracked,
                change = FilesChangeKind.Cleanup,
                selectionKey = "cleanup:/tmp/x",
            ),
            trackedLink(
                project,
                "mods/new.jar",
                DiskState.Untracked,
                change = FilesChangeKind.Added,
                onDisk = true,
                selectionKey = project.id.values.first(),
            ),
        )
        composeRule.setContent {
            JewelTestTheme { LinkListHost(links, FilesPreviewMode.Sync) }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasContentDescription(FilesDirectionToDiskDescription)).assertExists()
        composeRule.onNode(hasContentDescription(FilesDirectionToPakkuDescription)).assertExists()
    }

    @Test
    fun badgeSide() {
        val project = modProject()
        val links = listOf(
            trackedLink(
                project,
                "mods/fetch.jar",
                DiskState.Missing,
                change = FilesChangeKind.Fetch,
                selectionKey = "fetch:x",
            ),
            trackedLink(
                project,
                "mods/remove.jar",
                DiskState.Modified,
                change = FilesChangeKind.Removed,
                onDisk = true,
                selectionKey = "ui-key",
            ),
        )
        composeRule.setContent {
            JewelTestTheme { LinkListHost(links) }
        }
        composeRule.waitForIdle()
        composeRule.onNode(
            hasTestTag("FilesLinkRow") and hasAnyDescendant(hasText("fetch.jar")) and
                hasAnyDescendant(hasText("Download")),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNode(
            hasTestTag("FilesLinkRow") and hasAnyDescendant(hasText("remove.jar")) and
                hasAnyDescendant(hasText("Remove")) and hasAnyDescendant(hasText("Test Mod")),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun collapseHidesChildRows() {
        val project = modProject()
        val links = listOf(
            trackedLink(project, "mods/one.jar", DiskState.InSync, onDisk = true),
            trackedLink(project, "mods/two.jar", DiskState.InSync, onDisk = true),
        )
        composeRule.setContent {
            JewelTestTheme { LinkListHost(links, FilesPreviewMode.None) }
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("one.jar").fetchSemanticsNodes()
        composeRule.onAllNodesWithContentDescription("Collapse", substring = true).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("one.jar").fetchSemanticsNodes().let {
            assertTrue(it.isEmpty(), "collapsed folder should hide file rows")
        }
    }

    @Test
    fun virtualizationLimitsComposedRows() {
        val project = modProject(name = "Bulk", fileName = "item.jar")
        val links = (1..1_000).map { i ->
            trackedLink(
                project,
                "mods/item-$i.jar",
                DiskState.InSync,
                onDisk = true,
            )
        }
        RecomposeCounter.reset()
        composeRule.setContent {
            JewelTestTheme { LinkListHost(links, FilesPreviewMode.None) }
        }
        composeRule.waitForIdle()
        val probeCount = RecomposeCounter["FilesLinkRow"]
        val rowTags = composeRule.onAllNodesWithTag("FilesLinkRow").fetchSemanticsNodes().size
        assertTrue(probeCount <= 40, "FilesLinkRow probe=$probeCount (recorded cap 40)")
        assertTrue(rowTags <= 40, "mounted FilesLinkRow nodes=$rowTags")
    }
}
