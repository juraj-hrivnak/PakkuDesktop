/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.actions

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.michaelbull.result.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.actions.errors.IOExportingError
import teksturepako.pakku.api.actions.export.ExportProfile
import teksturepako.pakku.api.actions.export.exportDefaultProfiles
import teksturepako.pakku.api.data.workingPath
import teksturepako.pakku.cli.ui.shortForm
import teksturepako.pakku.io.toHumanReadableSize
import teksturepako.pakkuDesktop.app.io.RevealFileAction
import teksturepako.pakkuDesktop.app.ui.viewmodel.ModpackViewModel
import teksturepako.pakkuDesktop.app.ui.viewmodel.state.ModpackUiState
import teksturepako.pakkuDesktop.pkui.component.toast.showToast
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.fileSize
import kotlin.io.path.pathString
import kotlin.time.Duration

fun exportImpl(modpackUiState: ModpackUiState)
{
    if (modpackUiState.action.first != null) return

    actionRunner("Exporting") {
        launch {
            val lockFile = modpackUiState.lockFile?.getOrElse {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it.rawMessage)
                        }
                    }
                }
                return@launch
            } ?: return@launch

            val configFile = modpackUiState.configFile?.getOrElse {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it.rawMessage)
                        }
                    }
                }
                return@launch
            } ?: return@launch

            val platforms = lockFile.getPlatforms().getOrElse {
                withContext(Dispatchers.Main) {
                    ModpackViewModel.toasts.showToast {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .width(300.dp)
                        ) {
                            Text(it.rawMessage)
                        }
                    }
                }
                return@launch
            }

            exportDefaultProfiles(
                onError = { profile: ExportProfile, error: ActionError ->
                    if (error !is IOExportingError)
                    {
                        val message = "[${profile.name} profile] ${error.rawMessage}"

                        withContext(Dispatchers.Main) {
                            ModpackViewModel.toasts.showToast {
                                Box(
                                    modifier = Modifier.padding(16.dp).width(300.dp)
                                ) {
                                    Column {
                                        Text("[${profile.name} profile]", fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        Text(error.rawMessage)
                                    }
                                }
                            }
                        }
                        println(message)
                    }
                },
                onSuccess = { profile: ExportProfile, path: Path, duration: Duration ->
                    val fileSize = path.fileSize().toHumanReadableSize()
                    val filePath = Path(workingPath).relativize(path).pathString

                    val message = "[${profile.name} profile] exported to '$filePath' " +
                            "($fileSize) in ${duration.shortForm()}"

                    withContext(Dispatchers.Main) {
                        ModpackViewModel.toasts.showToast {
                            Box(
                                modifier = Modifier.padding(16.dp).width(300.dp)
                            ) {
                                Column {
                                    Text("[${profile.name} profile]", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text("exported to '$filePath'", style = JewelTheme.consoleTextStyle)
                                    Text(" ($fileSize) in ${duration.shortForm()}")
                                    Spacer(Modifier.height(8.dp))
                                    DefaultButton(
                                        onClick = {
                                            RevealFileAction.openFile(path)
                                        }
                                    ) {
                                        Text("Open")
                                    }
                                }
                            }
                        }
                    }
                    println(message)
                },
                lockFile, configFile, platforms,
            ).joinAll()
        }
    }
}
