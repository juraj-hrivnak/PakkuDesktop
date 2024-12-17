package pakkupro.view.components

import com.github.michaelbull.result.Result
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text
import pakkupro.view.components.project.Aliases
import pakkupro.view.components.project.Redistributable
import pakkupro.view.components.project.Side
import teksturepako.pakku.api.actions.ActionError
import teksturepako.pakku.api.projects.Project
import teksturepako.pakku.api.projects.ProjectSide

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectName(project: Project)
{
    val gradientColors = listOf(Color(27, 204, 234), Color(81, 203, 255))

    project.name.values.firstOrNull()?.let { projectName ->
        FlowRow(Modifier.padding(16.dp)) {
            Column {
                SelectionContainer {
                    Text(
                        projectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = gradientColors
                            )
                        )
                    )
                }
            }
            Column(Modifier.size(30.dp)) {
                CopyButton(
                    onClick = {
                        it.setText(AnnotatedString((projectName)))
                    },
                    Modifier
                        .size(25.dp)
                        .offset(y = 7.dp)
                        .padding(4.dp),
                    tooltip = "project name"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectProperties(
    project: Project,
    textModifier: Modifier = Modifier,
    editable: Boolean = false,
    onSideSelect: (ProjectSide?) -> Result<ProjectSide?, ActionError?>
)
{
    Column(
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        FlowRow(
            Modifier.padding(vertical = 6.dp)
        ) {
            ContentBox(Modifier.padding(2.dp)) {
                Text("Type:", textModifier)
            }

            ContentBox(
                if (editable)
                {
                    Modifier
                        .padding(2.dp)
                        .clickable {}
                }
                else Modifier.padding(2.dp)
            ) {
                Text(project.type.name, textModifier)
            }
        }

        Side(project, editable, onSelect = onSideSelect)

        Redistributable(project, editable)

        // -- ALIASES --

        Aliases(project, editable)
    }
}
