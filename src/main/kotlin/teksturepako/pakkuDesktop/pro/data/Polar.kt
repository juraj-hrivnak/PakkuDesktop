package teksturepako.pakkuDesktop.pro.data

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.json
import teksturepako.pakku.io.createHash
import teksturepako.pakkuDesktop.pro.data.polar.Meta
import teksturepako.pakkuDesktop.pro.data.polar.request.ActivateLicenseKeyReq
import teksturepako.pakkuDesktop.pro.data.polar.request.ValidateLicenseKeyReq
import teksturepako.pakkuDesktop.pro.data.polar.response.ActivateLicenseKey
import teksturepako.pakkuDesktop.pro.data.polar.response.ValidateLicenseKey
import teksturepako.pakkuDesktop.pro.io.AddressType
import teksturepako.pakkuDesktop.pro.io.getNetworkAddress
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import com.github.michaelbull.result.fold as resultFold

object Polar
{
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            Json
        }
        install(HttpTimeout) {
            val timeout = 1.minutes.inWholeMilliseconds

            socketTimeoutMillis = timeout
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
        }
        install(UserAgent) {
            agent = "PakkuPro (github.com/juraj-hrivnak/PakkuDesktop)"
        }
        engine {
            pipelining = true
            config {
                retryOnConnectionFailure(true)

                val timeout = 1.minutes.toJavaDuration()

                connectTimeout(timeout)
                callTimeout(timeout)
                writeTimeout(timeout)
            }
        }
    }

    private const val ORGANIZATION_ID = "bea3ac99-8009-4f70-a4ff-bfad9f6428c1"

    data class ActivationError(val message: String) : ActionError()
    {
        override val rawMessage: String
            get() = message
    }

    private suspend fun activate(key: String, label: String): Result<ActivateLicenseKey, ActionError>
    {
        val pcId = generatePcId() ?: return Err(ActivationError("Failed to generate PC ID for you license activation."))

        val url = "https://api.polar.sh/v1/users/license-keys/activate"
        val bodyContent = Json.encodeToString(ActivateLicenseKeyReq(key, ORGANIZATION_ID, label, Meta(pcId)))

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(bodyContent)
        }

        return when (response.status.value)
        {
            200  -> {
                val jsonValue = json.decodeFromString<ActivateLicenseKey>(response.body())
                return Ok(jsonValue)
            }
            403  -> Err(ActivationError("The limit for activation of this license key was reached."))
            404  -> Err(ActivationError("License key not found."))
            422  -> Err(ActivationError("Failed to activate your license key due to a validation error."))
            else -> Err(ActivationError("Failed to activate your license key due to an unknown error."))
        }
    }

    private fun generatePcId(): String? = getNetworkAddress(AddressType.MAC)
        ?.toByteArray()?.let { createHash("sha256", it) }

    data class ValidationError(val message: String) : ActionError()
    {
        override val rawMessage: String
            get() = message
    }

    private suspend fun validate(key: String, activationId: String): Result<ValidateLicenseKey, ActionError>
    {
        val pcId = generatePcId() ?: return Err(ValidationError("Failed to generate PC ID for you license key."))

        val url = "https://api.polar.sh/v1/users/license-keys/validate"
        val bodyContent = Json.encodeToString(ValidateLicenseKeyReq(key, ORGANIZATION_ID, activationId))

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(bodyContent)
        }

        return when (response.status.value)
        {
            200  -> {
                val jsonValue = json.decodeFromString<ValidateLicenseKey>(response.body())

                if (jsonValue.activation?.meta?.pcId != pcId)
                {
                    return Err(ValidationError("Failed to validate license key. This computer is not valid."))
                }

                return Ok(jsonValue)
            }
            404  -> Err(ActivationError("License key not found."))
            422  -> Err(ActivationError("Failed to validate license key due to a validation error."))
            else -> Err(ActivationError("Failed to validate license key due to an unknown error."))
        }
    }

    suspend fun isActivated(): Boolean
    {
        val licenseKeyData = LicenseKeyData.read() ?: return false

        return validate(licenseKeyData.key, licenseKeyData.activationId).resultFold(
            failure = { false }, success = { true }
        )
    }

    suspend fun processLicenseKey(key: String): Result<String, ActionError>
    {
        suspend fun initData(): Result<LicenseKeyData, ActionError>
        {
            val osName = System.getProperty("os.name") ?: null

            val label = buildString {
                if (osName != null)
                {
                    append(osName)
                    append(" ")
                }
                append("Activation")
            }

            activate(key, label).resultFold(
                failure = { error ->
                    return Err(error)
                },
                success = { activationModel ->
                    val data = LicenseKeyData(
                        key = activationModel.licenseKey.key,
                        displayKey = activationModel.licenseKey.displayKey,
                        activationId = activationModel.id
                    )
                    data.write()?.let { error ->
                        return Err(error)
                    }
                    return Ok(data)
                }
            )
        }

        val licenseKeyData = initData().getOrElse { error -> return Err(error) }

        return validate(licenseKeyData.key, licenseKeyData.activationId).resultFold(
            failure = { error -> Err(error) },
            success = { Ok(licenseKeyData.displayKey) }
        )
    }
}