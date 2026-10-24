package com.ascolith.textseek.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@JvmInline
@Serializable(with = SearchGroupIdSerializer::class)
value class SearchGroupId(val value: Long)

class SearchGroupIdSerializer : KSerializer<SearchGroupId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SearchGroupId", PrimitiveKind.LONG)
    
    override fun serialize(encoder: Encoder, value: SearchGroupId) {
        encoder.encodeLong(value.value)
    }
    
    override fun deserialize(decoder: Decoder): SearchGroupId {
        return SearchGroupId(decoder.decodeLong())
    }
}
