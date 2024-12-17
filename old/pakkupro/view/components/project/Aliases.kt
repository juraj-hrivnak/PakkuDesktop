package pakkupro.view.components.project

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.jetbrains.jewel.ui.component.Text
import pakkupro.view.components.ContentBox
import pakkupro.view.components.ContentBoxTextField
import pakkupro.viewmodel.MainViewModel
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.projects.Project

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Aliases(
    project: Project,
    editable: Boolean,
    textModifier: Modifier = Modifier,
)
{
    val aliases = project.slug.values.firstOrNull()?.let { slug ->
        MainViewModel.configFile?.projects?.getOrPut(slug) {
            ConfigFile.ProjectConfig()
        }?.apply { if (aliases == null) aliases = mutableSetOf() }?.aliases
    }

    val aliasesStateList = aliases?.toMutableStateList()

    if (editable)
    {
        FlowRow(
            Modifier.padding(vertical = 6.dp)
        ) {
            ContentBox(Modifier.padding(2.dp)) {
                Text("Aliases:", textModifier)
            }

            if (aliasesStateList != null)
            {
                for ((i, alias) in aliasesStateList.withIndex())
                {
                    val state = TextFieldState(alias)

                    ContentBoxTextField(
                        state, Modifier.padding(2.dp),
                        onEnterPressed = {
                            if (state.text.isBlank()) return@ContentBoxTextField
                            if (aliasesStateList.contains(state.text)) return@ContentBoxTextField

                            aliasesStateList.remove(alias)
                            aliases.remove(alias)

                            aliasesStateList.add(i, state.text.toString())
                            aliases.add(state.text.toString())

                            runBlocking {
                                MainViewModel.configFile?.write()
                            }
                        },
                        onRemoveClicked = {
                            aliasesStateList.remove(alias)
                            aliases.remove(alias)

                            runBlocking {
                                MainViewModel.configFile?.write()
                            }
                        }
                    )
                }
            }

            val state = TextFieldState("")

            ContentBoxTextField(
                state,
                Modifier.padding(2.dp),
                onEnterPressed = {
                    println("Enter released")

                    if (state.text.isBlank()) return@ContentBoxTextField
                    if (aliasesStateList != null && aliasesStateList.contains(state.text)) return@ContentBoxTextField

                    aliasesStateList?.add(state.text.toString())
                    aliases?.add(state.text.toString())

                    runBlocking {
                        MainViewModel.configFile?.write()
                    }

                    state.clearText()
                },
                removeButtonEnabled = false
            )
        }
    }
    else if (aliasesStateList?.isNotEmpty() == true)
    {
        FlowRow(
            Modifier.padding(vertical = 6.dp)
        ) {
            ContentBox(Modifier.padding(2.dp)) {
                Text("Aliases:", textModifier)
            }

            for (alias in aliasesStateList)
            {
                ContentBox(
                    Modifier.padding(2.dp),
                ) {
                    Text(alias, textModifier)
                }
            }
        }
    }
}
