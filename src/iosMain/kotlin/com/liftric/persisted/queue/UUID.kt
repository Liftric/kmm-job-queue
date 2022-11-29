package com.liftric.persisted.queue

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import platform.Foundation.NSUUID
import kotlin.reflect.KClass

actual typealias UUID = NSUUID

actual fun KClass<UUID>.instance(): UUID = NSUUID()

actual object UUIDSerializer: KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return UUID(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString().lowercase())
    }
}
