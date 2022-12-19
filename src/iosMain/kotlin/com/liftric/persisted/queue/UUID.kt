package com.liftric.persisted.queue

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import platform.Foundation.NSUUID

actual typealias UUID = NSUUID

internal actual object UUIDFactory {
    actual fun create(): UUID = NSUUID()
    actual fun fromString(string: String): UUID = UUID(string)
}

actual object UUIDSerializer: KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return UUID(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString().lowercase())
    }
}
