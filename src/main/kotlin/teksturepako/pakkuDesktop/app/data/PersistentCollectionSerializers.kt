/*
 * Copyright (c) Juraj Hrivnák. All Rights Reserved unless otherwise explicitly stated.
 */

package teksturepako.pakkuDesktop.app.data

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ProfileListSerializer : KSerializer<PersistentList<Profile>> {
    private val delegate = ListSerializer(Profile.serializer())

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: PersistentList<Profile>) =
        delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): PersistentList<Profile> =
        delegate.deserialize(decoder).toPersistentList()
}

object StringPersistentSetSerializer : KSerializer<PersistentSet<String>> {
    private val delegate = SetSerializer(String.serializer())

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: PersistentSet<String>) =
        delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): PersistentSet<String> =
        delegate.deserialize(decoder).toPersistentSet()
}
