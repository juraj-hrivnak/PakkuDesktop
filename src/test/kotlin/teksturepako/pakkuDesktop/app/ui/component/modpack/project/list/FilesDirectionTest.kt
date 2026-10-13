/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.project.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.github.michaelbull.result.Ok
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling
import org.junit.Rule
import org.junit.rules.RuleChain
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectFile
import teksturepako.pakku.api.projects.ProjectType
import teksturepako.pakkuDesktop.app.ui.LocalShiftKeyState
import teksturepako.pakkuDesktop.app.ui.ShiftKeyState
import teksturepako.pakkuDesktop.app.ui.model.DiskSnapshot
import teksturepako.pakkuDesktop.app.ui.model.FilesChangeKind
import teksturepako.pakkuDesktop.app.ui.model.FilesFabAction
import teksturepako.pakkuDesktop.app.ui.model.FilesPreviewMode
import teksturepako.pakkuDesktop.app.ui.model.FilesTabModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg
import teksturepako.pakkuDesktop.app.ui.model.pathKey
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.modpackUpdate
import teksturepako.pakkuDesktop.elm.RecomposeCounter
import teksturepako.pakkuDesktop.elm.assumeAwtForComposeUiTests

class FilesDirectionTest {
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

    private fun modProject(): Project {
        val file = ProjectFile(type = "modrinth", id = "f", fileName = "foo.jar")
        return Project(
            type = ProjectType.MOD,
            slug = mutableMapOf("modrinth" to "test-mod"),
            name = mutableMapOf("en" to "Test Mod"),
            id = mutableMapOf("modrinth" to "test-mod"),
            files = mutableSetOf(file),
        )
    }

    private fun fetchLinks(): List<FilesLink> {
        val project = modProject()
        return listOf(
            FilesLink(
                path = "mods/a.jar",
                pathKey = pathKey("mods/a.jar"),
                pakku = PakkuEntry.Tracked(project, project.files.first(), emptyList(), false),
                onDisk = false,
                state = DiskState.Missing,
                change = FilesChangeKind.Fetch,
                selectionKey = "fetch:a",
            ),
            FilesLink(
                path = "mods/b.jar",
                pathKey = pathKey("mods/b.jar"),
                pakku = PakkuEntry.Tracked(project, project.files.first(), emptyList(), false),
                onDisk = true,
                state = DiskState.Untracked,
                change = FilesChangeKind.Cleanup,
                selectionKey = "cleanup:b",
            ),
        )
    }

    @Test
    fun footerFetchWording() {
        val summary = filesFooterChangeSummary(fetchLinks(), FilesPreviewMode.Fetch)
        assertTrue(summary.endsWith("→ file system"))
        assertTrue(summary.contains("1 to download"))
        assertTrue(summary.contains("1 to delete"))
    }

    @Test
    fun footerSyncWording() {
        val project = modProject()
        val links = listOf(
            FilesLink(
                path = "mods/new.jar",
                pathKey = pathKey("mods/new.jar"),
                pakku = PakkuEntry.Detected(project),
                onDisk = true,
                state = DiskState.Untracked,
                change = FilesChangeKind.Added,
                selectionKey = project.id.values.first(),
            ),
        )
        val summary = filesFooterChangeSummary(links, FilesPreviewMode.Sync)
        assertTrue(summary.endsWith("→ Pakku"))
        assertTrue(summary.contains("1 to add"))
    }

    @Test
    fun footerNoneInSync() {
        val project = modProject()
        val links = listOf(
            FilesLink(
                path = "mods/foo.jar",
                pathKey = pathKey("mods/foo.jar"),
                pakku = PakkuEntry.Tracked(project, project.files.first(), emptyList(), false),
                onDisk = true,
                state = DiskState.InSync,
                change = null,
                selectionKey = null,
            ),
        )
        assertEquals("In sync", filesFooterChangeSummary(links, FilesPreviewMode.None))
    }

    private fun filesTabModel(actionName: String? = null): FilesTabModel =
        FilesTabModel(
            lockFile = Ok(LockFile()),
            configFile = null,
            selectedProject = null,
            selectedProjectKeys = persistentSetOf(),
            lastFilesFab = null,
            filesSplitRatio = 0.5f,
            actionName = actionName,
            filesPreviewMode = FilesPreviewMode.None,
            fetchDownloads = persistentListOf(),
            fetchOverrides = persistentListOf(),
            manualOverrides = persistentListOf(),
            diskSnapshot = DiskSnapshot(),
            fetchCleanup = persistentListOf(),
            filesHashMismatchKeys = persistentSetOf(),
            syncAdded = persistentListOf(),
            syncRemoved = persistentListOf(),
            syncUpdated = persistentListOf(),
            filesFilterText = "",
            filesFilterFolders = persistentSetOf(),
            filesFilterChangeKinds = persistentSetOf(),
            wantsFocusProjectsFilter = false,
            filesCollapsedFolders = persistentSetOf(),
        )

    @Test
    fun detectFabFetchPublishesMessages() {
        val messages = mutableListOf<ModpackMsg>()
        composeRule.setContent {
            JewelTestTheme {
                Box(Modifier.fillMaxSize()) {
                    FilesFloatingActions(messages::add, filesTabModel())
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Fetch").performClick()
        composeRule.waitForIdle()
        assertEquals(
            listOf(
                ModpackMsg.FilesFabRemembered(FilesFabAction.Refresh),
                ModpackMsg.FetchDetectRequested,
            ),
            messages,
        )
    }

    @Test
    fun detectFabsDisabledWhileBusy() {
        composeRule.setContent {
            JewelTestTheme {
                Box(Modifier.fillMaxSize()) {
                    FilesFloatingActions({}, filesTabModel(actionName = "Fetching"))
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasContentDescription("Fetch")).assertIsNotEnabled()
        composeRule.onNode(hasContentDescription("Sync")).assertIsNotEnabled()
    }

    @Test
    fun footerStatusComposableFetch() {
        composeRule.setContent {
            JewelTestTheme {
                FilesFooterStatus(
                    publish = {},
                    model = filesTabModel().copy(filesPreviewMode = FilesPreviewMode.Fetch),
                    links = fetchLinks(),
                    selectableKeys = emptyList(),
                    lastClickedIndex = remember { mutableStateOf<Int?>(null) },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("→ file system", substring = true), useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun activateLastFilesFabSyncDetect() {
        val model = ModpackModel(
            lastFilesFab = FilesFabAction.Sync,
            lockFile = Ok(LockFile()),
        )
        val next = modpackUpdate(ModpackMsg.ActivateLastFilesFab, model)
        assertTrue(next.wantsSyncDetect)
    }
}
