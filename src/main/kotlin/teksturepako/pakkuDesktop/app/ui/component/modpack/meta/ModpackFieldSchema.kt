/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.ui.component.modpack.meta

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import teksturepako.pakku.api.actions.errors.ActionError
import teksturepako.pakku.api.data.ConfigFile
import teksturepako.pakku.api.data.LockFile
import teksturepako.pakku.api.data.json
import teksturepako.pakkuDesktop.app.ui.model.MetaWrite

/** File a top-level field is stored in. */
enum class ModpackFile(val fileName: String)
{
    Config(ConfigFile.FILE_NAME),
    Lock(LockFile.FILE_NAME),
}

/**
 * One property of `pakku.json` or `pakku-lock.json`, or a property nested in one. Built from the
 * files' serializers by [ModpackFieldSchema]; [hint] only adds UI metadata.
 */
@Immutable
class ModpackField internal constructor(
    /** [teksturepako.pakkuDesktop.app.ui.model.ModpackModel.modpackMetaErrors] key; nested fields share their top-level field's key. */
    val key: String,
    /** [ModpackFieldHints] path. */
    val path: String,
    val serialName: String,
    val file: ModpackFile,
    val widget: FieldWidget,
    val hint: FieldHint,
    /** A blank text or a reset stores null instead of the default. */
    val nullable: Boolean,
    /** Properties of a [FieldWidget.Object], or of each [FieldWidget.ObjectMap] entry. */
    val fields: List<ModpackField>,
    private val enumOptions: List<String>,
    internal val binding: FieldBinding?,
)
{
    val label: String
        get() = hint.label ?: serialName.replace('_', ' ').replaceFirstChar { it.titlecase() }

    val options: List<String>
        get() = hint.options.ifEmpty { enumOptions }

    val island: ModpackIsland
        get() = hint.island ?: ModpackIsland.Other

    /** False for `pakku set` fields, which always hold a value. */
    val resettable: Boolean
        get() = (binding?.resettable ?: true) && hint.resettable

    override fun toString() = "ModpackField($key)"
}

/** Values of the top-level fields for one file pair. Each file is encoded at most once. */
@Stable
class ModpackFieldValues(
    internal val config: ConfigFile,
    internal val lock: LockFile,
    internal val unknownKeys: Map<String, JsonElement>,
)
{
    internal val configJson: JsonObject by lazy {
        valuesJson.encodeToJsonElement(ConfigFile.serializer(), config).jsonObject
    }
    internal val lockJson: JsonObject by lazy {
        valuesJson.encodeToJsonElement(LockFile.serializer(), lock).jsonObject
    }

    operator fun get(field: ModpackField): JsonElement = field.binding?.read(this) ?: JsonNull
}

/** Reads every key, defaults and nulls included, so a field always has a value to show. */
@OptIn(ExperimentalSerializationApi::class)
private val valuesJson = Json(json) {
    encodeDefaults = true
    explicitNulls = true
}

/** How a top-level field reads its value and turns an edit into a [MetaWrite]. */
internal sealed interface FieldBinding
{
    val resettable: Boolean get() = true

    fun read(values: ModpackFieldValues): JsonElement
    fun write(key: String, value: JsonElement?): MetaWrite

    class ConfigJson(private val name: String) : FieldBinding
    {
        override fun read(values: ModpackFieldValues) = values.configJson[name] ?: JsonNull
        override fun write(key: String, value: JsonElement?) = MetaWrite(
            fieldKey = key,
            validate = { config, _ -> config.jsonFieldErrors(name, value) },
            replaceConfig = { it.withJsonField(name, value) },
        )
    }

    class LockJson(private val name: String) : FieldBinding
    {
        override fun read(values: ModpackFieldValues) = values.lockJson[name] ?: JsonNull
        override fun write(key: String, value: JsonElement?) = MetaWrite(
            fieldKey = key,
            validate = { _, lock -> lock.jsonFieldErrors(name, value) },
            replaceLock = { it.withJsonField(name, value) },
        )
    }

    /** A `pakku.json` key [ConfigFile] does not declare; the writer keeps it verbatim. */
    class Unknown(private val name: String) : FieldBinding
    {
        override fun read(values: ModpackFieldValues) = values.unknownKeys[name] ?: JsonNull
        override fun write(key: String, value: JsonElement?) = MetaWrite(fieldKey = key, unknownKeys = mapOf(name to value))
    }

    /** A `pakku set` option: the [LockFile] getter and setter, with the CLI's checks. */
    class LockSetter(
        val name: String,
        private val get: (LockFile) -> JsonElement,
        private val validate: (LockFile, JsonElement) -> List<ActionError>,
        private val set: (LockFile, JsonElement) -> Unit,
    ) : FieldBinding
    {
        override val resettable get() = false

        override fun read(values: ModpackFieldValues) = get(values.lock)
        override fun write(key: String, value: JsonElement?) = MetaWrite(
            fieldKey = key,
            validate = { _, lock ->
                if (value == null) listOf(PackMetaSettingError("'$name' cannot be reset.")) else validate(lock, value)
            },
            mutate = { _, lock -> if (value != null) set(lock, value) },
            writesLock = true,
        )
    }
}

