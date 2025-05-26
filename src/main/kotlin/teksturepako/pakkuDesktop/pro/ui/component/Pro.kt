package teksturepako.pakkuDesktop.pro.ui.component

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.pro.ui.viewmodel.LicenseKeyViewModel

@Composable
fun Pro(content: @Composable () -> Unit)
{
    if (LicenseKeyViewModel.isActivated == true)
    {
        content()
    }
}