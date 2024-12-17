package teksturepako.pakkupro.data.models.polar.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakkupro.data.models.polar.Meta

@Serializable
data class ActivateLicenseKeyReq(
    val key: String,
    @SerialName("organization_id") val organizationId: String,
    val label: String,
    val meta: Meta
)
