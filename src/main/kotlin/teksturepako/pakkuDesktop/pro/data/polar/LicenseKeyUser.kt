package teksturepako.pakkuDesktop.pro.data.polar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LicenseKeyUser(
    val id: String,
    @SerialName("public_name") val publicName: String,
    val email: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)
