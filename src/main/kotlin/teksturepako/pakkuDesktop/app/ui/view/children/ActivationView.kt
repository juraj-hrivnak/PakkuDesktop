package teksturepako.pakkuDesktop.app.ui.view.children

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import teksturepako.pakkuDesktop.app.ui.application.PakkuApplicationScope
import teksturepako.pakkuDesktop.app.ui.application.titlebar.MainTitleBar
import teksturepako.pakkuDesktop.app.ui.component.FadeIn
import teksturepako.pakkuDesktop.pro.ui.component.license.LicenseKeyField
import teksturepako.pakkuDesktop.app.ui.component.text.GradientHeader
import teksturepako.pakkuDesktop.app.ui.modifier.subtractTopHeight

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
                .padding(teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants.commonPaddingSize),
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
                .padding(teksturepako.pakkuDesktop.app.ui.PakkuDesktopConstants.commonPaddingSize),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            LicenseKeyField()
        }
    }
}