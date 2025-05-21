package teksturepako.pakkuDesktop.ui.view.children

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.ui.PakkuDesktopConstants
import teksturepako.pakkuDesktop.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.ui.component.FadeIn
import teksturepako.pakkuDesktop.ui.component.license.LicenseKeyField
import teksturepako.pakkuDesktop.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.ui.modifier.subtractTopHeight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PakkuApplicationScope.ActivationView()
{
    val titleBarHeight = 40.dp

    MainTitleBar(Modifier.height(titleBarHeight)) {
        FadeIn {
            Text("Welcome to Pakku Pro")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .subtractTopHeight(titleBarHeight)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5F)
                .padding(PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowColumn(
                verticalArrangement = Arrangement.Center,
            ) {
                FadeIn {
                    GradientHeader("Welcome to Pakku Pro!")
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            LicenseKeyField()
        }
    }
}