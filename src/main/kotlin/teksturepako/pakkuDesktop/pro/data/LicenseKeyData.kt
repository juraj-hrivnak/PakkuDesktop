/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.pro.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.jsonEncodeDefaults
import teksturepako.pakku.io.readPathTextOrNull
import teksturepako.pakku.io.tryOrNull
import teksturepako.pakku.io.writeToFile
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.pathString
import kotlin.io.path.setAttribute

@Serializable
data class LicenseKeyData(
    val key: String,
    @SerialName("display_key") val displayKey: String,
    @SerialName("activation_id") val activationId: String,
)
{
    companion object
    {
        private const val FILE_NAME = "license-key-data.json"
        private val parentDir = Path(".secret")
        private val path = Path(parentDir.pathString, FILE_NAME)

        suspend fun read(): LicenseKeyData?
        {
            val text = readPathTextOrNull(path) ?: return null

            return runCatching { jsonEncodeDefaults.decodeFromString<LicenseKeyData>(text) }.getOrNull()
        }
    }

    suspend fun write(): ActionError?
    {
        parentDir.tryOrNull {
            it.createDirectory()
            it.setAttribute("dos:hidden", true)
        }

        return writeToFile<LicenseKeyData>(
            this, path.pathString, format = jsonEncodeDefaults
        )
    }
}
