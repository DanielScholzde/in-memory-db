package de.danielscholz.database.serializer

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


class PersistentSetSerializer : KSerializer<PersistentSet<Long>> {

    private val listSerializer: KSerializer<List<Long>> = ListSerializer(Long.serializer())

    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun serialize(encoder: Encoder, value: PersistentSet<Long>) = listSerializer.serialize(encoder, value.toList())

    override fun deserialize(decoder: Decoder): PersistentSet<Long> = persistentSetOf<Long>().addAll(listSerializer.deserialize(decoder))

}