object ModpackFieldSchema
{
    private const val MAX_DEPTH = 4

    private val configDescriptor = ConfigFile.serializer().descriptor
    private val lockDescriptor = LockFile.serializer().descriptor

    private val lockSetters: Map<String, FieldBinding.LockSetter> = listOf(
        FieldBinding.LockSetter(
            name = "target",
            get = { lock -> JsonPrimitive(lock.getProjectProvider().get()?.serialName.orEmpty()) },
            validate = { lock, value -> lock.targetChangeErrors(value.text) },
            set = { lock, value -> lock.setTarget(value.text.trim().lowercase()) },
        ),
        FieldBinding.LockSetter(
            name = "mc_versions",
            get = { lock -> stringListValue(lock.getMcVersions()) },
            validate = { lock, value -> lock.mcVersionsChangeErrors(value.strings) },
            set = { lock, value -> lock.setMcVersions(value.strings) },
        ),
        FieldBinding.LockSetter(
            name = "loaders",
            get = { lock -> stringMapValue(lock.getLoadersWithVersions()) },
            validate = { lock, value -> lock.loadersChangeErrors(value.stringEntries.map { it.first }) },
            set = { lock, value -> lock.setLoaders(linkedMapOf(*value.stringEntries.toTypedArray())) },
        ),
    ).associateBy { it.name }

    /** Every top-level property of both files, excluded ones included, in serializer order. */
    val allFields: List<ModpackField> =
        topLevel(ModpackFile.Config, configDescriptor) + topLevel(ModpackFile.Lock, lockDescriptor)

    /** What the Overview shows: everything not [FieldHint.excluded], by island, then hint order. */
    val fields: List<ModpackField> = allFields
        .filterNot { it.hint.excluded }
        .sortedWith(compareBy({ it.island.ordinal }, { it.hint.order }))

    /** Lock fields bound to `pakku set`, for checking they still exist in [LockFile]. */
    internal val lockSetterNames: Set<String> get() = lockSetters.keys

    fun isConfigKey(key: String): Boolean = configDescriptor.getElementIndex(key) != CompositeDecoder.UNKNOWN_NAME

    /** Top-level keys of [raw] that [ConfigFile] does not declare, in file order. */
    fun unknownKeys(raw: JsonObject): Map<String, JsonElement> = raw.filterKeys { !isConfigKey(it) }

    /** Raw-JSON editor for an [unknownKeys] entry. */
    fun unknownField(name: String): ModpackField = ModpackField(
        key = keyOf(ModpackFile.Config, name),
        path = name,
        serialName = name,
        file = ModpackFile.Config,
        widget = FieldWidget.RawJson,
        hint = FieldHint(label = name, island = ModpackIsland.RawJson),
        nullable = true,
        fields = emptyList(),
        enumOptions = emptyList(),
        binding = FieldBinding.Unknown(name),
    )

    /**
     * [config] as a `pakku.json` object, with the keys it does not declare carried over from
     * [onDisk] and then [edits] applied (null removes a key). Declared keys keep serializer order.
     */
    fun configJsonWithUnknownKeys(
        config: ConfigFile,
        onDisk: JsonObject?,
        edits: Map<String, JsonElement?> = emptyMap(),
    ): JsonObject
    {
        val unknown = LinkedHashMap(onDisk?.let(::unknownKeys).orEmpty())
        edits.filterKeys { !isConfigKey(it) }.forEach { (name, value) ->
            if (value == null) unknown.remove(name) else unknown[name] = value
        }
        return JsonObject(json.encodeToJsonElement(ConfigFile.serializer(), config).jsonObject + unknown)
    }

    private fun keyOf(file: ModpackFile, name: String) = "${file.fileName}:$name"

    private fun bindingFor(file: ModpackFile, name: String): FieldBinding = when (file)
    {
        ModpackFile.Config -> FieldBinding.ConfigJson(name)
        ModpackFile.Lock -> lockSetters[name] ?: FieldBinding.LockJson(name)
    }

    private fun topLevel(file: ModpackFile, descriptor: SerialDescriptor): List<ModpackField> =
        (0 until descriptor.elementsCount).map { i ->
            val name = descriptor.getElementName(i)
            field(file, keyOf(file, name), name, name, descriptor.getElementDescriptor(i), bindingFor(file, name), 0)
        }

    private fun nested(file: ModpackFile, key: String, parentPath: String, descriptor: SerialDescriptor, depth: Int) =
        (0 until descriptor.elementsCount).map { i ->
            val name = descriptor.getElementName(i)
            field(file, key, "$parentPath.$name", name, descriptor.getElementDescriptor(i), null, depth + 1)
        }

