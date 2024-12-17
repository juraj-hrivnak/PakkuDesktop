package teksturepako.pakkupro.data.models.polar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Meta(
    @SerialName("pc_id") val pcId: String
)