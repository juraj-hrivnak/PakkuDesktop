/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.view.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.app.ui.PakkuDesktopIcons
import teksturepako.pakkuDesktop.app.ui.application.appName
import teksturepako.pakkuDesktop.app.ui.application.titlebar.LocalTitleBarStartSlotMaxWidth
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TitleBarStartRow
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TITLE_BAR_BASE_HEIGHT_DP
import teksturepako.pakkuDesktop.app.ui.application.titlebar.TITLE_BAR_BORDER_HEIGHT_DP
import teksturepako.pakkuDesktop.app.ui.application.titlebar.titleBarLabelMaxWidth
import teksturepako.pakkuDesktop.app.ui.component.FadeIn
import teksturepako.pakkuDesktop.app.ui.component.dropdown.WelcomeViewDropdown
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.component.text.Header
import teksturepako.pakkuDesktop.app.ui.component.welcome.RecentModpackCard
import teksturepako.pakkuDesktop.app.ui.driver.LocalPickDirectory
import teksturepako.pakkuDesktop.app.ui.model.AppModel
import teksturepako.pakkuDesktop.app.ui.model.AppMsg
import teksturepako.pakkuDesktop.app.ui.model.WelcomeModel
import teksturepako.pakkuDesktop.app.ui.model.WelcomeMsg
import teksturepako.pakkuDesktop.app.ui.modifier.allowDragAndDrop
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight
import teksturepako.pakkuDesktop.pkui.component.ContentBox
import java.nio.file.Files
import kotlin.math.roundToInt

@Composable
fun WelcomeTitleBarStart(
    publish: (WelcomeMsg) -> Unit,
    model: WelcomeModel,
    displayName: String,
) {
    TitleBarStartRow {
        Text(
            displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(
                    max = titleBarLabelMaxWidth(LocalTitleBarStartSlotMaxWidth.current, fraction = 0.45f),
                ),
        )
        WelcomeViewDropdown(
            publish = publish,
            model = model,
        )
    }
}

@Composable
fun WelcomeView(
    publish: (WelcomeMsg) -> Unit,
    model: WelcomeModel,
    appModel: AppModel,
    appPublish: (AppMsg) -> Unit,
) {
    val profileData = model.profileData
    val titleBarHeight = TITLE_BAR_BASE_HEIGHT_DP.dp
    val titleBarChromeHeight = titleBarHeight + TITLE_BAR_BORDER_HEIGHT_DP.dp
    val pickDirectory = LocalPickDirectory.current
    val displayName = appName(appModel.isProActivated)

    val extend = remember { ExtendOnScrollState() }
    val bottomGap = 16.dp
    val bottomGapPx = with(LocalDensity.current) { bottomGap.toPx() }

    Column(
        Modifier
            .fillMaxSize()
            .subtractTopHeight(titleBarChromeHeight)
            .nestedScroll(extend.nestedScrollConnection)
            .scrollable(rememberScrollableState(extend::consume), Orientation.Vertical)
    ) {
        // Welcome Header
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45F)
                // fully extended, the box's top gap matches its bottom gap
                .onSizeChanged { extend.maxPx = (it.height - bottomGapPx).coerceAtLeast(0f) }
                .graphicsLayer {
                    alpha = 1f - extend.progress
                    translationY = -extend.progress * 24.dp.toPx()
                }
                .padding(PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientHeader("Welcome to $displayName!")
        }

        // Modpacks Box
        Box(
            Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(bottom = bottomGap),
            contentAlignment = Alignment.Center
        ) {
            // Grow outside FadeIn. AnimatedVisibility composites an alpha layer the size of
            // its layout, which clips this upward overflow for the whole fade.
            Box(Modifier.fillMaxSize(0.9f).extendTop { extend.extendPx }) {
                FadeIn {
                    ContentBox(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .allowDragAndDrop { paths ->
                            val firstDir = paths.firstOrNull { Files.isDirectory(it) } ?: return@allowDragAndDrop
                            appPublish(AppMsg.DirectoryPicked(firstDir.toString()))
                        },
                ) {
                    val scrollState = rememberScrollState()

                    Column {
                        // Header and Open button
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Header("Modpacks", Modifier.padding(horizontal = 24.dp))

                            Row(
                                Modifier.padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            ) {
                                OutlinedButton(onClick = { pickDirectory() }) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            key = AllIconsKeys.General.InlineAdd,
                                            contentDescription = "new modpack icon",
                                            tint = JewelTheme.contentColor,
                                            hints = arrayOf(),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("New Modpack...")
                                    }
                                }
                                OutlinedButton(onClick = { pickDirectory() }) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            key = PakkuDesktopIcons.open,
                                            contentDescription = "Open Icon",
                                            tint = JewelTheme.contentColor,
                                            hints = arrayOf(),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("Open...")
                                    }
                                }
                                OutlinedButton(onClick = { appPublish(AppMsg.ShowCloneDialog) }) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            key = AllIconsKeys.General.Vcs,
                                            contentDescription = "Clone Repository Icon",
                                            tint = JewelTheme.contentColor,
                                            hints = arrayOf(),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("Clone Repository...")
                                    }
                                }
                            }
                        }

                        Spacer(
                            Modifier.padding(vertical = 16.dp)
                                .background(JewelTheme.globalColors.borders.disabled)
                                .height(1.dp).fillMaxWidth()
                        )

                        Box(Modifier.fillMaxSize()) {
                            FlowRow(
                                Modifier.fillMaxWidth().verticalScroll(scrollState).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                profileData.recentProfilesFiltered.forEach { profile ->
                                    RecentModpackCard(
                                        profile = profile,
                                        summary = model.summaries[profile.path],
                                        onClick = { publish(WelcomeMsg.DirectoryPicked(profile.path)) },
                                    )
                                }
                            }

                            VerticalScrollbar(
                                scrollState, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

/**
 * Scrolling down first grows the modpacks box upward over the header, then scrolls the list;
 * scrolling up first scrolls the list back to the top, then shrinks the box.
 */
@Stable
private class ExtendOnScrollState {
    var maxPx by mutableFloatStateOf(0f)
    private var offsetPx by mutableFloatStateOf(0f)

    val extendPx: Float get() = offsetPx.coerceIn(0f, maxPx)
    val progress: Float get() = if (maxPx > 0f) extendPx / maxPx else 0f

    /** [delta] uses scroll-delta signs (negative scrolls down and extends). Returns the consumed part. */
    fun consume(delta: Float): Float {
        val current = extendPx
        val next = (current - delta).coerceIn(0f, maxPx)
        offsetPx = next
        return current - next
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f) Offset(0f, consume(available.y)) else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f) Offset(0f, consume(available.y)) else Offset.Zero
    }
}

/** Grows the content [extendPx] above this element's top edge; the bottom edge stays put. */
private fun Modifier.extendTop(extendPx: () -> Float) = layout { measurable, constraints ->
    if (!constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val extend = extendPx().roundToInt()
    val height = constraints.maxHeight
    val placeable = measurable.measure(constraints.copy(minHeight = height + extend, maxHeight = height + extend))
    layout(placeable.width, height) { placeable.place(0, -extend) }
}
