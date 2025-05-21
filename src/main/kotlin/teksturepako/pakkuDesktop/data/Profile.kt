package teksturepako.pakkuDesktop.data

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val name: String,
    val path: String,
    @SerialName("last_opened") val lastOpened: Instant
)
