package teksturepako.pakkupro.data.models.polar.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateLicenseKeyReq(
    val key: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("activation_id") val activationId: String,
)
