package com.ascolith.textseek.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@JvmInline
@Serializable(with = SearchSettingIdSerializer::class)
value class SearchSettingId(val value: Long)

class SearchSettingIdSerializer : KSerializer<SearchSettingId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SearchSettingId", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: SearchSettingId) {
        encoder.encodeLong(value.value)
    }

    override fun deserialize(decoder: Decoder): SearchSettingId {
        return SearchSettingId(decoder.decodeLong())
    }
}
