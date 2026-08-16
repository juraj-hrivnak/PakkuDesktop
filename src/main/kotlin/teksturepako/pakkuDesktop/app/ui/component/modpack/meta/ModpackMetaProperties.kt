/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.michaelbull.result.get
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite
import teksturepako.pakkuDesktop.app.ui.model.ModpackModel
import teksturepako.pakkuDesktop.app.ui.model.ModpackMsg

@Composable
fun ModpackMetaProperties(publish: (ModpackMsg) -> Unit, model: ModpackModel) {
    val config = model.configFile?.get() ?: return
    val lock = model.lockFile?.get() ?: return

    val name = config.getName()
    val version = config.getVersion()
    val author = config.getAuthor()
    val description = config.getDescription()
    val mcVersion = lock.getFirstMcVersion().orEmpty()
    val loaderEntry = lock.getLoadersWithVersions().firstOrNull()
    val loaderName = loaderEntry?.first.orEmpty()
    val loaderVersion = loaderEntry?.second.orEmpty()
    val target = lock.getProjectProvider().get()?.serialName.orEmpty()

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Modpack",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (model.editingModpack) {
                DefaultButton(onClick = { publish(ModpackMsg.ModpackEditing(false)) }) {
                    Text("Done")
                }
            } else {
                OutlinedButton(onClick = { publish(ModpackMsg.ModpackEditing(true)) }) {
                    Text("Edit")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroNameField(
                value = name,
                editing = model.editingModpack,
                publish = publish,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    ModpackCommitTextField(
                        label = "Version",
                        value = version,
                        placeholder = "No version",
                        publish = publish,
                        model = model,
                        mutate = { cfg, _, text -> cfg.setVersion(text) },
                    )
                }
                Box(Modifier.weight(1f)) {
                    ModpackCommitTextField(
                        label = "Author",
                        value = author,
                        placeholder = "No author",
                        publish = publish,
                        model = model,
                        mutate = { cfg, _, text -> cfg.setAuthor(text) },
                    )
                }
            }
            if (!model.editingModpack &&
                (mcVersion.isNotBlank() || loaderName.isNotBlank() || target.isNotBlank())
            ) {
                Text(
                    buildString {
                        if (mcVersion.isNotBlank()) append("MC $mcVersion")
                        if (loaderName.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(loaderName)
                            if (loaderVersion.isNotBlank()) append(" $loaderVersion")
                        }
                        if (target.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(target)
                        }
                    },
                    color = JewelTheme.contentColor.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
        }

        Divider(Orientation.Horizontal)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GroupHeader("Description")
            ModpackCommitTextField(
                label = "",
                value = description,
                placeholder = "No description",
                singleLine = false,
                publish = publish,
                model = model,
                mutate = { cfg, _, text -> cfg.setDescription(text) },
            )
        }

        Divider(Orientation.Horizontal)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GroupHeader("Runtime")
            Text(
                "Primary Minecraft version, loader, and export target.",
                color = JewelTheme.contentColor.copy(alpha = 0.45f),
                fontSize = 12.sp,
            )

            ModpackCommitTextField(
                label = "Minecraft",
                value = mcVersion,
                placeholder = "e.g. 1.20.1",
                publish = publish,
                model = model,
                mutate = { _, lk, text ->
                    if (text.isNotBlank()) lk.setMcVersions(listOf(text.trim()))
                },
            )

            ModpackLoaderChips(
                selected = loaderName,
                publish = publish,
                model = model,
                mutate = { _, lk, text ->
                    val ver = lk.getLoadersWithVersions().firstOrNull()?.second.orEmpty()
                    if (text.isNotBlank()) lk.setLoaders(mapOf(text.trim().lowercase() to ver))
                },
            )

            ModpackCommitTextField(
                label = "Loader version",
                value = loaderVersion,
                placeholder = "e.g. 0.15.0",
                publish = publish,
                model = model,
                mutate = { _, lk, text ->
                    val loader = lk.getLoadersWithVersions().firstOrNull()?.first.orEmpty()
                    if (loader.isNotBlank()) lk.setLoader(loader, text.trim())
                },
            )

            ModpackTargetChips(
                selected = target,
                publish = publish,
                model = model,
                mutate = { _, lk, text ->
                    if (text.isNotBlank()) lk.setTarget(text.trim().lowercase())
                },
            )
        }
    }
}

@Composable
private fun HeroNameField(
    value: String,
    editing: Boolean,
    publish: (ModpackMsg) -> Unit,
) {
    if (editing) {
        val textFieldState = rememberTextFieldState(value)
        var focused by remember { mutableStateOf(false) }

        LaunchedEffect(value) {
            if (!focused && textFieldState.text.toString() != value) {
                textFieldState.setTextAndPlaceCursorAtEnd(value)
            }
        }

        fun commit() {
            val text = textFieldState.text.toString()
            if (text == value) return
            publish(
                ModpackMsg.MetaWriteRequested(
                    MetaWrite { config, _ -> config.setName(text) },
                ),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Name",
                color = JewelTheme.contentColor.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            TextField(
                textFieldState,
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .onFocusChanged { focus ->
                        val wasFocused = focused
                        focused = focus.isFocused
                        if (wasFocused && !focus.isFocused) commit()
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                commit()
                                true
                            }
                            else -> false
                        }
                    },
            )
        }
    } else {
        Text(
            value.ifBlank { "Untitled modpack" },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (value.isBlank()) {
                JewelTheme.contentColor.copy(alpha = 0.4f)
            } else {
                JewelTheme.contentColor
            },
        )
    }
}
