package teksturepako.pakkupro.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.michaelbull.result.fold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakkupro.data.Polar

object LicenseKeyViewModel
{
    var isActivated by mutableStateOf<Boolean?>(null)
        private set

    var error by mutableStateOf<ActionError?>(null)
        private set

    suspend fun checkActivation()
    {
        isActivated = Polar.isActivated()
    }

    fun process(licenseKeyText: String, coroutineScope: CoroutineScope)
    {
        coroutineScope.launch {
            Polar.processLicenseKey(licenseKeyText).fold(
                success = { isActivated = true },
                failure = { error = it }
            )
        }
    }
}