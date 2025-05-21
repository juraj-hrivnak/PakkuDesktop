package teksturepako.pakkuDesktop.data.models.polar.response

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakkuDesktop.data.models.polar.LicenseKeyUser
import teksturepako.pakkuDesktop.data.models.polar.Meta

@Serializable
data class ActivateLicenseKey(
    val id: String,
    @SerialName("license_key_id") val licenseKeyId: String,
    val label: String,
    val meta: Meta,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("modified_at") val modifiedAt: Instant? = null,
    @SerialName("license_key") val licenseKey: LicenseKey,
)
{
    @Serializable
    data class LicenseKey(
        val id: String,
        @SerialName("organization_id") val organizationId: String,
        @SerialName("user_id") val userId: String,
        val user: LicenseKeyUser,
        @SerialName("benefit_id") val benefitId: String,
        val key: String,
        @SerialName("display_key") val displayKey: String,
        /** "granted" | "revoked" | "disabled" */
        val status: String,
        @SerialName("limit_activations") val limitActivations: Int? = null,
        val usage: Int,
        @SerialName("limit_usage") val limitUsage: Int? = null,
        val validations: Int,
        @SerialName("last_validated_at") val lastValidatedAt: Instant? = null,
        @SerialName("expires_at") val expiresAt: Instant? = null
    )
}
