package teksturepako.pakkuDesktop.pro

import androidx.compose.runtime.Composable
import teksturepako.pakkuDesktop.ui.viewmodel.LicenseKeyViewModel

@Composable
fun Pro(content: @Composable () -> Unit)
{
    if (LicenseKeyViewModel.isActivated == true)
    {
        content()
    }
}