    @OptIn(ExperimentalSerializationApi::class)
    private fun field(
        file: ModpackFile,
        key: String,
        path: String,
        serialName: String,
        descriptor: SerialDescriptor,
        binding: FieldBinding?,
        depth: Int,
    ): ModpackField
    {
        val shape = if (depth < MAX_DEPTH) widgetOf(descriptor) else FieldWidget.RawJson
        val nestedFields = when (shape)
        {
            FieldWidget.Object -> nested(file, key, path, descriptor, depth)
            FieldWidget.ObjectMap -> nested(file, key, path, descriptor.getElementDescriptor(1), depth)
            else -> emptyList()
        }
        val enumOptions = if (descriptor.kind == SerialKind.ENUM) {
            (0 until descriptor.elementsCount).map(descriptor::getElementName)
        } else {
            emptyList()
        }
        val hint = ModpackFieldHints.hint(file, path) ?: FieldHint()
        return ModpackField(
            key = key,
            path = path,
            serialName = serialName,
            file = file,
            widget = hint.widget ?: shape,
            hint = hint,
            nullable = descriptor.isNullable,
            fields = nestedFields,
            enumOptions = enumOptions,
            binding = binding,
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun widgetOf(descriptor: SerialDescriptor): FieldWidget = when (descriptor.kind)
    {
        PrimitiveKind.STRING -> FieldWidget.Text
        PrimitiveKind.BOOLEAN -> FieldWidget.Boolean
        PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT,
        PrimitiveKind.LONG, PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> FieldWidget.Number
        SerialKind.ENUM -> FieldWidget.Choice
        StructureKind.LIST ->
            if (descriptor.getElementDescriptor(0).kind == PrimitiveKind.STRING) FieldWidget.StringList
            else FieldWidget.RawJson
        StructureKind.MAP ->
        {
            val value = descriptor.getElementDescriptor(1).kind
            when
            {
                descriptor.getElementDescriptor(0).kind != PrimitiveKind.STRING -> FieldWidget.RawJson
                value == PrimitiveKind.STRING -> FieldWidget.StringMap
                value == StructureKind.CLASS -> FieldWidget.ObjectMap
                else -> FieldWidget.RawJson
            }
        }
        StructureKind.CLASS -> FieldWidget.Object
        else -> FieldWidget.RawJson
    }
}

// -- Edits --

/** Write for a top-level field: [value] replaces the stored value; null resets it. */
fun ModpackField.write(value: JsonElement?): MetaWrite =
    requireNotNull(binding) { "$path is nested; write its top-level field" }.write(key, value)

/** Shows [error] on this field without writing anything. */
fun ModpackField.reject(error: ActionError): MetaWrite =
    MetaWrite(fieldKey = key, validate = { _, _ -> listOf(error) })

/** Committed value of a text editor: blank is null for nullable fields; numbers stay unquoted. */
fun ModpackField.textValue(text: String): JsonElement? = when
{
    text.isEmpty() && nullable -> null
    widget == FieldWidget.Number ->
        text.toLongOrNull()?.let(::JsonPrimitive) ?: text.toDoubleOrNull()?.let(::JsonPrimitive) ?: JsonPrimitive(text)
    else -> JsonPrimitive(text)
}

fun parseJsonValue(text: String): Result<JsonElement, ActionError> =
    runCatching { json.parseToJsonElement(text) }.fold(
        onSuccess = { Ok(it) },
        onFailure = { error ->
            val reason = error.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "malformed JSON"
            Err(PackMetaSettingError("Invalid JSON: $reason"))
        },
    )

fun stringListValue(entries: List<String>): JsonElement = JsonArray(entries.map(::JsonPrimitive))

fun stringMapValue(entries: List<Pair<String, String>>): JsonElement =
    JsonObject(entries.associate { (key, value) -> key to JsonPrimitive(value) })

/** This object with [name] set to [value]; a null value removes it. */
fun JsonElement.withChild(name: String, value: JsonElement?): JsonObject
{
    val current = this as? JsonObject ?: JsonObject(emptyMap())
    return JsonObject(if (value == null || value is JsonNull) current - name else current + (name to value))
}

fun JsonElement.child(name: String): JsonElement = (this as? JsonObject)?.get(name) ?: JsonNull

/** A stored property, as opposed to null or blank. `false` and `0` count. */
fun JsonElement.isOverride(): Boolean = when (this)
{
    is JsonNull -> false
    is JsonArray -> any { it.isOverride() }
    is JsonObject -> values.any { it.isOverride() }
    is JsonPrimitive -> !isString || content.isNotBlank()
}

// -- Reads --

/** Content of a primitive; empty for null, arrays, and objects. */
val JsonElement.text: String
    get() = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

val JsonElement.strings: List<String>
    get() = (this as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

val JsonElement.stringEntries: List<Pair<String, String>>
    get() = (this as? JsonObject)?.map { (key, value) -> key to value.text }.orEmpty()

/** An object with at least one non-null property. */
val JsonElement.hasValues: Boolean
    get() = (this as? JsonObject)?.values?.any { it !is JsonNull } == true

/** Badge texts for a [FieldHint.summary] field. */
val JsonElement.summaryLabels: List<String>
    get() = when (this)
    {
        is JsonArray -> strings
        is JsonObject -> stringEntries.map { (key, value) -> if (value.isBlank()) key else "$key $value" }
        else -> listOf(text)
    }.filter { it.isNotBlank() }

fun JsonElement.toPrettyJson(): String = json.encodeToString(JsonElement.serializer(), this)
