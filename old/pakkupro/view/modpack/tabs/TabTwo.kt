package pakkupro.view.modpack.tabs

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.code.highlighting.NoOpCodeHighlighter
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.markdown.standalone.ProvideMarkdownStyling
import org.jetbrains.jewel.intui.markdown.standalone.dark
import org.jetbrains.jewel.intui.markdown.standalone.light
import org.jetbrains.jewel.intui.markdown.standalone.styling.dark
import org.jetbrains.jewel.intui.markdown.standalone.styling.extensions.github.alerts.dark
import org.jetbrains.jewel.intui.markdown.standalone.styling.extensions.github.alerts.light
import org.jetbrains.jewel.intui.markdown.standalone.styling.light
import org.jetbrains.jewel.markdown.LazyMarkdown
import org.jetbrains.jewel.markdown.MarkdownBlock
import org.jetbrains.jewel.markdown.extension.autolink.AutolinkProcessorExtension
import org.jetbrains.jewel.markdown.extensions.github.alerts.AlertStyling
import org.jetbrains.jewel.markdown.extensions.github.alerts.GitHubAlertProcessorExtension
import org.jetbrains.jewel.markdown.extensions.github.alerts.GitHubAlertRendererExtension
import org.jetbrains.jewel.markdown.processing.MarkdownProcessor
import org.jetbrains.jewel.markdown.rendering.MarkdownBlockRenderer
import org.jetbrains.jewel.markdown.rendering.MarkdownStyling
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.component.scrollbarContentSafePadding
import pakkupro.viewmodel.MainViewModel
import java.awt.Desktop
import java.net.URI

@OptIn(ExperimentalJewelApi::class)
@Composable
@Preview
fun tabTwo(coroutineScope: CoroutineScope)
{
    MainViewModel.updatePakkuApi()

    val isDark = JewelTheme.isDark

    val markdownStyling = remember(isDark) { if (isDark) MarkdownStyling.dark() else MarkdownStyling.light() }

    var markdownBlocks by remember { mutableStateOf(emptyList<MarkdownBlock>()) }
    val extensions = remember { listOf(GitHubAlertProcessorExtension, AutolinkProcessorExtension) }

    val processor = remember { MarkdownProcessor(extensions, editorMode = true) }

    LaunchedEffect(MarkdownReadme) {
        markdownBlocks =  processor.processMarkdownDocument(MarkdownReadme)
    }

    val blockRenderer = remember(markdownStyling, extensions) {
        if (isDark) {
            MarkdownBlockRenderer.dark(
                styling = markdownStyling,
                rendererExtensions = listOf(GitHubAlertRendererExtension(AlertStyling.dark(), markdownStyling)),
            )
        } else {
            MarkdownBlockRenderer.light(
                styling = markdownStyling,
                rendererExtensions = listOf(GitHubAlertRendererExtension(AlertStyling.light(), markdownStyling)),
            )
        }
    }

    ProvideMarkdownStyling(markdownStyling, blockRenderer, NoOpCodeHighlighter) {
        val lazyListState = rememberLazyListState()
        VerticallyScrollableContainer(lazyListState) {
            LazyMarkdown(
                markdownBlocks = markdownBlocks,
                contentPadding =
                PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp + scrollbarContentSafePadding(), bottom = 8.dp),
                state = lazyListState,
                selectable = true,
                onUrlClick = { url -> Desktop.getDesktop().browse(URI.create(url)) },
            )
        }
    }
}
